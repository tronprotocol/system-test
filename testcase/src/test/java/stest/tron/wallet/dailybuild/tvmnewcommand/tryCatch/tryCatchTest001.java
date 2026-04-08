package stest.tron.wallet.dailybuild.tvmnewcommand.tryCatch;

import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class tryCatchTest001 extends TronBaseTest {
  private String testFoundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private byte[] testFoundationAddress = PublicMethod.getFinalAddress(testFoundationKey);  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] testAddress001 = ecKey1.getAddress();
  String testKey001 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private byte[] contractAddress;
  private byte[] errorContractAddress;

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(testKey001);    PublicMethod
        .sendcoin(testAddress001, 10000_000_000L, testFoundationAddress, testFoundationKey,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "src/test/resources/soliditycode/tryCatch001.sol";
  String contractName = "tryTest";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    contractAddress = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit, 0, 100, null,
            testFoundationKey, testFoundationAddress, blockingStubFull);

    contractName = "errorContract";
    retMap = PublicMethod.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    errorContractAddress = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit, 0, 100, null,
            testFoundationKey, testFoundationAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

  }


  @Test(enabled = true,  description = "try catch  revert no msg", groups = {"contract", "daily"})
  public void tryCatchTest001() {
    String methodStr = "getErrorSwitch(address,uint256)";
  String argStr = "\"" + Base58.encode58Check(errorContractAddress) + "\",0";
  String TriggerTxid = PublicMethod.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethod
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals("NoErrorMsg", PublicMethod
        .getContractStringMsg(transactionInfo.get().getContractResult(0).toByteArray()));


  }

  @Test(enabled = true, description = "try catch  revert msg", groups = {"contract", "daily"})
  public void tryCatchTest002() {
    String methodStr = "getErrorSwitch(address,uint256)";
  String argStr = "\"" + Base58.encode58Check(errorContractAddress) + "\",1";
  String TriggerTxid = PublicMethod.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethod
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals("Revert Msg.", PublicMethod
        .getContractStringMsg(transactionInfo.get().getContractResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "try catch  Require no msg", groups = {"contract", "daily"})
  public void tryCatchTest003() {
    String methodStr = "getErrorSwitch(address,uint256)";
  String argStr = "\"" + Base58.encode58Check(errorContractAddress) + "\",2";
  String TriggerTxid = PublicMethod.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethod
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals("NoErrorMsg", PublicMethod
        .getContractStringMsg(transactionInfo.get().getContractResult(0).toByteArray()));

  }

  @Test(enabled = true, description = "try catch  Require msg", groups = {"contract", "daily"})
  public void tryCatchTest004() {
    String methodStr = "getErrorSwitch(address,uint256)";
  String argStr = "\"" + Base58.encode58Check(errorContractAddress) + "\",3";
  String TriggerTxid = PublicMethod.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethod
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals("Require Msg.", PublicMethod
        .getContractStringMsg(transactionInfo.get().getContractResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "try catch  assert", groups = {"contract", "daily"})
  public void tryCatchTest005() {
    String methodStr = "getErrorSwitch(address,uint256)";
  String argStr = "\"" + Base58.encode58Check(errorContractAddress) + "\",4";
  String TriggerTxid = PublicMethod.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethod
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertEquals(contractResult.SUCCESS,
        transactionInfo.get().getReceipt().getResult());

  }

  @Test(enabled = true, description = "try catch  transfer fail", groups = {"contract", "daily"})
  public void tryCatchTest006() {
    String methodStr = "getErrorSwitch(address,uint256)";
  String argStr = "\"" + Base58.encode58Check(errorContractAddress) + "\",5";
  String TriggerTxid = PublicMethod.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethod
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals("NoErrorMsg", PublicMethod
        .getContractStringMsg(transactionInfo.get().getContractResult(0).toByteArray()));

  }

  @Test(enabled = true, description = "try catch  Send_Error", groups = {"contract", "daily"})
  public void tryCatchTest007() {
    String methodStr = "getErrorSwitch(address,uint256)";
  String argStr = "\"" + Base58.encode58Check(errorContractAddress) + "\",6";
  String TriggerTxid = PublicMethod.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethod
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals("success", PublicMethod
        .getContractStringMsg(transactionInfo.get().getContractResult(0).toByteArray()));

  }

  @Test(enabled = true, description = "try catch  Math_Error", groups = {"contract", "daily"})
  public void tryCatchTest008() {
    String methodStr = "getErrorSwitch(address,uint256)";
  String argStr = "\"" + Base58.encode58Check(errorContractAddress) + "\",7";
  String TriggerTxid = PublicMethod.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethod
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertEquals(contractResult.SUCCESS,
        transactionInfo.get().getReceipt().getResult());

  }

  @Test(enabled = true, description = "try catch  ArrayOverFlow_Error", groups = {"contract", "daily"})
  public void tryCatchTest009() {
    String methodStr = "getErrorSwitch(address,uint256)";
  String argStr = "\"" + Base58.encode58Check(errorContractAddress) + "\",8";
  String TriggerTxid = PublicMethod.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethod
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertEquals(contractResult.SUCCESS,
        transactionInfo.get().getReceipt().getResult());

  }

}

