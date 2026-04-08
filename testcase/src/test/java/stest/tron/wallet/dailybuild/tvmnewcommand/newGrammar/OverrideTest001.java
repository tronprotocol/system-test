package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.MultiNode;
@Slf4j
@MultiNode
public class OverrideTest001 extends TronBaseTest {

  private final byte[] fromAddress = PublicMethod.getFinalAddress(testKey002);
  private String fullnodeLocal = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private byte[] contractAddress = null;
  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] dev001Address = ecKey1.getAddress();
  private String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {    PublicMethod.printAddress(dev001Key);
    Assert.assertTrue(PublicMethod.sendcoin(dev001Address, 1000_000_000L, fromAddress,
        testKey002, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "Deploy 0.5.15 about override(Base1,Base2)", groups = {"contract", "daily"})
  public void test01OverrideContract515() {
    String contractName = "override001";
  String code = Configuration.getByPath("testng.conf")
        .getString("code.code_override001");
  String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_override001");
  String txid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            "0", 0, null, dev001Key,
            dev001Address, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod.getTransactionInfoById(txid,
        blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());
    contractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethod.getContract(contractAddress,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    txid = PublicMethod.triggerContract(contractAddress, "setValue(uint256)", "5", false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());

    TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "x()", "#",
            false, 0, 0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert
        .assertEquals(0, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));

    transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "y()", "#",
            false, 0, 0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert
        .assertEquals(5, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));

  }

  @Test(enabled = true, description = "Deploy 0.6.0 about not need override", groups = {"contract", "daily"})
  public void test02NotNeedOverride() {
    String filePath = "./src/test/resources/soliditycode/override002.sol";
  String contractName = "D";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  String txid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            "0", 0, null, dev001Key,
            dev001Address, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod.getTransactionInfoById(txid,
        blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());
    contractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethod.getContract(contractAddress,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    txid = PublicMethod.triggerContract(contractAddress, "setValue(uint256)", "5", false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());

    TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "x()", "#",
            false, 0, 0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert
        .assertEquals(5, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "Deploy 0.6.0 about override(Base1,Base2)", groups = {"contract", "daily"})
  public void test03OverrideMultipleFunctionsWithTheSameName() {
    String filePath = "./src/test/resources/soliditycode/override003.sol";
  String contractName = "C";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  String txid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            "0", 0, null, dev001Key,
            dev001Address, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod.getTransactionInfoById(txid,
        blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());
    contractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethod.getContract(contractAddress,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    txid = PublicMethod.triggerContract(contractAddress, "setValue(uint256)", "5", false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());

    TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "x()", "#",
            false, 0, 0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert
        .assertEquals(5, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));

    transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "y()", "#",
            false, 0, 0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert
        .assertEquals(0, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "Deploy 0.6.0 about override modifier", groups = {"contract", "daily"})
  public void test04OverrideModifier060() {
    String filePath = "./src/test/resources/soliditycode/override004.sol";
  String contractName = "C";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  String txid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            "0", 0, null, dev001Key,
            dev001Address, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod.getTransactionInfoById(txid,
        blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());
    contractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethod.getContract(contractAddress,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    txid = PublicMethod.triggerContract(contractAddress, "setValue(uint256)", "7", false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(1,infoById.get().getResultValue());
    Assert.assertTrue(infoById.get().getContractResult(0).toStringUtf8().contains("x must >= 6"));

    txid = PublicMethod.triggerContract(contractAddress, "setValue2(uint256)", "6", false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());

    txid = PublicMethod.triggerContract(contractAddress, "setValue(uint256)", "8", false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());

    TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "x()", "#",
            false, 0, 0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert
        .assertEquals(8, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "Deploy 0.5.15 about override modifier", groups = {"contract", "daily"})
  public void test05OverrideModifier515() {
    String contractName = "C";
  String code = Configuration.getByPath("testng.conf")
        .getString("code.code_override002");
  String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_override002");
  String txid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            "0", 0, null, dev001Key,
            dev001Address, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod.getTransactionInfoById(txid,
        blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());
    contractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethod.getContract(contractAddress,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    txid = PublicMethod.triggerContract(contractAddress, "setValue(uint256)", "7", false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(1,infoById.get().getResultValue());
    Assert.assertTrue(infoById.get().getContractResult(0).toStringUtf8().contains("x must >= 6"));

    txid = PublicMethod.triggerContract(contractAddress, "setValue2(uint256)", "6", false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());

    txid = PublicMethod.triggerContract(contractAddress, "setValue(uint256)", "8", false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());

    TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "x()", "#",
            false, 0, 0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert
        .assertEquals(8, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "Deploy 0.6.0 public override external function", groups = {"contract", "daily"})
  public void test06PublicOverrideExternalFunction060() {
    String filePath = "./src/test/resources/soliditycode/override005.sol";
  String contractName = "Test";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  String txid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            "0", 0, null, dev001Key,
            dev001Address, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod.getTransactionInfoById(txid,
        blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());
    contractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethod.getContract(contractAddress,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "stopped()", "#",
            false, 0, 0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert
        .assertEquals(0, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));

    transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "i()", "#",
            false, 0, 0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert
        .assertEquals(32482989, ByteArray.toInt(transactionExtention.getConstantResult(0)
            .toByteArray()));

    transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "i2()", "#",
            false, 0, 0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert
        .assertEquals(-32482989, ByteArray.toInt(transactionExtention.getConstantResult(0)
            .toByteArray()));

    transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "ui()", "#",
            false, 0, 0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert
        .assertEquals(23487823, ByteArray.toInt(transactionExtention.getConstantResult(0)
            .toByteArray()));

    transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "origin()", "#",
            false, 0, 0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
  byte[] tmpAddress = new byte[20];
    System
        .arraycopy(transactionExtention.getConstantResult(0).toByteArray(), 12, tmpAddress, 0, 20);
    Assert.assertEquals("TW63BNR5M7LuH1fjXS7Smyza3PZXfHAAs2",
        Base58.encode58Check(ByteArray.fromHexString("41" + ByteArray.toHexString(tmpAddress))));

    transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "b32()", "#",
            false, 0, 0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert.assertEquals("b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105c",
        ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));

    transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "choice()", "#",
            false, 0, 0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000003",
        ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "Deploy 0.5.15 public override external function", groups = {"contract", "daily"})
  public void test07PublicOverrideExternalFunction515() {
    String contractName = "Test";
  String code = Configuration.getByPath("testng.conf")
        .getString("code.code_override003");
  String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_override003");
  String txid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            "0", 0, null, dev001Key,
            dev001Address, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod.getTransactionInfoById(txid,
        blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());
    contractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethod.getContract(contractAddress,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "stopped()", "#",
            false, 0, 0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert
        .assertEquals(0, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));

    transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "i()", "#",
            false, 0, 0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert
        .assertEquals(32482989, ByteArray.toInt(transactionExtention.getConstantResult(0)
            .toByteArray()));

    transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "i2()", "#",
            false, 0, 0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert
        .assertEquals(-32482989, ByteArray.toInt(transactionExtention.getConstantResult(0)
            .toByteArray()));

    transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "ui()", "#",
            false, 0, 0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert
        .assertEquals(23487823, ByteArray.toInt(transactionExtention.getConstantResult(0)
            .toByteArray()));

    transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "origin()", "#",
            false, 0, 0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
  byte[] tmpAddress = new byte[20];
    System
        .arraycopy(transactionExtention.getConstantResult(0).toByteArray(), 12, tmpAddress, 0, 20);
    Assert.assertEquals("TW63BNR5M7LuH1fjXS7Smyza3PZXfHAAs2",
        Base58.encode58Check(ByteArray.fromHexString("41" + ByteArray.toHexString(tmpAddress))));

    transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "b32()", "#",
            false, 0, 0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert.assertEquals("b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105c",
        ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));

    transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "choice()", "#",
            false, 0, 0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000003",
        ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));
  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    long balance = PublicMethod.queryAccount(dev001Key, blockingStubFull).getBalance();
    PublicMethod.sendcoin(fromAddress, balance, dev001Address, dev001Key,
        blockingStubFull);  }
}


