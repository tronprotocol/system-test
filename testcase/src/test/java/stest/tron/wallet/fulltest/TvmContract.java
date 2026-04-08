package stest.tron.wallet.fulltest;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.protos.Protocol.Account;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class TvmContract extends TronBaseTest {

  //testng001、testng002、testng003、testng004  only for test, do not worry !
  private final String testKey002 =
      "FC8BF0238748587B9617EB6D15D47A66C0E07C1A1959033CF249C6532DC29FE6";
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contract008Address = ecKey1.getAddress();
  String contract008Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  /**
   * constructor.
   */

  @BeforeClass(enabled = false)
  public void beforeClass() {
    PublicMethod.printAddress(contract008Key);    Assert.assertTrue(PublicMethod.sendcoin(contract008Address, 500000000L, foundationAddress,
        testKey002, blockingStubFull));
    logger.info(Long.toString(PublicMethod.queryAccount(contract008Key, blockingStubFull)
        .getBalance()));
    Assert.assertTrue(PublicMethod.freezeBalanceGetEnergy(contract008Address, 1000000L,
        3, 1, contract008Key, blockingStubFull));
    Assert.assertTrue(PublicMethod.buyStorage(50000000L, contract008Address, contract008Key,
        blockingStubFull));
    Assert.assertTrue(PublicMethod.freezeBalance(contract008Address, 5000000L,
        3, contract008Key, blockingStubFull));

  }

  @Test(enabled = false, groups = {"full"})
  public void deployErc721CryptoKitties() {
    AccountResourceMessage accountResource = PublicMethod.getAccountResource(contract008Address,
        blockingStubFull);
  Long energyLimit = accountResource.getEnergyLimit();
  Long storageLimit = accountResource.getStorageLimit();
  Long energyUsage = accountResource.getEnergyUsed();
  Long storageUsage = accountResource.getStorageUsed();

    logger.info("before energy limit is " + Long.toString(energyLimit));
    logger.info("before energy usage is " + Long.toString(energyUsage));
    logger.info("before storage limit is " + Long.toString(storageLimit));
    logger.info("before storage usaged is " + Long.toString(storageUsage));
  Long maxFeeLimit = 50000000L;
  String contractName = "ERC721";
  String code = Configuration.getByPath("testng.conf")
        .getString("code.code_TvmContract_deployErc721CryptoKitties");
  String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_TvmContract_deployErc721CryptoKitties");
  Long m = 0L;
  Long freeNet;
    accountResource = PublicMethod.getAccountResource(contract008Address, blockingStubFull);
  Long net = accountResource.getFreeNetUsed();
    Account account = PublicMethod.queryAccount(contract008Key, blockingStubFull);
  Long netUsed = account.getNetUsage();
    logger.info("before net used is " + Long.toString(netUsed));
    logger.info("before balance is " + account.getBalance());

    for (Integer i = 0; i < 1; i++) {
      byte[] contractAddress = PublicMethod.deployContract("1", abi, code, "",
          30000000L, 0L, 1, null, contract008Key, contract008Address, blockingStubFull);
      accountResource = PublicMethod.getAccountResource(contract008Address, blockingStubFull);
      freeNet = accountResource.getFreeNetUsed();
      energyUsage = accountResource.getEnergyUsed();
      logger.info(
          "Time " + Integer.toString(i) + ": energy usage is " + Long.toString(energyUsage - m));
      logger.info("Time " + Integer.toString(i) + ": free net used is " + Long
          .toString(freeNet - net));
      account = PublicMethod.queryAccount(contract008Key, blockingStubFull);
      logger.info("after balance is " + account.getBalance());
      netUsed = account.getNetUsage();
      logger.info("after net used is " + Long.toString(netUsed));
      net = freeNet;
      m = energyUsage;
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

    }
    //SmartContract smartContract = PublicMethod.getContract(contractAddress,blockingStubFull);
  //Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
  //Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
  //Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
  //logger.info(smartContract.getName());
  //logger.info(smartContract.getAbi().toString());
    accountResource = PublicMethod.getAccountResource(contract008Address, blockingStubFull);
    energyLimit = accountResource.getEnergyLimit();
    storageLimit = accountResource.getStorageLimit();
    energyUsage = accountResource.getEnergyUsed();
    storageUsage = accountResource.getStorageUsed();
  //Assert.assertTrue(storageUsage > 0);
  //Assert.assertTrue(storageLimit > 0);
  //Assert.assertTrue(energyLimit > 0);
  //Assert.assertTrue(energyUsage > 0);

    logger.info("after energy limit is " + Long.toString(energyLimit));
    logger.info("after energy usage is " + Long.toString(energyUsage));
    logger.info("after storage limit is " + Long.toString(storageLimit));
    logger.info("after storage usaged is " + Long.toString(storageUsage));
  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {  }
}


