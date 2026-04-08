package stest.tron.wallet.dailybuild.trctoken;

import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class ContractTrcToken036 extends TronBaseTest {


  private static final long now = System.currentTimeMillis();
  private static final long TotalSupply = 10000000L;
  private static ByteString assetAccountId = null;
  private static String tokenName = "testAssetIssue_" + Long.toString(now);
  byte[] transferTokenContractAddress;
  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] dev001Address = ecKey1.getAddress();
  String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] user001Address = ecKey2.getAddress();
  String user001Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  int originEnergyLimit = 50000;
  byte[] transferTokenWithPureTestAddress;  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {  }


  @Test(enabled = false, description = "Deploy contract", groups = {"contract", "daily"})
  public void deploy01TransferTokenContract() {

    Assert
        .assertTrue(PublicMethod.sendcoin(dev001Address, 9999000000L, foundationAddress,
            testKey002, blockingStubFull));
    logger.info(
        "dev001Address:" + Base58.encode58Check(dev001Address));
    Assert
        .assertTrue(PublicMethod.sendcoin(user001Address, 4048000000L, foundationAddress,
            testKey002, blockingStubFull));
    logger.info(
        "user001Address:" + Base58.encode58Check(user001Address));
    PublicMethod.waitProduceNextBlock(blockingStubFull);


    long start = System.currentTimeMillis() + 2000;
    long end = System.currentTimeMillis() + 1000000000;
  //Create a new AssetIssue success.
    Assert.assertTrue(PublicMethod.createAssetIssue(dev001Address, tokenName, TotalSupply, 1,
        100, start, end, 1, description, url, 10000L,
        10000L, 1L, 1L, dev001Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    assetAccountId = PublicMethod.queryAccount(dev001Address, blockingStubFull).getAssetIssuedID();
  // deploy transferTokenContract
    //    String filePath = "src/test/resources/soliditycode/contractTrcToken036.sol";
  //    String contractName = "IllegalDecorate";
  //    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  //    String code = retMap.get("byteCode").toString();
  //    String abi = retMap.get("abI").toString();
  //    transferTokenContractAddress = PublicMethod
    //        .deployContract(contractName, abi, code, "", maxFeeLimit,
    //            0L, 0, originEnergyLimit, "0",
    //            0, null, dev001Key, dev001Address,
    //            blockingStubFull);
  //
    //    // devAddress transfer token to userAddress
    //    PublicMethod
    //        .transferAsset(transferTokenContractAddress, assetAccountId.toByteArray(), 100,
    //            dev001Address,
    //            dev001Key,
    //            blockingStubFull);
  //    Assert
    //        .assertTrue(PublicMethod.sendcoin(transferTokenContractAddress, 100, foundationAddress,
    //            testKey002, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = false, description = "Trigger transferTokenWithPure contract", groups = {"contract", "daily"})
  public void deploy02TransferTokenContract() {
    Account info;
    AccountResourceMessage resourceInfo = PublicMethod.getAccountResource(dev001Address,
        blockingStubFull);
    info = PublicMethod.queryAccount(dev001Address, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
  Long beforeAssetIssueDevAddress = PublicMethod
        .getAssetIssueValue(dev001Address, assetAccountId,
            blockingStubFull);
  Long beforeAssetIssueUserAddress = PublicMethod
        .getAssetIssueValue(user001Address, assetAccountId,
            blockingStubFull);
  Long beforeAssetIssueContractAddress = PublicMethod
        .getAssetIssueValue(transferTokenContractAddress,
            assetAccountId,
            blockingStubFull);
  Long user001AddressAddressBalance = PublicMethod
        .queryAccount(user001Address, blockingStubFull).getBalance();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("beforeAssetIssueCount:" + beforeAssetIssueContractAddress);
    logger.info("beforeAssetIssueDevAddress:" + beforeAssetIssueDevAddress);
    logger.info("beforeAssetIssueUserAddress:" + beforeAssetIssueUserAddress);
    logger.info("user001AddressAddressBalance:" + user001AddressAddressBalance);
  // user trigger A to transfer token to B
    String param =
        "\"" + Base58.encode58Check(user001Address) + "\",\"1\"";
  final String triggerTxid = PublicMethod.triggerContract(transferTokenContractAddress,
        "transferTokenWithPure(address,uint256)",
        param, false, 10, 1000000000L, assetAccountId
            .toStringUtf8(),
        10, dev001Address, dev001Key,
        blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account infoafter = PublicMethod.queryAccount(dev001Address, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethod.getAccountResource(dev001Address,
        blockingStubFull);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterAssetIssueDevAddress = PublicMethod
        .getAssetIssueValue(dev001Address, assetAccountId,
            blockingStubFull);
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
  Long afterAssetIssueContractAddress = PublicMethod
        .getAssetIssueValue(transferTokenContractAddress,
            assetAccountId,
            blockingStubFull);
  Long afterAssetIssueUserAddress = PublicMethod
        .getAssetIssueValue(user001Address, assetAccountId,
            blockingStubFull);
  Long afteruser001AddressAddressBalance = PublicMethod
        .queryAccount(user001Address, blockingStubFull).getBalance();

    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("afterAssetIssueCount:" + afterAssetIssueDevAddress);
    logger.info("afterAssetIssueDevAddress:" + afterAssetIssueContractAddress);
    logger.info("afterAssetIssueUserAddress:" + afterAssetIssueUserAddress);
    logger.info("afterContractAddressBalance:" + afteruser001AddressAddressBalance);

    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(beforeAssetIssueDevAddress - 10 == afterAssetIssueDevAddress);
    Assert.assertTrue(beforeAssetIssueUserAddress + 10 == afterAssetIssueUserAddress);
    Assert.assertTrue(user001AddressAddressBalance + 10 == afteruser001AddressAddressBalance);
  String filePath = "src/test/resources/soliditycode/contractTrcToken036.sol";
  String contractName1 = "IllegalDecorate1";
    HashMap retMap1 = PublicMethod.getBycodeAbi(filePath, contractName1);
  String code1 = retMap1.get("byteCode").toString();
  String abi1 = retMap1.get("abI").toString();
    transferTokenWithPureTestAddress = PublicMethod
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            0L, 0, originEnergyLimit, "0",
            0, null, dev001Key, dev001Address,
            blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  // devAddress transfer token to userAddress
    PublicMethod
        .transferAsset(transferTokenWithPureTestAddress, assetAccountId.toByteArray(), 100,
            dev001Address,
            dev001Key,
            blockingStubFull);
    Assert
        .assertTrue(PublicMethod.sendcoin(transferTokenWithPureTestAddress, 100, foundationAddress,
            testKey002, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = false, description = "Trigger transferTokenWithConstant contract", groups = {"contract", "daily"})
  public void deploy03TransferTokenContract() {
    Account info1;
    AccountResourceMessage resourceInfo1 = PublicMethod.getAccountResource(dev001Address,
        blockingStubFull);
    info1 = PublicMethod.queryAccount(dev001Address, blockingStubFull);
  Long beforeBalance1 = info1.getBalance();
  Long beforeEnergyUsed1 = resourceInfo1.getEnergyUsed();
  Long beforeNetUsed1 = resourceInfo1.getNetUsed();
  Long beforeFreeNetUsed1 = resourceInfo1.getFreeNetUsed();
  Long beforeAssetIssueDevAddress1 = PublicMethod
        .getAssetIssueValue(dev001Address, assetAccountId,
            blockingStubFull);
  Long beforeAssetIssueUserAddress1 = PublicMethod
        .getAssetIssueValue(user001Address, assetAccountId,
            blockingStubFull);
  Long beforeAssetIssueContractAddress1 = PublicMethod
        .getAssetIssueValue(transferTokenContractAddress,
            assetAccountId,
            blockingStubFull);
  Long user001AddressAddressBalance1 = PublicMethod
        .queryAccount(user001Address, blockingStubFull).getBalance();
    logger.info("beforeBalance:" + beforeBalance1);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed1);
    logger.info("beforeNetUsed:" + beforeNetUsed1);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed1);
    logger.info("beforeAssetIssueCount:" + beforeAssetIssueContractAddress1);
    logger.info("beforeAssetIssueDevAddress:" + beforeAssetIssueDevAddress1);
    logger.info("beforeAssetIssueUserAddress:" + beforeAssetIssueUserAddress1);
    logger.info("user001AddressAddressBalance:" + user001AddressAddressBalance1);
  // user trigger A to transfer token to B
    String param1 =
        "\"" + Base58.encode58Check(user001Address) + "\",\"1\"";
  final String triggerTxid1 = PublicMethod.triggerContract(transferTokenWithPureTestAddress,
        "transferTokenWithConstant(address,uint256)",
        param1, false, 10, 1000000000L, assetAccountId
            .toStringUtf8(),
        10, dev001Address, dev001Key,
        blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account infoafter1 = PublicMethod.queryAccount(dev001Address, blockingStubFull);
    AccountResourceMessage resourceInfoafter1 = PublicMethod.getAccountResource(dev001Address,
        blockingStubFull);
  Long afterBalance1 = infoafter1.getBalance();
  Long afterEnergyUsed1 = resourceInfoafter1.getEnergyUsed();
  Long afterAssetIssueDevAddress1 = PublicMethod
        .getAssetIssueValue(dev001Address, assetAccountId,
            blockingStubFull);
  Long afterNetUsed1 = resourceInfoafter1.getNetUsed();
  Long afterFreeNetUsed1 = resourceInfoafter1.getFreeNetUsed();
  Long afterAssetIssueContractAddress1 = PublicMethod
        .getAssetIssueValue(transferTokenContractAddress,
            assetAccountId,
            blockingStubFull);
  Long afterAssetIssueUserAddress1 = PublicMethod
        .getAssetIssueValue(user001Address, assetAccountId,
            blockingStubFull);
  Long afteruser001AddressAddressBalance1 = PublicMethod
        .queryAccount(user001Address, blockingStubFull).getBalance();

    logger.info("afterBalance:" + afterBalance1);
    logger.info("afterEnergyUsed:" + afterEnergyUsed1);
    logger.info("afterNetUsed:" + afterNetUsed1);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed1);
    logger.info("afterAssetIssueCount:" + afterAssetIssueDevAddress1);
    logger.info("afterAssetIssueDevAddress:" + afterAssetIssueContractAddress1);
    logger.info("afterAssetIssueUserAddress:" + afterAssetIssueUserAddress1);
    logger.info("afterContractAddressBalance:" + afteruser001AddressAddressBalance1);

    Optional<TransactionInfo> infoById1 = PublicMethod
        .getTransactionInfoById(triggerTxid1, blockingStubFull);
    Assert.assertEquals(beforeBalance1, afterBalance1);
    Assert.assertEquals(beforeAssetIssueDevAddress1, afterAssetIssueDevAddress1);
    Assert.assertEquals(beforeAssetIssueUserAddress1, afterAssetIssueUserAddress1);
    Assert.assertEquals(user001AddressAddressBalance1, afteruser001AddressAddressBalance1);
  }

  @Test(enabled = false, description = "Trigger transferTokenWithView contract", groups = {"contract", "daily"})
  public void deploy04TransferTokenContract() {
    String filePath2 = "src/test/resources/soliditycode/contractTrcToken036.sol";
  String contractName2 = "IllegalDecorate2";
    HashMap retMap2 = PublicMethod.getBycodeAbi(filePath2, contractName2);
  String code2 = retMap2.get("byteCode").toString();
  String abi2 = retMap2.get("abI").toString();
  byte[] transferTokenWithViewAddress = PublicMethod
        .deployContract(contractName2, abi2, code2, "", maxFeeLimit,
            0L, 0, originEnergyLimit, "0",
            0, null, dev001Key, dev001Address,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  // devAddress transfer token to userAddress
    PublicMethod
        .transferAsset(transferTokenWithViewAddress, assetAccountId.toByteArray(), 100,
            dev001Address,
            dev001Key,
            blockingStubFull);
    Assert
        .assertTrue(PublicMethod.sendcoin(transferTokenWithViewAddress, 100, foundationAddress,
            testKey002, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account info2;
    AccountResourceMessage resourceInfo2 = PublicMethod.getAccountResource(dev001Address,
        blockingStubFull);
    info2 = PublicMethod.queryAccount(dev001Address, blockingStubFull);
  Long beforeBalance2 = info2.getBalance();
  Long beforeEnergyUsed2 = resourceInfo2.getEnergyUsed();
  Long beforeNetUsed2 = resourceInfo2.getNetUsed();
  Long beforeFreeNetUsed2 = resourceInfo2.getFreeNetUsed();
  Long beforeAssetIssueDevAddress2 = PublicMethod
        .getAssetIssueValue(dev001Address, assetAccountId,
            blockingStubFull);
  Long beforeAssetIssueUserAddress2 = PublicMethod
        .getAssetIssueValue(user001Address, assetAccountId,
            blockingStubFull);
  Long beforeAssetIssueContractAddress2 = PublicMethod
        .getAssetIssueValue(transferTokenWithViewAddress,
            assetAccountId,
            blockingStubFull);
  Long user001AddressAddressBalance2 = PublicMethod
        .queryAccount(user001Address, blockingStubFull).getBalance();
    logger.info("beforeAssetIssueContractAddress2:" + beforeAssetIssueContractAddress2);
    logger.info("beforeAssetIssueDevAddress2:" + beforeAssetIssueDevAddress2);
    logger.info("beforeAssetIssueUserAddress2:" + beforeAssetIssueUserAddress2);
    logger.info("user001AddressAddressBalance2:" + user001AddressAddressBalance2);
  // user trigger A to transfer token to B
    String param2 =
        "\"" + Base58.encode58Check(user001Address) + "\",\"1\"";
  String triggerTxid2 = PublicMethod.triggerContract(transferTokenWithViewAddress,
        "transferTokenWithView(address,uint256)",
        param2, false, 10, 1000000000L, assetAccountId
            .toStringUtf8(),
        10, dev001Address, dev001Key,
        blockingStubFull);

    Account infoafter2 = PublicMethod.queryAccount(dev001Address, blockingStubFull);
    AccountResourceMessage resourceInfoafter2 = PublicMethod.getAccountResource(dev001Address,
        blockingStubFull);
  Long afterBalance2 = infoafter2.getBalance();
  Long afterEnergyUsed2 = resourceInfoafter2.getEnergyUsed();
  Long afterAssetIssueDevAddress2 = PublicMethod
        .getAssetIssueValue(dev001Address, assetAccountId,
            blockingStubFull);
  Long afterNetUsed2 = resourceInfoafter2.getNetUsed();
  Long afterFreeNetUsed2 = resourceInfoafter2.getFreeNetUsed();
  Long afterAssetIssueContractAddress2 = PublicMethod
        .getAssetIssueValue(transferTokenWithViewAddress,
            assetAccountId,
            blockingStubFull);
  Long afterAssetIssueUserAddress2 = PublicMethod
        .getAssetIssueValue(user001Address, assetAccountId,
            blockingStubFull);
  Long afteruser001AddressAddressBalance2 = PublicMethod
        .queryAccount(user001Address, blockingStubFull).getBalance();

    logger.info("afterAssetIssueDevAddress2:" + afterAssetIssueDevAddress2);
    logger.info("afterAssetIssueContractAddress2:" + afterAssetIssueContractAddress2);
    logger.info("afterAssetIssueUserAddress2:" + afterAssetIssueUserAddress2);
    logger.info("afteruser001AddressAddressBalance2:" + afteruser001AddressAddressBalance2);

    Optional<TransactionInfo> infoById2 = PublicMethod
        .getTransactionInfoById(triggerTxid2, blockingStubFull);

    Assert.assertEquals(beforeAssetIssueDevAddress2, afterAssetIssueDevAddress2);
    Assert.assertEquals(beforeAssetIssueUserAddress2, afterAssetIssueUserAddress2);
    Assert.assertEquals(user001AddressAddressBalance2, afteruser001AddressAddressBalance2);
  }

  @Test(enabled = false, description = "Trigger transferTokenWithNoPayable contract", groups = {"contract", "daily"})
  public void deploy05TransferTokenContract() {
    String filePath = "src/test/resources/soliditycode/contractTrcToken036.sol";
  String contractName3 = "IllegalDecorate3";
    HashMap retMap3 = PublicMethod.getBycodeAbi(filePath, contractName3);
  String code3 = retMap3.get("byteCode").toString();
  String abi3 = retMap3.get("abI").toString();
  byte[] transferTokenWithOutPayableTestAddress = PublicMethod
        .deployContract(contractName3, abi3, code3, "", maxFeeLimit,
            0L, 0, originEnergyLimit, "0",
            0, null, dev001Key, dev001Address,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    PublicMethod
        .transferAsset(transferTokenWithOutPayableTestAddress, assetAccountId.toByteArray(), 100,
            dev001Address,
            dev001Key,
            blockingStubFull);
    Assert
        .assertTrue(PublicMethod.sendcoin(transferTokenWithOutPayableTestAddress, 100, foundationAddress,
            testKey002, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Account info3;
    AccountResourceMessage resourceInfo3 = PublicMethod.getAccountResource(dev001Address,
        blockingStubFull);
    info3 = PublicMethod.queryAccount(dev001Address, blockingStubFull);
  Long beforeBalance3 = info3.getBalance();
  Long beforeEnergyUsed3 = resourceInfo3.getEnergyUsed();
  Long beforeNetUsed3 = resourceInfo3.getNetUsed();
  Long beforeFreeNetUsed3 = resourceInfo3.getFreeNetUsed();
  Long beforeAssetIssueDevAddress3 = PublicMethod
        .getAssetIssueValue(dev001Address, assetAccountId,
            blockingStubFull);
  Long beforeAssetIssueUserAddress3 = PublicMethod
        .getAssetIssueValue(user001Address, assetAccountId,
            blockingStubFull);
  Long beforeAssetIssueContractAddress3 = PublicMethod
        .getAssetIssueValue(
            transferTokenWithOutPayableTestAddress,
            assetAccountId,
            blockingStubFull);
  Long user001AddressAddressBalance3 = PublicMethod
        .queryAccount(user001Address, blockingStubFull).getBalance();
    logger.info("beforeBalance:" + beforeBalance3);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed3);
    logger.info("beforeNetUsed:" + beforeNetUsed3);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed3);
    logger.info("beforeAssetIssueCount:" + beforeAssetIssueContractAddress3);
    logger.info("beforeAssetIssueDevAddress:" + beforeAssetIssueDevAddress3);
    logger.info("beforeAssetIssueUserAddress:" + beforeAssetIssueUserAddress3);
    logger.info("user001AddressAddressBalance:" + user001AddressAddressBalance3);
  String param3 =
        "\"" + Base58.encode58Check(user001Address) + "\",\"1\"";
  String triggerTxid3 = PublicMethod.triggerContract(transferTokenWithOutPayableTestAddress,
        "transferTokenWithOutPayable(address,uint256)",
        param3, false, 10, 1000000000L, assetAccountId
            .toStringUtf8(),
        10, dev001Address, dev001Key,
        blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account infoafter3 = PublicMethod.queryAccount(dev001Address, blockingStubFull);
    AccountResourceMessage resourceInfoafter3 = PublicMethod.getAccountResource(dev001Address,
        blockingStubFull);
  Long afterBalance3 = infoafter3.getBalance();
  Long afterEnergyUsed3 = resourceInfoafter3.getEnergyUsed();
  Long afterAssetIssueDevAddress3 = PublicMethod
        .getAssetIssueValue(dev001Address, assetAccountId,
            blockingStubFull);
  Long afterNetUsed3 = resourceInfoafter3.getNetUsed();
  Long afterFreeNetUsed3 = resourceInfoafter3.getFreeNetUsed();
  Long afterAssetIssueContractAddress3 = PublicMethod
        .getAssetIssueValue(
            transferTokenWithOutPayableTestAddress, assetAccountId,
            blockingStubFull);
  Long afterAssetIssueUserAddress3 = PublicMethod
        .getAssetIssueValue(user001Address, assetAccountId,
            blockingStubFull);
  Long afteruser001AddressAddressBalance3 = PublicMethod
        .queryAccount(user001Address, blockingStubFull).getBalance();

    Optional<TransactionInfo> infoById3 = PublicMethod
        .getTransactionInfoById(triggerTxid3, blockingStubFull);
    Assert.assertTrue(infoById3.get().getResultValue() == 1);

    Assert.assertEquals(beforeAssetIssueDevAddress3, afterAssetIssueDevAddress3);
    Assert.assertEquals(beforeAssetIssueUserAddress3, afterAssetIssueUserAddress3);
    Assert.assertEquals(user001AddressAddressBalance3, afteruser001AddressAddressBalance3);
    PublicMethod.unFreezeBalance(dev001Address, dev001Key, 1,
        null, blockingStubFull);
    PublicMethod.unFreezeBalance(user001Address, user001Key, 1,
        null, blockingStubFull);

  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(dev001Address, dev001Key, foundationAddress, blockingStubFull);
    PublicMethod.freeResource(user001Address, user001Key, foundationAddress, blockingStubFull);
    PublicMethod.unFreezeBalance(foundationAddress, testKey002, 0, dev001Address, blockingStubFull);
    PublicMethod.unFreezeBalance(foundationAddress, testKey002, 0, user001Address, blockingStubFull);  }


}


