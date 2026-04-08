package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;
@Slf4j
public class LengthTest extends TronBaseTest {
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
        .sendcoin(testAddress001, 10000_000_000L, testFoundationAddress, testFoundationKey,
            blockingStubFull);
  String filePath = "src/test/resources/soliditycode/arrayLength001.sol";
  String contractName = "arrayLength";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    contractAddress = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit, 0, 100, null,
            testFoundationKey, testFoundationAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "push() increase Array length", groups = {"contract", "daily"})
  public void arrayLengthTest001() {

    String methodStr = "arrayPush()";
  String argStr = "";
  String TriggerTxid = PublicMethod.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethod
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0,transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals(""
        + "0000000000000000000000000000000000000000000000000000000000000020"
        + "0000000000000000000000000000000000000000000000000000000000000002"
        + "0000000000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000",
        ByteArray.toHexString(transactionInfo.get().getContractResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "push(value) increase Array length", groups = {"contract", "daily"})
  public void arrayLengthTest002() {

    String methodStr = "arrayPushValue()";
  String argStr = "";
  String TriggerTxid = PublicMethod.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethod
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0,transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals(""
            + "0000000000000000000000000000000000000000000000000000000000000020"
            + "0000000000000000000000000000000000000000000000000000000000000002"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0100000000000000000000000000000000000000000000000000000000000000",
        ByteArray.toHexString(transactionInfo.get().getContractResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "pop() decrease Array length", groups = {"contract", "daily"})
  public void arrayLengthTest003() {

    String methodStr = "arrayPop()";
  String argStr = "";
  String TriggerTxid = PublicMethod.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethod
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0,transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals(""
            + "0000000000000000000000000000000000000000000000000000000000000020"
            + "0000000000000000000000000000000000000000000000000000000000000000",
        ByteArray.toHexString(transactionInfo.get().getContractResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "push() return no value", groups = {"contract", "daily"})
  public void arrayLengthTest004() {

    String methodStr = "arrayPushReturn()";
  String argStr = "";
  String TriggerTxid = PublicMethod.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethod
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0,transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals(""
            + "0000000000000000000000000000000000000000000000000000000000000000",
        ByteArray.toHexString(transactionInfo.get().getContractResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "push(value) return value", groups = {"contract", "daily"})
  public void arrayLengthTest005() {

    String methodStr = "arrayPushValueReturn()";
  String argStr = "";
  String TriggerTxid = PublicMethod.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethod
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0,transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals("",
        ByteArray.toHexString(transactionInfo.get().getContractResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "pop() return no value", groups = {"contract", "daily"})
  public void arrayLengthTest006() {

    String methodStr = "arrayPopReturn()";
  String argStr = "";
  String TriggerTxid = PublicMethod.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethod
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0,transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals("",
        ByteArray.toHexString(transactionInfo.get().getContractResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "bytes push() return value", groups = {"contract", "daily"})
  public void arrayLengthTest007() {

    String methodStr = "bytesPush()";
  String argStr = "";
  String TriggerTxid = PublicMethod.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethod
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0,transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals(""
            + "0000000000000000000000000000000000000000000000000000000000000000",
        ByteArray.toHexString(transactionInfo.get().getContractResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "bytes push(value) return no value", groups = {"contract", "daily"})
  public void arrayLengthTest008() {

    String methodStr = "bytesPushValue()";
  String argStr = "";
  String TriggerTxid = PublicMethod.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethod
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0,transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals("",
        ByteArray.toHexString(transactionInfo.get().getContractResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "bytes pop() return no value", groups = {"contract", "daily"})
  public void arrayLengthTest009() {

    String methodStr = "bytesPop()";
  String argStr = "";
  String TriggerTxid = PublicMethod.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethod
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0,transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals("",
        ByteArray.toHexString(transactionInfo.get().getContractResult(0).toByteArray()));
  }


  @Test(enabled = true, description = "array length change before v0.5.15", groups = {"contract", "daily"})
  public void arrayLengthV0515() {
    String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_arrayLenth_0.5.15");
  String code = Configuration.getByPath("testng.conf")
        .getString("code.code_arrayLength_0.5.15");
  String contractName = "arrayLength";
  byte[] v0515Address = PublicMethod.deployContract(contractName,abi,code,"",maxFeeLimit,0,100,
        null, testKey001, testAddress001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String Txid = PublicMethod.triggerContract(v0515Address,"ChangeSize()","",false,0,maxFeeLimit,
        testAddress001,testKey001,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethod
        .getTransactionInfoById(Txid, blockingStubFull);

    Assert.assertEquals(0,transactionInfo.get().getResultValue());
    Assert.assertEquals(""
        + "0000000000000000000000000000000000000000000000000000000000000020"
        + "0000000000000000000000000000000000000000000000000000000000000001"
        + "0100000000000000000000000000000000000000000000000000000000000000",
        ByteArray.toHexString(transactionInfo.get().getContractResult(0).toByteArray()));

  }


}
