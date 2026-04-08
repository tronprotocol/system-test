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
 * Cross-node state consistency tests.
 *
 * <p>Verifies that account balance and contract state are consistent
 * when queried from different nodes after transactions are confirmed.
 */
@Slf4j
@MultiNode(reason = "Cross-node state consistency requires two nodes")
public class CrossNodeConsistencyTest extends TronBaseTest {

  private String fullnode2;
  private ManagedChannel channelFull2;
  private WalletGrpc.WalletBlockingStub blockingStubFull2;

  private ECKey testEcKey;
  private byte[] testAddress;
  private String testKeyStr;

  @BeforeClass(enabled = true)
  public void beforeClass() {
    fullnode2 = requireSecondFullnode();
    channelFull2 = ManagedChannelBuilder.forTarget(fullnode2).usePlaintext().build();
    blockingStubFull2 = WalletGrpc.newBlockingStub(channelFull2);

    testEcKey = new ECKey(Utils.getRandom());
    testAddress = testEcKey.getAddress();
    testKeyStr = ByteArray.toHexString(testEcKey.getPrivKeyBytes());

    // Fund the test account
    Assert.assertTrue(PublicMethod.sendcoin(testAddress, TronConstants.TEN_THOUSAND_TRX,
        foundationAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "Account balance should be consistent across nodes",
      groups = {"daily"})
  public void test01BalanceConsistency() {
    Account accOnNode1 = PublicMethod.queryAccount(testAddress, blockingStubFull);
    Account accOnNode2 = PublicMethod.queryAccount(testAddress, blockingStubFull2);

    long balance1 = accOnNode1.getBalance();
    long balance2 = accOnNode2.getBalance();
    logger.info("Balance on node1: {}, node2: {}", balance1, balance2);
    Assert.assertEquals(balance1, balance2,
        "Account balance should be identical on both nodes");
  }

  @Test(enabled = true, dependsOnMethods = "test01BalanceConsistency",
      description = "Balance update via node1 should be reflected on node2",
      groups = {"daily"})
  public void test02BalanceUpdatePropagation() {
    long sendAmount = 1_000_000L;

    ECKey receiverKey = new ECKey(Utils.getRandom());
    byte[] receiverAddr = receiverKey.getAddress();

    // Transfer via node1
    Assert.assertTrue(PublicMethod.sendcoin(receiverAddr, sendAmount,
        testAddress, testKeyStr, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    // Check sender balance consistency
    Account senderOnNode1 = PublicMethod.queryAccount(testAddress, blockingStubFull);
    Account senderOnNode2 = PublicMethod.queryAccount(testAddress, blockingStubFull2);
    Assert.assertEquals(senderOnNode1.getBalance(), senderOnNode2.getBalance(),
        "Sender balance should be consistent after transfer");

    // Check receiver balance consistency
    Account receiverOnNode1 = PublicMethod.queryAccount(receiverAddr, blockingStubFull);
    Account receiverOnNode2 = PublicMethod.queryAccount(receiverAddr, blockingStubFull2);
    Assert.assertEquals(receiverOnNode1.getBalance(), receiverOnNode2.getBalance(),
        "Receiver balance should be consistent after transfer");
    Assert.assertEquals(receiverOnNode1.getBalance(), sendAmount,
        "Receiver should have received the exact amount");
    logger.info("Balance update propagated correctly to both nodes");
  }

  @Test(enabled = true, description = "Foundation account should be consistent on both nodes",
      groups = {"daily"})
  public void test03FoundationAccountConsistency() {
    Account foundOnNode1 = PublicMethod.queryAccount(foundationAddress, blockingStubFull);
    Account foundOnNode2 = PublicMethod.queryAccount(foundationAddress, blockingStubFull2);

    Assert.assertEquals(foundOnNode1.getBalance(), foundOnNode2.getBalance(),
        "Foundation balance should be identical on both nodes");
    Assert.assertEquals(foundOnNode1.getAccountName(), foundOnNode2.getAccountName(),
        "Foundation account name should match");
    logger.info("Foundation account consistent: balance={}",
        foundOnNode1.getBalance());
  }

  @AfterClass(enabled = true)
  public void afterClass() throws InterruptedException {
    PublicMethod.freeResource(testAddress, testKeyStr, foundationAddress, blockingStubFull);
    if (channelFull2 != null) {
      channelFull2.shutdown().awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
    }
  }
}
