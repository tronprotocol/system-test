package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

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

import java.util.HashMap;

@Slf4j
public class NewFeatureForSolc0822 extends TronBaseTest {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethod.getFinalAddress(testNetAccountKey);
  byte[] contractD = null;
  byte[] contractM = null;
  byte[] contractA = null;
  byte[] contractB = null;
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
  String filePath = "src/test/resources/soliditycode/NewFeature0822_1.sol";
  String contractName = "D";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    contractD = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);

    contractName = "M";
    retMap = PublicMethod.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    contractM = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
            0L, 100, null, contractExcKey,
            contractExcAddress, blockingStubFull);

    contractName = "A";
    retMap = PublicMethod.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    contractA = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
            0L, 100, null, contractExcKey,
            contractExcAddress, blockingStubFull);

    contractName = "B";
    retMap = PublicMethod.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    contractB = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
            0L, 100, null, contractExcKey,
            contractExcAddress, blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethod.getContract(contractD,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
    smartContract = PublicMethod.getContract(contractM, blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
    smartContract = PublicMethod.getContract(contractA, blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
    smartContract = PublicMethod.getContract(contractB, blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
  }


  @Test(enabled = true, description = "event at file level", groups = {"contract", "daily"})
  public void test001FileLevelEvent() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
            .triggerConstantContractForExtention(contractD, "f()", "#", false,
                    0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
            transactionExtention.getTransaction().getRet(0).getRet().toString());
    System.out.println(transactionExtention);
    Assert.assertEquals(ByteArray.toHexString("abc".getBytes()),
            ByteArray.toHexString(transactionExtention.getLogs(0).getData().substring(64).toByteArray()).substring(0,6));
    Assert.assertEquals(8,
            ByteArray.toInt(transactionExtention.getLogs(1).getData().substring(0, 32).toByteArray()));
    Assert.assertEquals(2, transactionExtention.getLogs(1).getTopicsCount());
    Assert.assertEquals(6,
            ByteArray.toInt(transactionExtention.getLogs(2).getData().toByteArray()));
  }

  @Test(enabled = true, description = "Fix internal error when requesting userdoc or devdoc for a contract " +
          "that emits an event defined in a foreign contract or interface", groups = {"contract", "daily"})
  public void test002ImmutableInitInCondition() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
            .triggerConstantContractForExtention(contractM, "emitEvent(uint256)", "8", false,
                    0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
            transactionExtention.getTransaction().getRet(0).getRet().toString());
    System.out.println(transactionExtention);
    Assert.assertEquals(2, transactionExtention.getLogs(0).getTopicsCount());
    Assert.assertEquals(ByteArray.toHexString(contractExcAddress).substring(2),
            ByteArray.toHexString(transactionExtention.getLogs(0).getTopics(1).substring(12).toByteArray()));
    Assert.assertEquals(8,
            ByteArray.toInt(transactionExtention.getLogs(0).getData().toByteArray()));
  }

  @Test(enabled = true, description = "Remove redundant overflow checks of certain ``for`` loops when the counter variable cannot overflow.", groups = {"contract", "daily"})
  public void test003UncheckForLoopsOverflow() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
            .triggerConstantContractForExtention(contractA, "inc()", "#", false,
                    0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
            transactionExtention.getTransaction().getRet(0).getRet().toString());
    long incEnergy = transactionExtention.getEnergyUsed();
    transactionExtention = PublicMethod
            .triggerConstantContractForExtention(contractB, "inc()", "#", false,
                    0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
            transactionExtention.getTransaction().getRet(0).getRet().toString());
    long incUncheckEnergy = transactionExtention.getEnergyUsed();
    logger.info("incEnergy: " + incEnergy + "  incUncheckEnergy: " + incUncheckEnergy);
    Assert.assertEquals(incEnergy, incUncheckEnergy);
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(contractExcAddress, contractExcKey,
        testNetAccountAddress, blockingStubFull);  }


}

