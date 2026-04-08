package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

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

import java.util.HashMap;
import java.util.Optional;

@Slf4j
public class NewFeatureForSolc0821 extends TronBaseTest {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethod.getFinalAddress(testNetAccountKey);
  byte[] contractH = null;
  byte[] contractC = null;
  byte[] contractK = null;
  byte[] lib = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(contractExcKey);    Assert.assertTrue(PublicMethod
        .sendcoin(contractExcAddress, 50100_000_000L,
            testNetAccountAddress, testNetAccountKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "src/test/resources/soliditycode/NewFeature0821_1.sol";
  String contractName = "H";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    contractH = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);

    contractName = "C";
    retMap = PublicMethod.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    contractC = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
            0L, 100, null, contractExcKey,
            contractExcAddress, blockingStubFull);


    PublicMethod.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethod.getContract(contractH,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
    smartContract = PublicMethod.getContract(contractC, blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
  }

  @Test(enabled = true, description = "expressions triggering selector can be executed correctly", groups = {"contract", "daily"})
  public void test001ExpressionsTriggerSelector() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractH,"f()", "", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
  int result = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(42, result);
  }

  @Test(enabled = true, description = "Allow qualified access to events from other contracts.", groups = {"contract", "daily"})
  public void test002AccessForeignEvent() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractC,"f()", "", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(4, transactionExtention.getLogsList().size());
    Assert.assertEquals(transactionExtention.getLogs(0).toString(),
            transactionExtention.getLogs(1).toString());
    Assert.assertEquals(transactionExtention.getLogs(0).toString(),
            transactionExtention.getLogs(2).toString());
    Assert.assertEquals(transactionExtention.getLogs(0).toString(),
            transactionExtention.getLogs(3).toString());
    Assert.assertEquals("39cf8f4d81ddb7c9a936f0fa76190b3d8084a8c21c154d4b02236a58b3051ad9",
            ByteArray.toHexString(transactionExtention.getLogs(1).getData().substring(0, 32).toByteArray()));
    Assert.assertEquals(1,
            ByteArray.toInt(transactionExtention.getLogs(1).getData().substring(32, 64).toByteArray()));
    Assert.assertEquals(123,
            ByteArray.toInt(transactionExtention.getLogs(1).getData().substring(64).toByteArray()));
  }

  @Test(enabled = true, description = "immutable variable init in condition", groups = {"contract", "daily"})
  public void test003ImmutableInitInCondition() {
    String filePath = "src/test/resources/soliditycode/NewFeature0821_1.sol";;
  String contractName = "K";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  String constructorStr = "constructor(bool,uint256)";
  String argsStr = "true,2";
  String deplTxid = PublicMethod.deployContractWithConstantParame(contractName, abi, code,
            constructorStr, argsStr, "", maxFeeLimit, 0L, 100,1000L,
            "0", 0L, null, contractExcKey,
            contractExcAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info = PublicMethod
            .getTransactionInfoById(deplTxid, blockingStubFull);
    Assert.assertTrue(info.get().getResultValue() == 0);
    contractK = info.get().getContractAddress().toByteArray();

    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractK,"x()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
  int result = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(42, result);

    transactionExtention = PublicMethod
            .triggerConstantContractForExtention(contractK,"y()", "#", false,
                    0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    result = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
            transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(0, result);

    transactionExtention = PublicMethod
            .triggerConstantContractForExtention(contractK,"z()", "#", false,
                    0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    result = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
            transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(42, result);
  }

  @Test(enabled = true, description = "immutable variable init in try catch", groups = {"contract", "daily"})
  public void test004ImmutableInitInTryCatch() {
    String filePath = "src/test/resources/soliditycode/NewFeature0821_2.sol";;
  String contractName = "A";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  byte[] contractA = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
            0L, 100, null, contractExcKey,
            contractExcAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethod.getContract(contractA,
            blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
    smartContract = PublicMethod.getContract(contractA, blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
            .triggerConstantContractForExtention(contractA,"variable()", "#", false,
                    0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
  int result = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
            transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(5, result);
  }


  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(contractExcAddress, contractExcKey,
        testNetAccountAddress, blockingStubFull);  }


}

