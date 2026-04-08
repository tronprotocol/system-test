package stest.tron.wallet.onlinestress;

import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.protos.contract.SmartContractOuterClass;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;


@Slf4j
public class NewFeatureForSolc0811 extends TronBaseTest {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethod.getFinalAddress(testNetAccountKey);
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
  String filePath = "src/test/resources/soliditycode/NewFeature0811-test.sol";
  String contractName = "C";
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

  @Test(enabled = true, description = "Get enum.min or enum.max", groups = {"stress"})
  public void test001GetEnumMinOrMax() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(mapKeyContract,
            "getEnumMin()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
  int result = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(0, result);


    transactionExtention = PublicMethod
        .triggerConstantContractForExtention(mapKeyContract,
            "getEnumMax()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    result = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(3, result);

  }


  @Test(enabled = true,description = "User defined type of data", groups = {"stress"})
  public void test002SupportUserDefinedTypeOfData() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(mapKeyContract,
            "getUserDefinedValue()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
  int result = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(45, result);
  }


  @Test(enabled = true,description = "Get assembly address", groups = {"stress"})
  public void test003GetAssemblyAddress() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(mapKeyContract,
            "testGetAddress()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertTrue(ByteArray.toHexString(transactionExtention.getConstantResult(0)
        .toByteArray()).contains(ByteArray.toHexString(mapKeyContract).substring(2)));
  }


  @Test(enabled = true,description = "Get assembly selector", groups = {"stress"})
  public void test004GetAssemblySelector() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(mapKeyContract,
            "testGetSelector()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    long result = ByteArray.toLong(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(3781905051L, result);
  }


  @Test(enabled = true,description = "Get ABI-Encode calldata", groups = {"stress"})
  public void test005GetAbiEncodedCalldata() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(mapKeyContract,
            "fSignatureFromLiteralCall()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());

    Assert.assertEquals(""
            + "0000000000000000000000000000000000000000000000000000000000000020"
            + "0000000000000000000000000000000000000000000000000000000000000084"
            + "33d8581000000000000000000000000000000000000000000000000000000000"
            + "0000000100000000000000000000000000000000000000000000000000000000"
            + "0000004000000000000000000000000000000000000000000000000000000000"
            + "0000000331323300000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000",
        ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));
  }


  @Test(enabled = true,description = "Support constructor read immutable value", groups = {"stress"})
  public void test006SupportConstructorReadImmutableValue() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(mapKeyContract,
            "readX()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
  int result = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(33, result);


    transactionExtention = PublicMethod
        .triggerConstantContractForExtention(mapKeyContract,
            "readI()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    result = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(33, result);


  }


  @Test(enabled = true,description = "Fix immutable symbol bug", groups = {"stress"})
  public void test007FixImmutableSymbol() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(mapKeyContract,
            "fixBugTest()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());

    Assert.assertEquals("fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffc",
        ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));
  }


  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(contractExcAddress, contractExcKey,
        testNetAccountAddress, blockingStubFull);  }


}

