package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.protos.Protocol;
import org.tron.protos.contract.SmartContractOuterClass;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class EthGrammar02 extends TronBaseTest {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethod.getFinalAddress(testNetAccountKey);
  byte[] contractD = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  int salt = 11;

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(contractExcKey);    Assert.assertTrue(PublicMethod
        .sendcoin(contractExcAddress, 300100_000_000L,
            testNetAccountAddress, testNetAccountKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "src/test/resources/soliditycode/EthGrammar02.sol";
  String contractName = "D";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    contractD = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        500000000L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethod.getContract(contractD,
        blockingStubFull);
    Assert.assertEquals(1, smartContract.getVersion());
    Assert.assertNotNull(smartContract.getAbi());
  }

  @Test(enabled = true, description = "can not deploy contract with bytecode ef", groups = {"contract", "daily"})
  public void test16forbiddenBytecodeStartWithEf() {
    String code = "60ef60005360016000f3";
  String abi = "[{\"inputs\":[],\"stateMutability\":\"payable\",\"type\":\"constructor\"}]";
  String txid = PublicMethod.deployContractAndGetTransactionInfoById("test",
        abi, code, "", maxFeeLimit,
        500000000L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("info: " + info.get().toString());
    Assert.assertEquals(1, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.INVALID_CODE,
        info.get().getReceipt().getResult());
    Assert.assertEquals("invalid code: must not begin with 0xef".toLowerCase(),
        ByteArray.toStr(info.get().getResMessage().toByteArray()).toLowerCase());
  }

  @Test(enabled = true, description = "can not deploy contract with bytecode ef00", groups = {"contract", "daily"})
  public void test17forbiddenBytecodeStartWithEf() {
    String code = "60ef60005360026000f3";
  String abi = "[{\"inputs\":[],\"stateMutability\":\"payable\",\"type\":\"constructor\"}]";
  String txid = PublicMethod.deployContractAndGetTransactionInfoById("test",
        abi, code, "", maxFeeLimit, 500000000L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("info: " + info.get().toString());
    Assert.assertEquals(1, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.INVALID_CODE,
        info.get().getReceipt().getResult());
    Assert.assertEquals("invalid code: must not begin with 0xef".toLowerCase(),
        ByteArray.toStr(info.get().getResMessage().toByteArray()).toLowerCase());
  }

  @Test(enabled = true, description = "can not deploy contract with bytecode ef0000", groups = {"contract", "daily"})
  public void test18forbiddenBytecodeStartWithEf() {
    String code = "60ef60005360036000f3";
  String abi = "[{\"inputs\":[],\"stateMutability\":\"payable\",\"type\":\"constructor\"}]";
  String txid = PublicMethod.deployContractAndGetTransactionInfoById("test", abi,
        code, "", maxFeeLimit, 500000000L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("info: " + info.get().toString());
    Assert.assertEquals(1, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.INVALID_CODE,
        info.get().getReceipt().getResult());
    Assert.assertEquals("invalid code: must not begin with 0xef".toLowerCase(),
        ByteArray.toStr(info.get().getResMessage().toByteArray()).toLowerCase());
  }

  @Test(enabled = true, description = "can not deploy contract with bytecode"
      + " ef00000000000000000000000000000000000000000000000000000000000000", groups = {"contract", "daily"})
  public void test19forbiddenBytecodeStartWithEf() {
    String code = "60ef60005360206000f3";
  String abi = "[{\"inputs\":[],\"stateMutability\":\"payable\",\"type\":\"constructor\"}]";
  String txid = PublicMethod.deployContractAndGetTransactionInfoById("test", abi,
        code, "", maxFeeLimit, 500000000L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("info: " + info.get().toString());
    Assert.assertEquals(1, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.INVALID_CODE,
        info.get().getReceipt().getResult());
    Assert.assertEquals("invalid code: must not begin with 0xef".toLowerCase(),
        ByteArray.toStr(info.get().getResMessage().toByteArray()).toLowerCase());
  }

  @Test(enabled = true, description = "can deploy contract with bytecode fe", groups = {"contract", "daily"})
  public void test20forbiddenBytecodeStartWithEf() {
    String code = "60fe60005360016000f3";
  String abi = "[{\"inputs\":[],\"stateMutability\":\"payable\",\"type\":\"constructor\"}]";
  String txid = PublicMethod.deployContractAndGetTransactionInfoById("test", abi,
        code, "", maxFeeLimit, 500000000L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("info: " + info.get().toString());
    Assert.assertEquals(0, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        info.get().getReceipt().getResult());
  }

  @Test(enabled = true, description = "can not deploy contract by create with bytecode ef", groups = {"contract", "daily"})
  public void test21forbiddenBytecodeStartWithEf() {
    String methedStr = "createDeployEf(bytes)";
  String argsStr = "\"0x60ef60005360016000f3\"";
  String txid = PublicMethod.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress,
        contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(1, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.REVERT,
        info.get().getReceipt().getResult());
  }

  @Test(enabled = true, description = "can not deploy contract by create with bytecode ef00", groups = {"contract", "daily"})
  public void test22forbiddenBytecodeStartWithEf() {
    String methedStr = "createDeployEf(bytes)";
  String argsStr = "\"0x60ef60005360026000f3\"";
  String txid = PublicMethod.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress,
        contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(1, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.REVERT,
        info.get().getReceipt().getResult());
  }

  @Test(enabled = true, description = "can not deploy contract by create with bytecode ef0000", groups = {"contract", "daily"})
  public void test23forbiddenBytecodeStartWithEf() {
    String methedStr = "createDeployEf(bytes)";
  String argsStr = "\"0x60ef60005360036000f3\"";
  String txid = PublicMethod.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress,
        contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(1, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.REVERT,
        info.get().getReceipt().getResult());
  }

  @Test(enabled = true, description = "can not deploy contract by create with bytecode "
      + "ef00000000000000000000000000000000000000000000000000000000000000", groups = {"contract", "daily"})
  public void test24forbiddenBytecodeStartWithEf() {
    String methedStr = "createDeployEf(bytes)";
  String argsStr = "\"0x60ef60005360206000f3\"";
  String txid = PublicMethod.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress,
        contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(1, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.REVERT,
        info.get().getReceipt().getResult());
  }

  @Test(enabled = true, description = "can deploy contract by create with bytecode fe", groups = {"contract", "daily"})
  public void test25forbiddenBytecodeStartWithEf() {
    String methedStr = "createDeployEf(bytes)";
  String argsStr = "\"0x60fe60005360016000f3\"";
  String txid = PublicMethod.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress,
        contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        info.get().getReceipt().getResult());
  }

  @Test(enabled = true, description = "can not deploy contract by create2 with bytecode ef", groups = {"contract", "daily"})
  public void test26forbiddenBytecodeStartWithEf() {
    String methedStr = "create2DeployEf(bytes,uint256)";
  String argsStr = "\"0x60ef60005360016000f3\"," + salt;
  String txid = PublicMethod.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress,
        contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(1, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.REVERT,
        info.get().getReceipt().getResult());
  }

  @Test(enabled = true, description = "can not deploy contract by create2 with bytecode ef00", groups = {"contract", "daily"})
  public void test27forbiddenBytecodeStartWithEf() {
    salt++;
  String methedStr = "create2DeployEf(bytes,uint256)";
  String argsStr = "\"0x60ef60005360026000f3\"," + salt;
  String txid = PublicMethod.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress,
        contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(1, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.REVERT,
        info.get().getReceipt().getResult());
  }

  @Test(enabled = true, description = "can not deploy contract by create2 with bytecode ef0000", groups = {"contract", "daily"})
  public void test28forbiddenBytecodeStartWithEf() {
    salt++;
  String methedStr = "create2DeployEf(bytes,uint256)";
  String argsStr = "\"0x60ef60005360036000f3\"," + salt;
  String txid = PublicMethod.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress,
        contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(1, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.REVERT,
        info.get().getReceipt().getResult());
  }

  @Test(enabled = true, description = "can not deploy contract by create2 with bytecode "
      + "ef00000000000000000000000000000000000000000000000000000000000000", groups = {"contract", "daily"})
  public void test29forbiddenBytecodeStartWithEf() {
    salt++;
  String methedStr = "create2DeployEf(bytes,uint256)";
  String argsStr = "\"0x60ef60005360206000f3\"," + salt;
  String txid = PublicMethod.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress,
        contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(1, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.REVERT,
        info.get().getReceipt().getResult());
  }

  @Test(enabled = true, description = "can deploy contract by create2 with bytecode fe", groups = {"contract", "daily"})
  public void test30forbiddenBytecodeStartWithEf() {
    salt++;
  String methedStr = "create2DeployEf(bytes,uint256)";
  String argsStr = "\"0x60fe60005360016000f3\"," + salt;
  String txid = PublicMethod.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress,
        contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        info.get().getReceipt().getResult());
  }

  @Test(enabled = true, description = "can not sendcoin to contract", groups = {"contract", "daily"})
  public void test31forbiddenSendTrxToContract() {
    Assert.assertFalse(PublicMethod
        .sendcoin(contractD, 100_000_000L,
            testNetAccountAddress, testNetAccountKey, blockingStubFull));
  }

  @Test(enabled = true, description = "db key can use high 16 bytes,"
      + "0x6162630000000000000000000000000000000000000000000000000000000000", groups = {"contract", "daily"})
  public void test32DbKeyUseHigh16Bytes() {
    String slot = "0x6162630000000000000000000000000000000000000000000000000000000000";
    long value = 121;
  String methedStr = "setSlot(bytes,uint256)";
  String argsStr = "\"" + slot + "\"," + value;
  String txid = PublicMethod.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress,
        contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        info.get().getReceipt().getResult());

    methedStr = "getSlot(bytes)";
    argsStr = "\"" + slot + "\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractD,
            methedStr, argsStr, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    long result = ByteArray.toLong(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("result: " + result);
    Assert.assertEquals(value, result);
  }

  @Test(enabled = true, description = "slot high 16bytes all f,"
      + "0xffffffffffffffffffffffffffffffff00000000000000000000000000000000", groups = {"contract", "daily"})
  public void test33DbKeyUseHigh16Bytes() {
    String slot = "0xffffffffffffffffffffffffffffffff00000000000000000000000000000000";
    long value = 122;
  String methedStr = "setSlot(bytes,uint256)";
  String argsStr = "\"" + slot + "\"," + value;
  String txid = PublicMethod.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress,
        contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        info.get().getReceipt().getResult());

    methedStr = "getSlot(bytes)";
    argsStr = "\"" + slot + "\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractD,
            methedStr, argsStr, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    long result = ByteArray.toLong(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("result: " + result);
    Assert.assertEquals(value, result);
  }

  @Test(enabled = true, description = "slot high 16bytes 1,"
      + " 0x0000000000000000000000000000000100000000000000000000000000000000", groups = {"contract", "daily"})
  public void test34DbKeyUseHigh16Bytes() {
    String slot = "0x0000000000000000000000000000000100000000000000000000000000000000";
    long value = 123;
  String methedStr = "setSlot(bytes,uint256)";
  String argsStr = "\"" + slot + "\"," + value;
  String txid = PublicMethod.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress,
        contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        info.get().getReceipt().getResult());

    methedStr = "getSlot(bytes)";
    argsStr = "\"" + slot + "\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractD,
            methedStr, argsStr, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    long result = ByteArray.toLong(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("result: " + result);
    Assert.assertEquals(value, result);
  }

  @Test(enabled = true, description = "slot high 16bytes all 0,low 16bytes 1."
      + " 0x0000000000000000000000000000000000000000000000000000000000000001", groups = {"contract", "daily"})
  public void test35DbKeyUseHigh16Bytes() {
    String slot = "0x0000000000000000000000000000000000000000000000000000000000000001";
    long value = 124;
  String methedStr = "setSlot(bytes,uint256)";
  String argsStr = "\"" + slot + "\"," + value;
  String txid = PublicMethod.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress,
        contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        info.get().getReceipt().getResult());

    methedStr = "getSlot(bytes)";
    argsStr = "\"" + slot + "\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractD,
            methedStr, argsStr, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    long result = ByteArray.toLong(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("result: " + result);
    Assert.assertEquals(value, result);
  }

  @Test(enabled = true, description = "slot all 0,"
      + " 0x0000000000000000000000000000000000000000000000000000000000000000", groups = {"contract", "daily"})
  public void test36DbKeyUseHigh16BytesAllBytes0() {
    String slot = "0x0000000000000000000000000000000000000000000000000000000000000000";
    long value = 125;
  String methedStr = "setSlot(bytes,uint256)";
  String argsStr = "\"" + slot + "\"," + value;
  String txid = PublicMethod.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress,
        contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        info.get().getReceipt().getResult());

    methedStr = "getSlot(bytes)";
    argsStr = "\"" + slot + "\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractD,
            methedStr, argsStr, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    long result = ByteArray.toLong(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("result: " + result);
    Assert.assertEquals(value, result);
  }

  @Test(enabled = true, description = "slot all f,"
      + " 0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", groups = {"contract", "daily"})
  public void test37DbKeyUseHigh16BytesAllBytesF() {
    String slot = "0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";
    long value = 126;
  String methedStr = "setSlot(bytes,uint256)";
  String argsStr = "\"" + slot + "\"," + value;
  String txid = PublicMethod.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress,
        contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        info.get().getReceipt().getResult());

    methedStr = "getSlot(bytes)";
    argsStr = "\"" + slot + "\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractD,
            methedStr, argsStr, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    long result = ByteArray.toLong(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("result: " + result);
    Assert.assertEquals(value, result);
  }

  @Test(enabled = true, description = "TransactionExtention has logs and internal_transactions", groups = {"contract", "daily"})
  public void test38ConstantLogEven() {
    salt++;
  String methedStr = "create2DeployEf(bytes,uint256)";
  String argsStr = "\"0x60fe60005360016000f3\"," + salt;
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractD,
            methedStr, argsStr, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(1, transactionExtention.getLogsCount());
    Assert.assertEquals(1, transactionExtention.getInternalTransactionsCount());
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(contractExcAddress, contractExcKey,
        testNetAccountAddress, blockingStubFull);  }


}

