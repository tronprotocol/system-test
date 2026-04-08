package stest.tron.wallet.dailybuild.tvmnewcommand.transferfailed;

import static org.hamcrest.core.StringContains.containsString;
import static org.tron.api.GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR;
import static org.tron.protos.Protocol.Transaction.Result.contractResult.SUCCESS_VALUE;

import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.GrpcAPI.Return;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.ProposalEnum;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class ContractTestSendCoin001 extends TronBaseTest {

  private static final long now = System.currentTimeMillis();
  private static final long TotalSupply = 1000L;
  private static String tokenName = "testAssetIssue_" + Long.toString(now);
  private static ByteString assetAccountId = null;
  private byte[] transferTokenContractAddress = null;
  private String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  private String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");
  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] dev001Address = ecKey1.getAddress();
  private String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ECKey ecKey2 = new ECKey(Utils.getRandom());
  private byte[] user001Address = ecKey2.getAddress();
  private String user001Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {    PublicMethod.printAddress(dev001Key);
  }

  @Test(enabled = true, description = "Sendcoin and transferAsset to contractAddresss ,"
      + "then selfdestruct", groups = {"contract", "daily"})
  public void testSendCoinAndTransferAssetContract001() {
    Assert.assertTrue(PublicMethod
        .sendcoin(dev001Address, 3100_000_000L, fromAddress, foundationKey, blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(PublicMethod.freezeBalanceForReceiver(fromAddress,
        PublicMethod.getFreezeBalanceCount(dev001Address, dev001Key, 130000L, blockingStubFull), 0,
        1, ByteString.copyFrom(dev001Address), foundationKey, blockingStubFull));

    Assert.assertTrue(PublicMethod.freezeBalanceForReceiver(fromAddress, 10_000_000L + PublicMethod.randomFreezeAmount.getAndAdd(1), 0, 0,
        ByteString.copyFrom(dev001Address), foundationKey, blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    long start = System.currentTimeMillis() + 2000;
    long end = System.currentTimeMillis() + 1000000000;
  //Create a new AssetIssue success.
    Assert.assertTrue(PublicMethod
        .createAssetIssue(dev001Address, tokenName, TotalSupply, 1, 10000, start, end, 1,
            description, url, 100000L, 100000L, 1L, 1L, dev001Key, blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    assetAccountId = PublicMethod.queryAccount(dev001Address, blockingStubFull).getAssetIssuedID();

    logger.info("The token name: " + tokenName);
    logger.info("The token ID: " + assetAccountId.toStringUtf8());
  //before deploy, check account resource
    AccountResourceMessage accountResource = PublicMethod
        .getAccountResource(dev001Address, blockingStubFull);
    long energyLimit = accountResource.getEnergyLimit();
    long energyUsage = accountResource.getEnergyUsed();
    long balanceBefore = PublicMethod.queryAccount(dev001Key, blockingStubFull).getBalance();
  Long devAssetCountBefore = PublicMethod
        .getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);

    logger.info("before energyLimit is " + Long.toString(energyLimit));
    logger.info("before energyUsage is " + Long.toString(energyUsage));
    logger.info("before balanceBefore is " + Long.toString(balanceBefore));
    logger.info("before AssetId: " + assetAccountId.toStringUtf8() + ", devAssetCountBefore: "
        + devAssetCountBefore);
  String filePath = "src/test/resources/soliditycode/contractTrcToken031.sol";
  String contractName = "token";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  String tokenId = assetAccountId.toStringUtf8();
    long tokenValue = 100;
    long callValue = 5;
  final String deployContractTxid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            callValue, 0, 10000, tokenId, tokenValue, null, dev001Key, dev001Address,
            blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    accountResource = PublicMethod.getAccountResource(dev001Address, blockingStubFull);
    energyLimit = accountResource.getEnergyLimit();
    energyUsage = accountResource.getEnergyUsed();
    long balanceAfter = PublicMethod.queryAccount(dev001Key, blockingStubFull).getBalance();
  Long devAssetCountAfter = PublicMethod
        .getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);

    logger.info("after energyLimit is " + Long.toString(energyLimit));
    logger.info("after energyUsage is " + Long.toString(energyUsage));
    logger.info("after balanceAfter is " + Long.toString(balanceAfter));
    logger.info("after AssetId: " + assetAccountId.toStringUtf8() + ", devAssetCountAfter: "
        + devAssetCountAfter);

    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(deployContractTxid, blockingStubFull);
    logger.info("Deploy energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());

    if (deployContractTxid == null || infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage()
          .toStringUtf8());
    }

    transferTokenContractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethod
        .getContract(transferTokenContractAddress, blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    Return ret = PublicMethod
        .transferAssetForReturn(transferTokenContractAddress, assetAccountId.toByteArray(), 100L,
            dev001Address, dev001Key, blockingStubFull);

    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, ret.getCode());
    Assert.assertEquals("Contract validate error : Cannot transfer asset to smartContract.",
        ret.getMessage().toStringUtf8());
  Long contractAssetCount = PublicMethod
        .getAssetIssueValue(transferTokenContractAddress, assetAccountId, blockingStubFull);
    logger.info("Contract has AssetId: " + assetAccountId.toStringUtf8() + ", Count: "
        + contractAssetCount);

    Assert.assertEquals(Long.valueOf(tokenValue), contractAssetCount);

    Return ret1 = PublicMethod
        .sendcoinForReturn(transferTokenContractAddress, 1_000_000L, fromAddress, foundationKey,
            blockingStubFull);
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, ret1.getCode());
    Assert.assertEquals("Contract validate error : Cannot transfer TRX to a smartContract.",
        ret1.getMessage().toStringUtf8());
  String num = "\"" + Base58.encode58Check(dev001Address) + "\"";
  String txid = PublicMethod
        .triggerContract(transferTokenContractAddress, "kill(address)", num, false, 0, maxFeeLimit,
            dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    long contractAssetCountBefore = PublicMethod
        .getAssetIssueValue(transferTokenContractAddress, assetAccountId, blockingStubFull);
    long contractBefore = PublicMethod.queryAccount(transferTokenContractAddress, blockingStubFull)
        .getBalance();

    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    if(PublicMethod.allowTvmSelfdestructRestrictionIsActive(blockingStubFull)) {
      Assert.assertFalse(PublicMethod
              .transferAsset(transferTokenContractAddress, assetAccountId.toByteArray(), 100L,
                      dev001Address, dev001Key, blockingStubFull));
      Assert.assertFalse(PublicMethod
              .sendcoin(transferTokenContractAddress, 1_000_000L, fromAddress, foundationKey,
                      blockingStubFull));
      PublicMethod.waitProduceNextBlock(blockingStubFull);
//      PublicMethod.waitProduceNextBlock(blockingStubFull);
      long contractAssetCountAfter = PublicMethod
              .getAssetIssueValue(transferTokenContractAddress, assetAccountId, blockingStubFull);
      long contractAfetr = PublicMethod.queryAccount(transferTokenContractAddress, blockingStubFull)
              .getBalance();

      Assert.assertEquals(contractAssetCountBefore, contractAssetCountAfter);
      Assert.assertEquals(contractBefore, contractAfetr);
    }else {
      Assert.assertTrue(PublicMethod
              .transferAsset(transferTokenContractAddress, assetAccountId.toByteArray(), 100L,
                      dev001Address, dev001Key, blockingStubFull));
      Assert.assertTrue(PublicMethod
              .sendcoin(transferTokenContractAddress, 1_000_000L, fromAddress, foundationKey,
                      blockingStubFull));
      PublicMethod.waitProduceNextBlock(blockingStubFull);
//      PublicMethod.waitProduceNextBlock(blockingStubFull);
      long contractAssetCountAfter = PublicMethod
              .getAssetIssueValue(transferTokenContractAddress, assetAccountId, blockingStubFull);
      long contractAfetr = PublicMethod.queryAccount(transferTokenContractAddress, blockingStubFull)
              .getBalance();

      Assert.assertEquals((contractAssetCountBefore + 100L), contractAssetCountAfter);
      Assert.assertEquals((contractBefore + 1_000_000L), contractAfetr);
    }
  }


  @Test(enabled = true, description = "Use create to generate a contract address "
      + "Sendcoin and transferAsset to contractAddresss ,then selfdestruct,", groups = {"contract", "daily"})
  public void testSendCoinAndTransferAssetContract002() {

    assetAccountId = PublicMethod.queryAccount(dev001Address, blockingStubFull).getAssetIssuedID();

    logger.info("The token name: " + tokenName);
    logger.info("The token ID: " + assetAccountId.toStringUtf8());
  //before deploy, check account resource
    AccountResourceMessage accountResource = PublicMethod
        .getAccountResource(dev001Address, blockingStubFull);
    long energyLimit = accountResource.getEnergyLimit();
    long energyUsage = accountResource.getEnergyUsed();
    long balanceBefore = PublicMethod.queryAccount(dev001Key, blockingStubFull).getBalance();
  Long devAssetCountBefore = PublicMethod
        .getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);

    logger.info("before energyLimit is " + Long.toString(energyLimit));
    logger.info("before energyUsage is " + Long.toString(energyUsage));
    logger.info("before balanceBefore is " + Long.toString(balanceBefore));
    logger.info("before AssetId: " + assetAccountId.toStringUtf8() + ", devAssetCountBefore: "
        + devAssetCountBefore);
  String filePath = "src/test/resources/soliditycode/contractTransferToken001.sol";
  String contractName = "A";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  String tokenId = assetAccountId.toStringUtf8();
    long tokenValue = 100;
    long callValue = 5;
  final String deployContractTxid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            callValue, 0, 10000, tokenId, tokenValue, null, dev001Key, dev001Address,
            blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(deployContractTxid, blockingStubFull);

    transferTokenContractAddress = infoById.get().getContractAddress().toByteArray();

    accountResource = PublicMethod.getAccountResource(dev001Address, blockingStubFull);
    energyLimit = accountResource.getEnergyLimit();
    energyUsage = accountResource.getEnergyUsed();
    long balanceAfter = PublicMethod.queryAccount(dev001Key, blockingStubFull).getBalance();
  Long devAssetCountAfter = PublicMethod
        .getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);

    logger.info("after energyLimit is " + Long.toString(energyLimit));
    logger.info("after energyUsage is " + Long.toString(energyUsage));
    logger.info("after balanceAfter is " + Long.toString(balanceAfter));
    logger.info("after AssetId: " + assetAccountId.toStringUtf8() + ", devAssetCountAfter: "
        + devAssetCountAfter);
  String txid = PublicMethod
        .triggerContract(transferTokenContractAddress, "newB()", "#", false, 0, maxFeeLimit,
            dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
  byte[] a = infoById.get().getContractResult(0).toByteArray();
  byte[] b = subByte(a, 11, 1);
  byte[] c = subByte(a, 0, 11);
  byte[] e = "41".getBytes();
  byte[] d = subByte(a, 12, 20);

    logger.info("a:" + ByteArray.toHexString(a));

    logger.info("b:" + ByteArray.toHexString(b));
    logger.info("c:" + ByteArray.toHexString(c));

    logger.info("d:" + ByteArray.toHexString(d));

    logger.info("41" + ByteArray.toHexString(d));
  String exceptedResult = "41" + ByteArray.toHexString(d);
  String realResult = ByteArray.toHexString(b);
    Assert.assertEquals(realResult, "00");
    Assert.assertNotEquals(realResult, "41");
  String addressFinal = Base58.encode58Check(ByteArray.fromHexString(exceptedResult));
    logger.info("create Address : " + addressFinal);
  byte[] testContractAddress = WalletClient.decodeFromBase58Check(addressFinal);

    Return ret = PublicMethod
        .transferAssetForReturn(testContractAddress, assetAccountId.toByteArray(), 100L,
            dev001Address, dev001Key, blockingStubFull);

    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, ret.getCode());
    Assert.assertEquals("Contract validate error : Cannot transfer asset to smartContract.",
        ret.getMessage().toStringUtf8());
  Long contractAssetCount = PublicMethod
        .getAssetIssueValue(testContractAddress, assetAccountId, blockingStubFull);
    logger.info("Contract has AssetId: " + assetAccountId.toStringUtf8() + ", Count: "
        + contractAssetCount);

    Assert.assertEquals(Long.valueOf(0L), contractAssetCount);

    Return ret1 = PublicMethod
        .sendcoinForReturn(testContractAddress, 1_000_000L, fromAddress, foundationKey,
            blockingStubFull);
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, ret1.getCode());
    Assert.assertEquals("Contract validate error : Cannot transfer TRX to a smartContract.",
        ret1.getMessage().toStringUtf8());
  String num = "\"" + Base58.encode58Check(dev001Address) + "\"";

    txid = PublicMethod
        .triggerContract(testContractAddress, "kill(address)", num, false, 0, maxFeeLimit,
            dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
//    PublicMethod.waitProduceNextBlock(blockingStubFull);

    long contractAssetCountBefore = PublicMethod
        .getAssetIssueValue(testContractAddress, assetAccountId, blockingStubFull);
    long contractBefore = PublicMethod.queryAccount(testContractAddress, blockingStubFull)
        .getBalance();

    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    if(PublicMethod.allowTvmSelfdestructRestrictionIsActive(blockingStubFull)) {
      Assert.assertFalse(PublicMethod
              .transferAsset(testContractAddress, assetAccountId.toByteArray(), 100L, dev001Address,
                      dev001Key, blockingStubFull));
      Assert.assertFalse(PublicMethod
              .sendcoin(testContractAddress, 1_000_000L, fromAddress, foundationKey, blockingStubFull));
      PublicMethod.waitProduceNextBlock(blockingStubFull);
//    PublicMethod.waitProduceNextBlock(blockingStubFull);
      long contractAssetCountAfter = PublicMethod
              .getAssetIssueValue(testContractAddress, assetAccountId, blockingStubFull);
      long contractAfetr = PublicMethod.queryAccount(testContractAddress, blockingStubFull)
              .getBalance();

      Assert.assertEquals(contractAssetCountBefore, contractAssetCountAfter);
      Assert.assertEquals(contractBefore, contractAfetr);
    }else {
      Assert.assertTrue(PublicMethod
              .transferAsset(testContractAddress, assetAccountId.toByteArray(), 100L, dev001Address,
                      dev001Key, blockingStubFull));
      Assert.assertTrue(PublicMethod
              .sendcoin(testContractAddress, 1_000_000L, fromAddress, foundationKey, blockingStubFull));
      PublicMethod.waitProduceNextBlock(blockingStubFull);
//    PublicMethod.waitProduceNextBlock(blockingStubFull);
      long contractAssetCountAfter = PublicMethod
              .getAssetIssueValue(testContractAddress, assetAccountId, blockingStubFull);
      long contractAfetr = PublicMethod.queryAccount(testContractAddress, blockingStubFull)
              .getBalance();

      Assert.assertEquals(contractAssetCountBefore + 100L, contractAssetCountAfter);
      Assert.assertEquals(contractBefore + 1_000_000L,  contractAfetr);
    }
  }


  @Test(enabled = true, description = "Use create2 to generate a contract address "
      + "Sendcoin and transferAsset to contractAddresss ,then selfdestruct", groups = {"contract", "daily"})
  public void testSendCoinAndTransferAssetContract003() {

    ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    Assert.assertTrue(PublicMethod
        .sendcoin(contractExcAddress, 2000000000L, fromAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "src/test/resources/soliditycode/create2contractn2.sol";
  String contractName = "Factory";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  byte[] contractAddress = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit, 0L, 100, null, contractExcKey,
            contractExcAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Account info;

    AccountResourceMessage resourceInfo = PublicMethod
        .getAccountResource(contractExcAddress, blockingStubFull);
    info = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
  String contractName1 = "TestConstract";
    HashMap retMap1 = PublicMethod.getBycodeAbi(filePath, contractName1);
  String code1 = retMap1.get("byteCode").toString();
  String abi1 = retMap1.get("abI").toString();
  String txid = "";
  String num = "\"" + code1 + "\"" + "," + 1;
    txid = PublicMethod
        .triggerContract(contractAddress, "deploy(bytes,uint256)", num, false, 0, maxFeeLimit, "0",
            0, contractExcAddress, contractExcKey, blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
  Long fee = infoById.get().getFee();
  Long netUsed = infoById.get().getReceipt().getNetUsage();
  Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
  Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);

    Account infoafter = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethod
        .getAccountResource(contractExcAddress, blockingStubFull);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
  byte[] returnAddressBytes = infoById.get().getInternalTransactions(0).getTransferToAddress()
        .toByteArray();
  String returnAddress = Base58.encode58Check(returnAddressBytes);
    logger.info("returnAddress:" + returnAddress);

    Return ret = PublicMethod
        .transferAssetForReturn(returnAddressBytes, assetAccountId.toByteArray(), 100L,
            dev001Address, dev001Key, blockingStubFull);

    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, ret.getCode());
    Assert.assertEquals("Contract validate error : Cannot transfer asset to smartContract.",
        ret.getMessage().toStringUtf8());

    Return ret1 = PublicMethod
        .transferAssetForReturn(returnAddressBytes, assetAccountId.toByteArray(), 100L,
            dev001Address, dev001Key, blockingStubFull);

    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, ret1.getCode());
    Assert.assertEquals("Contract validate error : Cannot transfer asset to smartContract.",
        ret1.getMessage().toStringUtf8());

    txid = PublicMethod
        .triggerContract(returnAddressBytes, "i()", "#", false, 0, maxFeeLimit, "0", 0,
            contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
  Long fee1 = infoById1.get().getFee();
  Long netUsed1 = infoById1.get().getReceipt().getNetUsage();
  Long energyUsed1 = infoById1.get().getReceipt().getEnergyUsage();
  Long netFee1 = infoById1.get().getReceipt().getNetFee();
    long energyUsageTotal1 = infoById1.get().getReceipt().getEnergyUsageTotal();

    logger.info("fee1:" + fee1);
    logger.info("netUsed1:" + netUsed1);
    logger.info("energyUsed1:" + energyUsed1);
    logger.info("netFee1:" + netFee1);
    logger.info("energyUsageTotal1:" + energyUsageTotal1);

    Account infoafter1 = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter1 = PublicMethod
        .getAccountResource(contractExcAddress, blockingStubFull);
  Long afterBalance1 = infoafter1.getBalance();
  Long afterEnergyUsed1 = resourceInfoafter1.getEnergyUsed();
  Long afterNetUsed1 = resourceInfoafter1.getNetUsed();
  Long afterFreeNetUsed1 = resourceInfoafter1.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance1);
    logger.info("afterEnergyUsed:" + afterEnergyUsed1);
    logger.info("afterNetUsed:" + afterNetUsed1);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed1);

    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance1 + fee1 == afterBalance);
    Assert.assertTrue(afterEnergyUsed + energyUsed1 >= afterEnergyUsed1);
  Long returnnumber = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById1.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(1 == returnnumber);
    txid = PublicMethod
        .triggerContract(returnAddressBytes, "set()", "#", false, 0, maxFeeLimit, "0", 0,
            contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    txid = PublicMethod
        .triggerContract(returnAddressBytes, "i()", "#", false, 0, maxFeeLimit, "0", 0,
            contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById1 = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    returnnumber = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById1.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(5 == returnnumber);
  String param1 = "\"" + Base58.encode58Check(returnAddressBytes) + "\"";

    //if the receiver is the contract, it will do nothing, contract is still alive
    txid = PublicMethod
        .triggerContract(returnAddressBytes, "testSuicideNonexistentTarget(address)", param1, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById2 = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);

    Assert.assertEquals("suicide",
        ByteArray.toStr(infoById2.get().getInternalTransactions(0).getNote().toByteArray()));
    TransactionExtention transactionExtention = PublicMethod
        .triggerContractForExtention(returnAddressBytes, "i()", "#", false, 0, maxFeeLimit, "0", 0,
            contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    if(PublicMethod.allowTvmSelfdestructRestrictionIsActive(blockingStubFull)) {
      Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
      Assert.assertFalse(PublicMethod
              .transferAsset(returnAddressBytes, assetAccountId.toByteArray(), 100L, dev001Address,
                      dev001Key, blockingStubFull));
      Assert.assertFalse(PublicMethod
              .sendcoin(returnAddressBytes, 1_000_000L, fromAddress, foundationKey, blockingStubFull));
      txid = PublicMethod
              .triggerContract(contractAddress, "deploy(bytes,uint256)", num, false, 0, maxFeeLimit, "0",
                      0, contractExcAddress, contractExcKey, blockingStubFull);
      PublicMethod.waitProduceNextBlock(blockingStubFull);

      Optional<TransactionInfo> infoById3 = PublicMethod
              .getTransactionInfoById(txid, blockingStubFull);
      Assert.assertEquals(TransactionInfo.code.FAILED, infoById3.get().getResult());

      txid = PublicMethod
              .triggerContract(returnAddressBytes, "i()", "#", false, 0, maxFeeLimit, "0", 0,
                      contractExcAddress, contractExcKey, blockingStubFull);

      PublicMethod.waitProduceNextBlock(blockingStubFull);
      infoById1 = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
      returnnumber = ByteArray.toLong(ByteArray
              .fromHexString(ByteArray.toHexString(infoById1.get().getContractResult(0).toByteArray())));
      Assert.assertTrue(5 == returnnumber);

      ret = PublicMethod
              .transferAssetForReturn(returnAddressBytes, assetAccountId.toByteArray(), 100L,
                      dev001Address, dev001Key, blockingStubFull);

      Assert.assertEquals(CONTRACT_VALIDATE_ERROR, ret.getCode());
      Assert.assertEquals("Contract validate error : Cannot transfer asset to smartContract.",
              ret.getMessage().toStringUtf8());

      ret1 = PublicMethod
              .transferAssetForReturn(returnAddressBytes, assetAccountId.toByteArray(), 100L,
                      dev001Address, dev001Key, blockingStubFull);

      Assert.assertEquals(CONTRACT_VALIDATE_ERROR, ret1.getCode());
      Assert.assertEquals("Contract validate error : Cannot transfer asset to smartContract.",
              ret1.getMessage().toStringUtf8());
    }else {
      Assert.assertTrue(transactionExtention.getResult().getCode().toString().contains("CONTRACT_VALIDATE_ERROR"));
      Assert.assertTrue(transactionExtention.getResult().getMessage().toStringUtf8()
              .contains("Contract validate error : No contract or not a valid smart contract"));
      Assert.assertTrue(PublicMethod
              .transferAsset(returnAddressBytes, assetAccountId.toByteArray(), 100L, dev001Address,
                      dev001Key, blockingStubFull));
      Assert.assertTrue(PublicMethod
              .sendcoin(returnAddressBytes, 1_000_000L, fromAddress, foundationKey, blockingStubFull));
      txid = PublicMethod
              .triggerContract(contractAddress, "deploy(bytes,uint256)", num, false, 0, maxFeeLimit, "0",
                      0, contractExcAddress, contractExcKey, blockingStubFull);
      PublicMethod.waitProduceNextBlock(blockingStubFull);

      Optional<TransactionInfo> infoById3 = PublicMethod
              .getTransactionInfoById(txid, blockingStubFull);
      byte[] returnAddressBytes1 = infoById3.get().getInternalTransactions(0).getTransferToAddress()
              .toByteArray();
      String returnAddress1 = Base58.encode58Check(returnAddressBytes1);
      Assert.assertEquals(returnAddress1, returnAddress);
      txid = PublicMethod
              .triggerContract(returnAddressBytes1, "i()", "#", false, 0, maxFeeLimit, "0", 0,
                      contractExcAddress, contractExcKey, blockingStubFull);

      PublicMethod.waitProduceNextBlock(blockingStubFull);
      infoById1 = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
      returnnumber = ByteArray.toLong(ByteArray
              .fromHexString(ByteArray.toHexString(infoById1.get().getContractResult(0).toByteArray())));
      Assert.assertTrue(1 == returnnumber);

      ret = PublicMethod
              .transferAssetForReturn(returnAddressBytes1, assetAccountId.toByteArray(), 100L,
                      dev001Address, dev001Key, blockingStubFull);

      Assert.assertEquals(CONTRACT_VALIDATE_ERROR, ret.getCode());
      Assert.assertEquals("Contract validate error : Cannot transfer asset to smartContract.",
              ret.getMessage().toStringUtf8());

      ret1 = PublicMethod
              .transferAssetForReturn(returnAddressBytes1, assetAccountId.toByteArray(), 100L,
                      dev001Address, dev001Key, blockingStubFull);

      Assert.assertEquals(CONTRACT_VALIDATE_ERROR, ret1.getCode());
      Assert.assertEquals("Contract validate error : Cannot transfer asset to smartContract.",
              ret1.getMessage().toStringUtf8());
    }



  }


  public byte[] subByte(byte[] b, int off, int length) {
    byte[] b1 = new byte[length];
    System.arraycopy(b, off, b1, 0, length);
    return b1;

  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(dev001Address, dev001Key, fromAddress, blockingStubFull);  }
}


