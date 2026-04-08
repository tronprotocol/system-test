package stest.tron.wallet.dailybuild.separateExecution;

import com.google.protobuf.ByteString;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.protos.Protocol;
import org.tron.protos.contract.BalanceContract.BlockBalanceTrace;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j

public class GetAccountBalance001 extends TronBaseTest {  private final byte[] foundationAddress = PublicMethod.getFinalAddress(foundationKey);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] testAddress = ecKey1.getAddress();
  final String testKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] sendAddress = ecKey2.getAddress();
  final String sendKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  private Integer sendAmount = 1234;
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
  public void beforeClass() {    PublicMethod.sendcoin(sendAddress,100000000L,foundationAddress,foundationKey,
        blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, priority=1,description = "Test get account balance", groups = {"daily", "serial"})
  public void test01GetAccountBalance() {
    Protocol.Block currentBlock = blockingStubFull
        .getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());

    beforeFromBalance = PublicMethod.getAccountBalance(currentBlock, sendAddress, blockingStubFull);
    beforeToBalance = PublicMethod.getAccountBalance(currentBlock, testAddress, blockingStubFull);


  }

  @Test(enabled = true, priority=1,description = "Test get block balance", groups = {"daily", "serial"})
  public void test02GetBlockBalance() {
    String txid = PublicMethod.sendcoinGetTransactionId(testAddress, sendAmount, sendAddress,
        sendKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<Protocol.TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
  Long blockNum = infoById.get().getBlockNumber();

    Protocol.Block currentBlock = PublicMethod.getBlock(blockNum, blockingStubFull);

    BlockBalanceTrace blockBalanceTrace
        = PublicMethod.getBlockBalance(currentBlock, blockingStubFull);


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


    afterFromBalance = PublicMethod.getAccountBalance(currentBlock, sendAddress, blockingStubFull);
    afterToBalance = PublicMethod.getAccountBalance(currentBlock, testAddress, blockingStubFull);

    Assert.assertTrue(afterToBalance - beforeToBalance == sendAmount);
    Assert.assertTrue(beforeFromBalance - afterFromBalance >= sendAmount + 100000L);
  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(testAddress, testKey, sendAddress, blockingStubFull);  }

}
