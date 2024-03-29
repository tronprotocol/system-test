package stest.tron.wallet.dailybuild.separateExecution;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol;
import org.tron.protos.contract.BalanceContract.BlockBalanceTrace;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Utils;

@Slf4j

public class GetAccountBalance001 {
  private final String foundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] foundationAddress = PublicMethed.getFinalAddress(foundationKey);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] testAddress = ecKey1.getAddress();
  final String testKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] sendAddress = ecKey2.getAddress();
  final String sendKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  private Integer sendAmount = 1234;
  private String fullnode = Configuration.getByPath("testng.conf")
          .getStringList("fullnode.ip.list").get(0);
  Long beforeFromBalance;
  Long beforeToBalance;
  Long afterFromBalance;
  Long afterToBalance;
  private final String blackHoleAdd = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.blackHoleAddress");



  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext()
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    PublicMethed.sendcoin(sendAddress,100000000L,foundationAddress,foundationKey,
        blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, priority=1,description = "Test get account balance")
  public void test01GetAccountBalance() {
    Protocol.Block currentBlock = blockingStubFull
        .getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());

    beforeFromBalance = PublicMethed.getAccountBalance(currentBlock, sendAddress, blockingStubFull);
    beforeToBalance = PublicMethed.getAccountBalance(currentBlock, testAddress, blockingStubFull);


  }

  @Test(enabled = true, priority=1,description = "Test get block balance")
  public void test02GetBlockBalance() {
    String txid = PublicMethed.sendcoinGetTransactionId(testAddress, sendAmount, sendAddress,
        sendKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<Protocol.TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Long blockNum = infoById.get().getBlockNumber();

    Protocol.Block currentBlock = PublicMethed.getBlock(blockNum, blockingStubFull);

    BlockBalanceTrace blockBalanceTrace
        = PublicMethed.getBlockBalance(currentBlock, blockingStubFull);


    Assert.assertEquals(ByteString.copyFrom(sendAddress), blockBalanceTrace
        .getTransactionBalanceTrace(0).getOperation(0).getAddress());
    Assert.assertEquals(-100000L, blockBalanceTrace.getTransactionBalanceTrace(0)
        .getOperation(0).getAmount());


    Assert.assertEquals(ByteString.copyFrom(sendAddress), blockBalanceTrace
        .getTransactionBalanceTrace(0).getOperation(1).getAddress());
    Assert.assertEquals(-sendAmount - 1000000, blockBalanceTrace.getTransactionBalanceTrace(0)
        .getOperation(1).getAmount());



    Assert.assertEquals(ByteString.copyFrom(testAddress), blockBalanceTrace
        .getTransactionBalanceTrace(0).getOperation(2).getAddress());
    Assert.assertEquals(-sendAmount, -blockBalanceTrace.getTransactionBalanceTrace(0)
        .getOperation(2).getAmount());


    afterFromBalance = PublicMethed.getAccountBalance(currentBlock, sendAddress, blockingStubFull);
    afterToBalance = PublicMethed.getAccountBalance(currentBlock, testAddress, blockingStubFull);

    Assert.assertTrue(afterToBalance - beforeToBalance == sendAmount);
    Assert.assertTrue(beforeFromBalance - afterFromBalance >= sendAmount + 100000L);
  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethed.freedResource(testAddress, testKey, sendAddress, blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

}
