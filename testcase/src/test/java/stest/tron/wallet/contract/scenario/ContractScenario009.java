package stest.tron.wallet.contract.scenario;

import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class ContractScenario009 extends TronBaseTest {  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contract009Address = ecKey1.getAddress();
  String contract009Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private String compilerVersion = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.solidityCompilerVersion");

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(contract009Key);  }

  @Test(enabled = true, groups = {"contract", "smoke"})
  public void deployContainLibraryContract() {
    Assert.assertTrue(PublicMethod.sendcoin(contract009Address, 20000000L, foundationAddress,
        foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.freezeBalanceGetEnergy(contract009Address, 1000000L,
        3, 1, contract009Key, blockingStubFull));
    AccountResourceMessage accountResource = PublicMethod.getAccountResource(contract009Address,
        blockingStubFull);
  Long energyLimit = accountResource.getEnergyLimit();
  Long energyUsage = accountResource.getEnergyUsed();

    logger.info("before energy limit is " + Long.toString(energyLimit));
    logger.info("before energy usage is " + Long.toString(energyUsage));
  String filePath = "./src/test/resources/soliditycode/contractScenario009.sol";
  String contractName = "Set";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  byte[] libraryContractAddress;
    libraryContractAddress = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit,
            0L, 100, null, contract009Key, contract009Address, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    contractName = "C";
    retMap = PublicMethod.getBycodeAbiForLibrary(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
  String library = retMap.get("library").toString();
  //String libraryAddress =
    //    "browser/TvmTest_p1_Grammar_002.sol:Set:" + Base58.encode58Check(libraryContractAddress);
  String libraryAddress;
    libraryAddress = library
        + Base58.encode58Check(libraryContractAddress);
  byte[] contractAddress = PublicMethod
        .deployContractForLibrary(contractName, abi, code, "", maxFeeLimit, 0L, 100, libraryAddress,
            contract009Key, contract009Address, compilerVersion, blockingStubFull);
    SmartContract smartContract = PublicMethod.getContract(contractAddress, blockingStubFull);

    Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
    logger.info(ByteArray.toHexString(smartContract.getContractAddress().toByteArray()));
    accountResource = PublicMethod.getAccountResource(contract009Address, blockingStubFull);
    energyLimit = accountResource.getEnergyLimit();
    energyUsage = accountResource.getEnergyUsed();
    Assert.assertTrue(energyLimit > 0);
    Assert.assertTrue(energyUsage > 0);

    logger.info("after energy limit is " + Long.toString(energyLimit));
    logger.info("after energy usage is " + Long.toString(energyUsage));
  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {  }
}


