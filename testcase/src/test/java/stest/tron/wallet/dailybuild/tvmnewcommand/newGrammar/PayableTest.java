package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;
@Slf4j
public class PayableTest extends TronBaseTest {
  private String testFoundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private byte[] testFoundationAddress = PublicMethod.getFinalAddress(testFoundationKey);  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] testAddress001 = ecKey1.getAddress();
  String testKey001 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private byte[] contractAddress;

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(testKey001);    PublicMethod
        .sendcoin(testAddress001, 1000_000_000L, testFoundationAddress, testFoundationKey,
            blockingStubFull);
  String filePath = "src/test/resources/soliditycode/payable001.sol";
  String contractName = "PayableTest";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    contractAddress = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit, 10000, 100, null,
            testFoundationKey, testFoundationAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "payable(address) transfer", groups = {"contract", "daily"})
  public void tryCatchTest001() {

    Account account = PublicMethod
        .queryAccount(PublicMethod.decode58Check(
            "TBXSw8fM4jpQkGc6zZjsVABFpVN7UvXPdV"), blockingStubFull);
  Long balanceBefore = account.getBalance();
  String methodStr = "receiveMoneyTransfer(address,uint256)";
  String argStr = "\"TBXSw8fM4jpQkGc6zZjsVABFpVN7UvXPdV\",3";
  String TriggerTxid = PublicMethod.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethod
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0,transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
  Long balanceAfter = PublicMethod.queryAccount(PublicMethod.decode58Check(
        "TBXSw8fM4jpQkGc6zZjsVABFpVN7UvXPdV"), blockingStubFull).getBalance();
    Assert.assertEquals(balanceBefore + 3,balanceAfter.longValue());
  }

  @Test(enabled = true, description = "payable(address) send", groups = {"contract", "daily"})
  public void tryCatchTest002() {

    Account account = PublicMethod
        .queryAccount(PublicMethod.decode58Check(
            "TBXSw8fM4jpQkGc6zZjsVABFpVN7UvXPdV"), blockingStubFull);
  Long balanceBefore = account.getBalance();
  String methodStr = "receiveMoneySend(address,uint256)";
  String argStr = "\"TBXSw8fM4jpQkGc6zZjsVABFpVN7UvXPdV\",3";
  String TriggerTxid = PublicMethod.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethod
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0,transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
  Long balanceAfter = PublicMethod.queryAccount(PublicMethod.decode58Check(
        "TBXSw8fM4jpQkGc6zZjsVABFpVN7UvXPdV"), blockingStubFull).getBalance();
    Assert.assertEquals(balanceBefore + 3,balanceAfter.longValue());
  }

  @Test(enabled = true, description = "payable(address(contract)) transfer", groups = {"contract", "daily"})
  public void tryCatchTest003() {

    String filePath = "src/test/resources/soliditycode/payable001.sol";
  String contractName = "A";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  byte[] AContract = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit, 0, 100, null,
            testKey001, testAddress001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);


    Account account = PublicMethod
        .queryAccount(AContract, blockingStubFull);
  Long balanceBefore = account.getBalance();
  String methodStr = "receiveMoneyTransferWithContract(address,uint256)";
  String argStr = "\"" + Base58.encode58Check(AContract) + "\",3";
  String TriggerTxid = PublicMethod.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethod
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0,transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
  Long balanceAfter = PublicMethod.queryAccount(AContract, blockingStubFull).getBalance();
    Assert.assertEquals(balanceBefore + 3,balanceAfter.longValue());
  }

}
