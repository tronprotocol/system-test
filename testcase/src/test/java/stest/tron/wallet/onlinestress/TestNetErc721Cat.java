package stest.tron.wallet.onlinestress;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class TestNetErc721Cat extends TronBaseTest {

  //testng001、testng002、testng003、testng004
  //testng001、testng002、testng003、testng004
  private final String testKey002 =
      //"7306c6044ad7c03709980aa188b8555288b7e0608f5edbf76ff2381c5a7a15a8";
  //"3a54ba30e3ee41b602eca8fb3a3ca1f99f49a3d3ab5d8d646a2ccdd3ffd9c21d";
  //foundationAddress
      "FC8BF0238748587B9617EB6D15D47A66C0E07C1A1959033CF249C6532DC29FE6";
  String kittyCoreAddressAndCut = "";
  byte[] kittyCoreContractAddress = null;
  byte[] saleClockAuctionContractAddress = null;
  byte[] siringClockAuctionContractAddress = null;
  byte[] geneScienceInterfaceContractAddress = null;
  Integer consumeUserResourcePercent = 20;
  String txid = "";

  Optional<TransactionInfo> infoById = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] deployAddress = ecKey1.getAddress();
  String deployKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] triggerAddress = ecKey2.getAddress();
  String triggerKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  /**
   * constructor.
   */

  @BeforeClass(enabled = false)
  public void beforeClass() {
    PublicMethod.printAddress(deployKey);
    PublicMethod.printAddress(triggerKey);    Assert.assertTrue(PublicMethod.sendcoin(deployAddress, 50000000000L, foundationAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethod.sendcoin(triggerAddress, 50000000000L, foundationAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethod.freezeBalanceGetEnergy(deployAddress, 100000000L,
        3, 1, deployKey, blockingStubFull));
    /*    Assert.assertTrue(PublicMethod.freezeBalanceGetCpu(triggerAddress,100000000L,
        3,1,triggerKey,blockingStubFull));*/
    /*Assert.assertTrue(PublicMethod.buyStorage(500000000L,deployAddress,deployKey,
        blockingStubFull));
    Assert.assertTrue(PublicMethod.buyStorage(500000000L,triggerAddress,triggerKey,
        blockingStubFull));
    Assert.assertTrue(PublicMethod.freezeBalance(deployAddress,100000000L,3,
        deployKey,blockingStubFull));
    Assert.assertTrue(PublicMethod.freezeBalance(triggerAddress,100000000L,3,
        triggerKey,blockingStubFull));*/

  }

  @Test(enabled = false, groups = {"stress"})
  public void deployErc721KittyCore() {
    AccountResourceMessage accountResource = PublicMethod.getAccountResource(deployAddress,
        blockingStubFull);
  Long cpuLimit = accountResource.getEnergyLimit();
  //Long storageLimit = accountResource.getStorageLimit();
  Long cpuUsage = accountResource.getEnergyUsed();
  //Long storageUsage = accountResource.getStorageUsed();
    Account account = PublicMethod.queryAccount(deployAddress, blockingStubFull);
    logger.info("before balance is " + Long.toString(account.getBalance()));
    logger.info("before cpu limit is " + Long.toString(cpuLimit));
    logger.info("before cpu usage is " + Long.toString(cpuUsage));
  //logger.info("before storage limit is " + Long.toString(storageLimit));
  //logger.info("before storage usaged is " + Long.toString(storageUsage));
  Long maxFeeLimit = 3900000000L;
  String contractName = "KittyCore";
  String code = Configuration.getByPath("testng.conf")
        .getString("code.code_TestNetErc721Cat_deployErc721KittyCore");
  String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_TestNetErc721Cat_deployErc721KittyCore");
    logger.info("Kitty Core");
    kittyCoreContractAddress = PublicMethod.deployContract(contractName, abi, code, "",
        maxFeeLimit, 0L, consumeUserResourcePercent, null, deployKey,
        deployAddress, blockingStubFull);
    SmartContract smartContract = PublicMethod.getContract(kittyCoreContractAddress,
        blockingStubFull);

    Assert.assertTrue(smartContract.getAbi() != null);
    accountResource = PublicMethod.getAccountResource(deployAddress, blockingStubFull);
    cpuLimit = accountResource.getEnergyLimit();
  //storageLimit = accountResource.getStorageLimit();
    cpuUsage = accountResource.getEnergyUsed();
  //storageUsage = accountResource.getStorageUsed();
    account = PublicMethod.queryAccount(deployKey, blockingStubFull);
    logger.info("after balance is " + Long.toString(account.getBalance()));
    logger.info("after cpu limit is " + Long.toString(cpuLimit));
    logger.info("after cpu usage is " + Long.toString(cpuUsage));
  //logger.info("after storage limit is " + Long.toString(storageLimit));
  //logger.info("after storage usaged is " + Long.toString(storageUsage));
    logger.info(ByteArray.toHexString(kittyCoreContractAddress));
    logger.info(ByteArray.toHexString(kittyCoreContractAddress).substring(2));

    kittyCoreAddressAndCut = "000000000000000000000000" + ByteArray
        .toHexString(kittyCoreContractAddress).substring(2);
    kittyCoreAddressAndCut = kittyCoreAddressAndCut + "0000000000000000000000000000000000000000000"
        + "000000000000000000100";
  }

  @Test(enabled = false, groups = {"stress"})
  public void deploySaleClockAuction() {
    AccountResourceMessage accountResource = PublicMethod.getAccountResource(deployAddress,
        blockingStubFull);
  Long cpuLimit = accountResource.getEnergyLimit();
  //Long storageLimit = accountResource.getStorageLimit();
  Long cpuUsage = accountResource.getEnergyUsed();
  //Long storageUsage = accountResource.getStorageUsed();
    Account account = PublicMethod.queryAccount(deployKey, blockingStubFull);
    logger.info("before balance is " + Long.toString(account.getBalance()));
    logger.info("before cpu limit is " + Long.toString(cpuLimit));
    logger.info("before cpu usage is " + Long.toString(cpuUsage));
  //logger.info("before storage limit is " + Long.toString(storageLimit));
  //logger.info("before storage usaged is " + Long.toString(storageUsage));
  Long maxFeeLimit = 3900000000L;
  String contractName = "SaleClockAuction";
    logger.info("Sale Clock Auction");
  String code = Configuration.getByPath("testng.conf")
        .getString("code.code_TestNetErc721Cat_deploySaleClockAuction");
  String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_TestNetErc721Cat_deploySaleClockAuction");
    saleClockAuctionContractAddress = PublicMethod.deployContract(contractName, abi, code,
        "", maxFeeLimit, 0L, consumeUserResourcePercent, null, deployKey,
        deployAddress, blockingStubFull);
    SmartContract smartContract = PublicMethod.getContract(saleClockAuctionContractAddress,
        blockingStubFull);
    Assert.assertTrue(smartContract.getAbi() != null);
    accountResource = PublicMethod.getAccountResource(deployAddress, blockingStubFull);
    cpuLimit = accountResource.getEnergyLimit();
  //storageLimit = accountResource.getStorageLimit();
    cpuUsage = accountResource.getEnergyUsed();
  //storageUsage = accountResource.getStorageUsed();
    account = PublicMethod.queryAccount(deployKey, blockingStubFull);
    logger.info("after balance is " + Long.toString(account.getBalance()));
    logger.info("after cpu limit is " + Long.toString(cpuLimit));
    logger.info("after cpu usage is " + Long.toString(cpuUsage));
  //logger.info("after storage limit is " + Long.toString(storageLimit));
  //logger.info("after storage usaged is " + Long.toString(storageUsage));
  }

  @Test(enabled = false, groups = {"stress"})
  public void deploySiringClockAuction() {
    AccountResourceMessage accountResource = PublicMethod.getAccountResource(deployAddress,
        blockingStubFull);
  Long cpuLimit = accountResource.getEnergyLimit();
  //Long storageLimit = accountResource.getStorageLimit();
  Long cpuUsage = accountResource.getEnergyUsed();
  //Long storageUsage = accountResource.getStorageUsed();
    Account account = PublicMethod.queryAccount(deployKey, blockingStubFull);
    logger.info("before balance is " + Long.toString(account.getBalance()));
    logger.info("before cpu limit is " + Long.toString(cpuLimit));
    logger.info("before cpu usage is " + Long.toString(cpuUsage));
  //logger.info("before storage limit is " + Long.toString(storageLimit));
  //logger.info("before storage usaged is " + Long.toString(storageUsage));
  Long maxFeeLimit = 3900000000L;
  String contractName = "SiringClockAuction";
  String code = Configuration.getByPath("testng.conf")
        .getString("code.code_TestNetErc721Cat_deploySiringClockAuction");
  String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_TestNetErc721Cat_deploySiringClockAuction");
    logger.info("Siring Clock Auction");
    siringClockAuctionContractAddress = PublicMethod.deployContract(contractName, abi, code,
        "", maxFeeLimit, 0L, consumeUserResourcePercent, null, deployKey,
        deployAddress, blockingStubFull);
    SmartContract smartContract = PublicMethod.getContract(siringClockAuctionContractAddress,
        blockingStubFull);
    Assert.assertTrue(smartContract.getAbi() != null);
    accountResource = PublicMethod.getAccountResource(deployAddress, blockingStubFull);
    cpuLimit = accountResource.getEnergyLimit();
  //storageLimit = accountResource.getStorageLimit();
    cpuUsage = accountResource.getEnergyUsed();
  //storageUsage = accountResource.getStorageUsed();
    account = PublicMethod.queryAccount(deployKey, blockingStubFull);
    logger.info("after balance is " + Long.toString(account.getBalance()));
    logger.info("after cpu limit is " + Long.toString(cpuLimit));
    logger.info("after cpu usage is " + Long.toString(cpuUsage));
  //logger.info("after storage limit is " + Long.toString(storageLimit));
  //logger.info("after storage usaged is " + Long.toString(storageUsage));
  }

  @Test(enabled = false, groups = {"stress"})
  public void deployGeneScienceInterface() {
    AccountResourceMessage accountResource = PublicMethod.getAccountResource(deployAddress,
        blockingStubFull);
  Long cpuLimit = accountResource.getEnergyLimit();
  //Long storageLimit = accountResource.getStorageLimit();
  Long cpuUsage = accountResource.getEnergyUsed();
  //Long storageUsage = accountResource.getStorageUsed();
    Account account = PublicMethod.queryAccount(deployKey, blockingStubFull);
    logger.info("before balance is " + Long.toString(account.getBalance()));
    logger.info("before cpu limit is " + Long.toString(cpuLimit));
    logger.info("before cpu usage is " + Long.toString(cpuUsage));
  //logger.info("before storage limit is " + Long.toString(storageLimit));
  //logger.info("before storage usaged is " + Long.toString(storageUsage));
  Long maxFeeLimit = 3900000000L;
  String contractName = "GeneScienceInterface";
  String code = Configuration.getByPath("testng.conf")
        .getString("code.code_TestNetErc721Cat_deployGeneScienceInterface");
  String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_TestNetErc721Cat_deployGeneScienceInterface");
    logger.info("gene Science Interface");
    geneScienceInterfaceContractAddress = PublicMethod.deployContract(contractName, abi, code,
        "", maxFeeLimit,
        0L, consumeUserResourcePercent, null, deployKey, deployAddress, blockingStubFull);
    SmartContract smartContract = PublicMethod.getContract(geneScienceInterfaceContractAddress,
        blockingStubFull);
    Assert.assertTrue(smartContract.getAbi() != null);
    accountResource = PublicMethod.getAccountResource(deployAddress, blockingStubFull);
    cpuLimit = accountResource.getEnergyLimit();
  //storageLimit = accountResource.getStorageLimit();
    cpuUsage = accountResource.getEnergyUsed();
  //storageUsage = accountResource.getStorageUsed();
    account = PublicMethod.queryAccount(deployKey, blockingStubFull);
    logger.info("after balance is " + Long.toString(account.getBalance()));
    logger.info("after cpu limit is " + Long.toString(cpuLimit));
    logger.info("after cpu usage is " + Long.toString(cpuUsage));
  //logger.info("after storage limit is " + Long.toString(storageLimit));
  //logger.info("after storage usaged is " + Long.toString(storageUsage));
  }

  @Test(enabled = false, groups = {"stress"})
  public void triggerToSetThreeContractAddressToKittyCore() {
    //Set SaleAuctionAddress to kitty core.
    String saleContractString = "\"" + Base58.encode58Check(saleClockAuctionContractAddress) + "\"";
    txid = PublicMethod.triggerContract(kittyCoreContractAddress, "setSaleAuctionAddress(address)",
        saleContractString, false, 0, 10000000L, deployAddress, deployKey, blockingStubFull);
    logger.info(txid);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
  //Assert.assertTrue(infoById.get().getReceipt().getStorageDelta() > 50);
  //Set SiringAuctionAddress to kitty core.
    String siringContractString = "\"" + Base58.encode58Check(siringClockAuctionContractAddress)
        + "\"";
    txid = PublicMethod
        .triggerContract(kittyCoreContractAddress, "setSiringAuctionAddress(address)",
            siringContractString, false, 0, 10000000L,
            deployAddress, deployKey, blockingStubFull);
    logger.info(txid);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
  //Assert.assertTrue(infoById.get().getReceipt().getStorageDelta() > 50);
  //Set gen contract to kitty core
    String genContractString = "\"" + Base58.encode58Check(geneScienceInterfaceContractAddress)
        + "\"";
    txid = PublicMethod.triggerContract(kittyCoreContractAddress,
        "setGeneScienceAddress(address)", genContractString,
        false, 0, 10000000L, deployAddress, deployKey, blockingStubFull);
    logger.info(txid);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
  //Assert.assertTrue(infoById.get().getReceipt().getStorageDelta() > 50);
  //Start the game.
    txid = PublicMethod.triggerContract(kittyCoreContractAddress, "unpause()", "", false, 0,
        10000000L, deployAddress, deployKey, blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    logger.info("start the game " + txid);
  //Create one gen0 cat.
    txid = PublicMethod.triggerContract(kittyCoreContractAddress,
        "createGen0Auction(uint256)", "-1000000000000000", false,
        0, 100000000L, deployAddress, deployKey, blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    txid = PublicMethod.triggerContract(kittyCoreContractAddress,
        "gen0CreatedCount()", "#", false,
        0, 100000000L, deployAddress, deployKey, blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    /*      txid = PublicMethod.triggerContract(kittyCoreContractAddress,
          "name()","#",false,0,10000000,triggerAddress,
          triggerKey,blockingStubFull);
      logger.info("getname " + txid);*/

    txid = PublicMethod.triggerContract(kittyCoreContractAddress,
        "getKitty(uint256)", "1", false, 0, 10000000, triggerAddress,
        triggerKey, blockingStubFull);
    logger.info("getKitty " + txid);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
  String newCxoAddress = "\"" + Base58.encode58Check(triggerAddress)
        + "\"";

    txid = PublicMethod.triggerContract(kittyCoreContractAddress,
        "setCOO(address)", newCxoAddress, false, 0, 10000000, deployAddress,
        deployKey, blockingStubFull);
    logger.info("COO " + txid);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    txid = PublicMethod.triggerContract(kittyCoreContractAddress,
        "setCFO(address)", newCxoAddress, false, 0, 10000000, deployAddress,
        deployKey, blockingStubFull);
    logger.info("CFO " + txid);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    txid = PublicMethod.triggerContract(kittyCoreContractAddress,
        "setCEO(address)", newCxoAddress, false, 0, 1000000, deployAddress,
        deployKey, blockingStubFull);
    logger.info("CEO " + txid);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
  }

  @Test(enabled = false, threadPoolSize = 1, invocationCount = 1, groups = {"stress"})
  public void unCreateKitty() {
    Integer times = 0;
    logger.info("In create kitty, kitty core address is " + ByteArray
        .toHexString(kittyCoreContractAddress));
    while (times++ < 20) {
      txid = PublicMethod.triggerContract(kittyCoreContractAddress,
          "createGen0Auction(uint256)", "0", false,
          0, 100000000L, triggerAddress, triggerKey, blockingStubFull);
      logger.info("createGen0 " + txid);
      infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
  //Assert.assertTrue(infoById.get().getResultValue() == 0);
      /*      String promoKitty = "\"" + times.toString() + "\",\""
          +  Base58.encode58Check(kittyCoreContractAddress) + "\"";
      logger.info(promoKitty);
      txid = PublicMethod.triggerContract(kittyCoreContractAddress,
          "createPromoKitty(uint256,address)", promoKitty,false,
          0,10000000L,triggerAddress,triggerKey,blockingStubFull);
      logger.info("createPromoKitty " + txid);
      infoById = PublicMethod.getTransactionInfoById(txid,blockingStubFull);
      Assert.assertTrue(infoById.get().getResultValue() == 0);*/
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

    }

  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {  }
}


