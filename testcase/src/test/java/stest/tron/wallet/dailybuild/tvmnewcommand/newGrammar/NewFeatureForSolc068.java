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
public class NewFeatureForSolc068 extends TronBaseTest {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethod.getFinalAddress(testNetAccountKey);
  byte[] getSelectorContract = null;
  byte[] mapKeyContract = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(contractExcKey);    Assert.assertTrue(PublicMethod
        .sendcoin(contractExcAddress, 300100_000_000L,
            testNetAccountAddress, testNetAccountKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "src/test/resources/soliditycode/NewFeature068.sol";
  String contractName = "testMapKey";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    mapKeyContract = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethod.getContract(mapKeyContract,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
  }


  @Test(enabled = true, description = "map with enum key", groups = {"contract", "daily"})
  public void test01MapWithEnumKey() {
    String txid = PublicMethod.triggerContract(mapKeyContract,
        "setEnumValue(uint256)", "1", false,
        0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("txid: " + txid + "\n" + infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());


    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(mapKeyContract,
            "getEnumValue()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Protocol.Transaction transaction = transactionExtention.getTransaction();
  int trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("truerRes: " + trueRes + "   message:" + transaction.getRet(0).getRet());
    Assert.assertEquals(1, trueRes);
  }

  @Test(enabled = true, description = "map with contract key", groups = {"contract", "daily"})
  public void test02MapWithContractKey() {

    String txid = PublicMethod.triggerContract(mapKeyContract,
        "setContractValue()", "#", false,
        0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("txid: " + txid + "\n" + infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());


    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(mapKeyContract,
            "getContractValue()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Protocol.Transaction transaction = transactionExtention.getTransaction();
  int trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("truerRes: " + trueRes + "   message:" + transaction.getRet(0).getRet());
    Assert.assertEquals(2, trueRes);
  }

  @Test(enabled = true, description = "get function selector during compile period", groups = {"contract", "daily"})
  public void test03GetSelectorDuringCompile() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(mapKeyContract,
            "getfunctionSelector()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Protocol.Transaction transaction = transactionExtention.getTransaction();
  String trueRes = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("trueRes: " + trueRes + "   message:" + transaction.getRet(0).getRet());
    Assert.assertTrue(trueRes.startsWith("48593bae"));
  }

  @Test(enabled = true, description = "test storage variable init before been used", groups = {"contract", "daily"})
  public void test04StorageValInit() {

    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(mapKeyContract,
            "testStorage()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Protocol.Transaction transaction = transactionExtention.getTransaction();
  String trueRes = PublicMethod
        .getContractStringMsg(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("truerRes: " + trueRes + "   message:" + transaction.getRet(0).getRet());
    Assert.assertEquals("test", trueRes);
  }

  @Test(enabled = true, description = "test immutable variable inited when declared", groups = {"contract", "daily"})
  public void test05ImmutableInit() {

    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(mapKeyContract,
            "getOwner()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Protocol.Transaction transaction = transactionExtention.getTransaction();
  String trueRes = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("truerRes: " + trueRes + "   message:" + transaction.getRet(0).getRet());
    Assert.assertEquals(ByteArray.toHexString(PublicMethod
            .getFinalAddress(contractExcKey)).substring(2), trueRes.substring(24));

  }

  @Test(enabled = true, description = "test immutable variable inited in construct", groups = {"contract", "daily"})
  public void test06ImmutableInit() {

    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(mapKeyContract,
            "getImmutableVal()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Protocol.Transaction transaction = transactionExtention.getTransaction();
  int trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("truerRes: " + trueRes + "   message:" + transaction.getRet(0).getRet());
    Assert.assertEquals(5, trueRes);
  }

  @Test(enabled = true, description = "get interface id,"
      + "interface id is result of  all function selector's XOR ", groups = {"contract", "daily"})
  public void test07GetInterfaceId() {

    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(mapKeyContract,
            "getInterfaceId()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Protocol.Transaction transaction = transactionExtention.getTransaction();
  byte[] result = transactionExtention.getConstantResult(0).toByteArray();
  String trueRes = ByteArray.toHexString(ByteArray.subArray(result, 0, 4));
  String trueRes1 = ByteArray.toHexString(ByteArray.subArray(result, 32, 36));

    logger.info("truerRes: " + trueRes + " truerRes1: " + trueRes1
        + "   message:" + transaction.getRet(0).getRet());
    Assert.assertEquals("a9ab72bd", trueRes);
    Assert.assertEquals(trueRes, trueRes1);

  }

  @Test(enabled = true, description = "abstract contract can have vitrual modifier with empty body", groups = {"contract", "daily"})
  public void test08VirtualModifier() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(mapKeyContract,
            "requireOwner()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Protocol.Transaction transaction = transactionExtention.getTransaction();
  byte[] result = transactionExtention.getConstantResult(0).toByteArray();
  int trueRes = ByteArray.toInt(result);
    logger.info("truerRes: " + trueRes + "   message:" + transaction.getRet(0).getRet());
    Assert.assertEquals(6, trueRes);

  }

  @Test(enabled = true, description = "uint256 max and mine", groups = {"contract", "daily"})
  public void test09Uint256MaxMine() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(mapKeyContract,
            "getUint256MinAndMax()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Protocol.Transaction transaction = transactionExtention.getTransaction();
  byte[] result = transactionExtention.getConstantResult(0).toByteArray();
  String trueRes = ByteArray.toHexString(ByteArray.subArray(result, 0, 32));
  String trueRes1 = ByteArray.toHexString(ByteArray.subArray(result, 32, 64));
    logger.info("truerRes: " + trueRes + "truerRes1: " + trueRes1
        + "   message:" + transaction.getRet(0).getRet());
    Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000000",
        trueRes);
    Assert.assertEquals("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
        trueRes1);

  }

  @Test(enabled = true, description = "solidity 0.6.12 test Reference "
      + "variable can be marked by calldata", groups = {"contract", "daily"})
  public void test10CalldataModifier() {
    String hexAdd = ByteArray.toHexString(PublicMethod.getFinalAddress(contractExcKey));
  String args = "\"0x" + hexAdd + "\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(mapKeyContract,
            "calldataModifier(bytes)", args, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Protocol.Transaction transaction = transactionExtention.getTransaction();
  byte[] result = transactionExtention.getConstantResult(0).toByteArray();
  String trueRes = ByteArray.toHexString(result);
    logger.info("truerRes: " + trueRes + "   message:" + transaction.getRet(0).getRet());
    Assert.assertTrue(trueRes.contains(hexAdd));
  }


  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(contractExcAddress, contractExcKey,
        testNetAccountAddress, blockingStubFull);  }


}

