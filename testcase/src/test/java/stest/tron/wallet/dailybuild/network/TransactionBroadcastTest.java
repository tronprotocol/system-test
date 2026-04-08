package stest.tron.wallet.dailybuild.network;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol.Account;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.MultiNode;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.TronConstants;
import stest.tron.wallet.common.client.utils.Utils;

/**
 * Cross-node transaction broadcast tests.
 *
 * <p>Sends a transaction via node1 and verifies it is visible on node2,
 * confirming P2P transaction propagation.
 */
@Slf4j
@MultiNode(reason = "Cross-node transaction broadcast verification")
public class TransactionBroadcastTest extends TronBaseTest {

  private String fullnode2;
  private ManagedChannel channelFull2;
  private WalletGrpc.WalletBlockingStub blockingStubFull2;

  private ECKey receiverKey;
  private byte[] receiverAddress;
  private String receiverKeyStr;

  @BeforeClass(enabled = true)
  public void beforeClass() {
    fullnode2 = requireSecondFullnode();
    channelFull2 = ManagedChannelBuilder.forTarget(fullnode2).usePlaintext().build();
    blockingStubFull2 = WalletGrpc.newBlockingStub(channelFull2);

    receiverKey = new ECKey(Utils.getRandom());
    receiverAddress = receiverKey.getAddress();
    receiverKeyStr = ByteArray.toHexString(receiverKey.getPrivKeyBytes());
    PublicMethod.printAddress(receiverKeyStr);
  }

  @Test(enabled = true, description = "Transaction sent via node1 should be visible on node2",
      groups = {"daily"})
  public void test01TransactionPropagation() {
    long sendAmount = 1_000_000L; // 1 TRX

    // Send via node1
    String txId = PublicMethod.sendcoinGetTransactionId(receiverAddress, sendAmount,
        foundationAddress, foundationKey, blockingStubFull);
    Assert.assertNotNull(txId, "Transaction should be created on node1");
    logger.info("Transaction sent via node1, txId: {}", txId);

    // Wait for block inclusion and propagation
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    // Query balance on node2
    Account receiverAccount = PublicMethod.queryAccount(receiverAddress, blockingStubFull2);
    long balanceOnNode2 = receiverAccount.getBalance();
    logger.info("Receiver balance on node2: {}", balanceOnNode2);
    Assert.assertEquals(balanceOnNode2, sendAmount,
        "Receiver balance on node2 should match sent amount");
  }

  @Test(enabled = true, dependsOnMethods = "test01TransactionPropagation",
      description = "Multiple transactions propagate correctly",
      groups = {"daily"})
  public void test02MultipleTxPropagation() {
    long sendAmount = 500_000L; // 0.5 TRX

    Account beforeAccount = PublicMethod.queryAccount(receiverAddress, blockingStubFull2);
    long balanceBefore = beforeAccount.getBalance();

    // Send 3 transactions via node1
    for (int i = 0; i < 3; i++) {
      Assert.assertTrue(PublicMethod.sendcoin(receiverAddress, sendAmount,
          foundationAddress, foundationKey, blockingStubFull));
    }
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    // Verify on node2
    Account afterAccount = PublicMethod.queryAccount(receiverAddress, blockingStubFull2);
    long balanceAfter = afterAccount.getBalance();
    logger.info("Balance before: {}, after: {}", balanceBefore, balanceAfter);
    Assert.assertEquals(balanceAfter - balanceBefore, sendAmount * 3,
        "All 3 transactions should propagate to node2");
  }

  @AfterClass(enabled = true)
  public void afterClass() throws InterruptedException {
    PublicMethod.freeResource(receiverAddress, receiverKeyStr, foundationAddress, blockingStubFull);
    if (channelFull2 != null) {
      channelFull2.shutdown().awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
    }
  }
}
