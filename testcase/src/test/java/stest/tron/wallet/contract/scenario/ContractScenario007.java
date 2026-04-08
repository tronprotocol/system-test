package stest.tron.wallet.contract.scenario;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.protos.Protocol.Account;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class ContractScenario007 extends TronBaseTest {  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contract007Address = ecKey1.getAddress();
  String contract007Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(contract007Key);  }

  @Test(enabled = true, groups = {"contract", "smoke"})
  public void deployErc721CardMigration() {
    ecKey1 = new ECKey(Utils.getRandom());
    contract007Address = ecKey1.getAddress();
    contract007Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    Assert.assertTrue(PublicMethod.sendcoin(contract007Address, 20000000000L, foundationAddress,
        foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.freezeBalanceGetEnergy(contract007Address, 100000000L,
        3, 1, contract007Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    AccountResourceMessage accountResource = PublicMethod.getAccountResource(contract007Address,
        blockingStubFull);
  Long energyLimit = accountResource.getEnergyLimit();
  Long energyUsage = accountResource.getEnergyUsed();
    Account account = PublicMethod.queryAccount(contract007Key, blockingStubFull);
    logger.info("before balance is " + Long.toString(account.getBalance()));
    logger.info("before energy limit is " + Long.toString(energyLimit));
    logger.info("before energy usage is " + Long.toString(energyUsage));
  String contractName = "ERC721Token";
  String code = Configuration.getByPath("testng.conf")
        .getString("code.code_ContractScenario007_deployErc721CardMigration");
  String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_ContractScenario007_deployErc721CardMigration");
  byte[] contractAddress = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, contract007Key, contract007Address, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethod.getContract(contractAddress, blockingStubFull);
    Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
    accountResource = PublicMethod.getAccountResource(contract007Address, blockingStubFull);
    energyLimit = accountResource.getEnergyLimit();
    energyUsage = accountResource.getEnergyUsed();
    account = PublicMethod.queryAccount(contract007Key, blockingStubFull);
    logger.info("after balance is " + Long.toString(account.getBalance()));
    logger.info("after energy limit is " + Long.toString(energyLimit));
    logger.info("after energy usage is " + Long.toString(energyUsage));
    Assert.assertTrue(energyLimit > 0);
    Assert.assertTrue(energyUsage > 0);

  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {  }
}


