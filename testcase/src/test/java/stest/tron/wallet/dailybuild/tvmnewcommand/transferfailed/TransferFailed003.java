package stest.tron.wallet.dailybuild.tvmnewcommand.transferfailed;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.EnergyCost;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.MultiNode;

@Slf4j
@MultiNode
public class TransferFailed003 extends TronBaseTest {

  private static final long now = System.currentTimeMillis();
  private static final long TotalSupply = 10000000L;
  private static ByteString assetAccountId = null;
  private static String tokenName = "testAssetIssue_" + Long.toString(now);
  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethod.getFinalAddress(testNetAccountKey);
  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");
  byte[] contractAddress = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    initSolidityChannel();
    PublicMethod.printAddress(contractExcKey);    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext()
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext()
        .build();
    }


  @Test(enabled = true, description = "TransferToken enough tokenBalance", groups = {"contract", "daily"})
  public void test1TransferTokenEnough() {
    Assert.assertTrue(PublicMethod
        .sendcoin(contractExcAddress, 10000_000_000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    long start = System.currentTimeMillis() + 2000;
    long end = System.currentTimeMillis() + 1000000000;
  //Create a new AssetIssue success.
    Assert
        .assertTrue(PublicMethod.createAssetIssue(contractExcAddress, tokenName, TotalSupply, 1,
            10000, start, end, 1, description, url, 100000L,
            100000L, 1L, 1L, contractExcKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "src/test/resources/soliditycode/TransferFailed001.sol";
  String contractName = "EnergyOfTransferFailedTest";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();

    assetAccountId = PublicMethod.queryAccount(contractExcAddress, blockingStubFull)
        .getAssetIssuedID();
    contractAddress = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, 1000000000L,
        assetAccountId.toStringUtf8(), 100L, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  //Assert.assertTrue(PublicMethod.transferAsset(contractAddress,
    //    assetAccountId.toByteArray(), 100L, contractExcAddress, contractExcKey,
    //    blockingStubFull));
  //PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account info;

    AccountResourceMessage resourceInfo = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
  Long testNetAccountCountBefore = PublicMethod
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
  Long contractAccountCountBefore = PublicMethod
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("testNetAccountCountBefore:" + testNetAccountCountBefore);
    logger.info("contractAccountCountBefore:" + contractAccountCountBefore);
  String txid = "";
  String num = "1" + ",\"" + assetAccountId.toStringUtf8() + "\"";
    txid = PublicMethod.triggerContract(contractAddress,
        "testTransferTokenInsufficientBalance(uint256,trcToken)", num, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
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

    Account infoafter = PublicMethod.queryAccount(contractExcKey, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull1);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
  Long testNetAccountCountAfter = PublicMethod
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
  Long contractAccountCountAfter = PublicMethod
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("testNetAccountCountAfter:" + testNetAccountCountAfter);
    logger.info("contractAccountCountAfter:" + contractAccountCountAfter);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(testNetAccountCountBefore + 1 == testNetAccountCountAfter);
    Assert.assertTrue(contractAccountCountBefore - 1 == contractAccountCountAfter);

    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);


  }

  @Test(enabled = true, description = "TransferToken insufficient tokenBalance", groups = {"contract", "daily"})
  public void test2TransferTokenNotEnough() {

    Account info;

    AccountResourceMessage resourceInfo = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
  Long testNetAccountCountBefore = PublicMethod
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
  Long contractAccountCountBefore = PublicMethod
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("testNetAccountCountBefore:" + testNetAccountCountBefore);
    logger.info("contractAccountCountBefore:" + contractAccountCountBefore);
  String txid = "";
  String num = "1000" + ",\"" + assetAccountId.toStringUtf8() + "\"";
    txid = PublicMethod.triggerContract(contractAddress,
        "testTransferTokenInsufficientBalance(uint256,trcToken)", num, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
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

    Account infoafter = PublicMethod.queryAccount(contractExcKey, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull1);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
  Long testNetAccountCountAfter = PublicMethod
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
  Long contractAccountCountAfter = PublicMethod
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("testNetAccountCountAfter:" + testNetAccountCountAfter);
    logger.info("contractAccountCountAfter:" + contractAccountCountAfter);
    logger.info("infoById:" + infoById);
    Assert.assertTrue(infoById.get().getResultValue() == 1);
    Assert.assertEquals(contractResult.REVERT, infoById.get().getReceipt().getResult());
    Assert.assertEquals(
        "REVERT opcode executed",
        ByteArray.toStr(infoById.get().getResMessage().toByteArray()));
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertEquals(testNetAccountCountBefore, testNetAccountCountAfter);
    Assert.assertEquals(contractAccountCountBefore, contractAccountCountAfter);

    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Assert.assertNotEquals(10000000, energyUsageTotal);

  }


  @Test(enabled = true, description = "TransferToken to nonexistent target", groups = {"contract", "daily"})
  public void test3TransferTokenNonexistentTarget() {

    Account info;

    AccountResourceMessage resourceInfo = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
  Long testNetAccountCountBefore = PublicMethod
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
  Long contractAccountCountBefore = PublicMethod
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("testNetAccountCountBefore:" + testNetAccountCountBefore);
    logger.info("contractAccountCountBefore:" + contractAccountCountBefore);
  String txid = "";
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] nonexistentAddress = ecKey2.getAddress();
  String num =
        "\"1" + "\",\"" + Base58.encode58Check(nonexistentAddress) + "\",\"" + assetAccountId
            .toStringUtf8() + "\"";
    txid = PublicMethod.triggerContract(contractAddress,
        "testTransferTokenNonexistentTarget(uint256,address,trcToken)", num, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infoById:" + infoById);
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

    Account infoafter = PublicMethod.queryAccount(contractExcKey, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull1);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
  Long testNetAccountCountAfter = PublicMethod
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
  Long contractAccountCountAfter = PublicMethod
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("testNetAccountCountAfter:" + testNetAccountCountAfter);
    logger.info("contractAccountCountAfter:" + contractAccountCountAfter);

    Assert.assertEquals(0, infoById.get().getResultValue());

    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertEquals(testNetAccountCountBefore, testNetAccountCountAfter);
    Assert.assertEquals(contractAccountCountBefore - 1, contractAccountCountAfter.longValue());

    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Assert.assertNotEquals(10000000, energyUsageTotal);
  Long nonexistentAddressAccount = PublicMethod
        .getAssetIssueValue(nonexistentAddress, assetAccountId, blockingStubFull1);
    Assert.assertEquals(1L, nonexistentAddressAccount.longValue());

    txid = PublicMethod.triggerContract(contractAddress,
        "testTransferTokenNonexistentTarget(uint256,address,trcToken)", num, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    infoById = null;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infoById:" + infoById);
    fee = infoById.get().getFee();
    netUsed = infoById.get().getReceipt().getNetUsage();
    energyUsed = infoById.get().getReceipt().getEnergyUsage();
    netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal2 = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal2);

    infoafter = PublicMethod.queryAccount(contractExcKey, blockingStubFull1);
    resourceInfoafter = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull1);
    afterBalance = infoafter.getBalance();
    afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    afterNetUsed = resourceInfoafter.getNetUsed();
    afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    testNetAccountCountAfter = PublicMethod
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
    contractAccountCountAfter = PublicMethod
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("testNetAccountCountAfter:" + testNetAccountCountAfter);
    logger.info("contractAccountCountAfter:" + contractAccountCountAfter);

    Assert.assertEquals(0, infoById.get().getResultValue());

    Assert.assertEquals(testNetAccountCountBefore, testNetAccountCountAfter);
    Assert.assertEquals(contractAccountCountBefore - 2, contractAccountCountAfter.longValue());

    Assert.assertEquals(energyUsageTotal,
        energyUsageTotal2 + EnergyCost.getNewAcctCall());

    nonexistentAddressAccount = PublicMethod
        .getAssetIssueValue(nonexistentAddress, assetAccountId, blockingStubFull1);
    Assert.assertEquals(2L, nonexistentAddressAccount.longValue());
  }


  @Test(enabled = true, description = "TransferToken to myself", groups = {"contract", "daily"})
  public void test4TransferTokenSelf() {

    Account info;

    AccountResourceMessage resourceInfo = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
  Long testNetAccountCountBefore = PublicMethod
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
  Long contractAccountCountBefore = PublicMethod
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("testNetAccountCountBefore:" + testNetAccountCountBefore);
    logger.info("contractAccountCountBefore:" + contractAccountCountBefore);
  String txid = "";
  String num = "1" + ",\"" + assetAccountId.toStringUtf8() + "\"";
    txid = PublicMethod.triggerContract(contractAddress,
        "testTransferTokenSelf(uint256,trcToken)", num, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infoById:" + infoById);
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

    Account infoafter = PublicMethod.queryAccount(contractExcKey, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull1);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
  Long testNetAccountCountAfter = PublicMethod
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
  Long contractAccountCountAfter = PublicMethod
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("testNetAccountCountAfter:" + testNetAccountCountAfter);
    logger.info("contractAccountCountAfter:" + contractAccountCountAfter);

    Assert.assertTrue(infoById.get().getResultValue() == 1);
    Assert.assertEquals(contractResult.TRANSFER_FAILED, infoById.get().getReceipt().getResult());
    Assert.assertEquals(
        "transfer trc10 failed: Cannot transfer asset to yourself.",
        ByteArray.toStr(infoById.get().getResMessage().toByteArray()));
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertEquals(testNetAccountCountBefore, testNetAccountCountAfter);
    Assert.assertEquals(contractAccountCountBefore, contractAccountCountAfter);

    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Assert.assertNotEquals(10000000, energyUsageTotal);


  }


  @Test(enabled = true, description = "TransferToken notexist tokenID ", groups = {"contract", "daily"})
  public void test5TransferTokenNotexist() {

    Account info;

    AccountResourceMessage resourceInfo = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
  Long testNetAccountCountBefore = PublicMethod
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
  Long contractAccountCountBefore = PublicMethod
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("testNetAccountCountBefore:" + testNetAccountCountBefore);
    logger.info("contractAccountCountBefore:" + contractAccountCountBefore);
  String txid = "";
  String fakeassetAccountId = Long.toString(0L);
  String num = "1" + ",\"" + fakeassetAccountId + "\"";
    txid = PublicMethod.triggerContract(contractAddress,
        "testTransferTokenInsufficientBalance(uint256,trcToken)", num, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infoById:" + infoById);
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

    Account infoafter = PublicMethod.queryAccount(contractExcKey, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull1);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
  Long testNetAccountCountAfter = PublicMethod
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
  Long contractAccountCountAfter = PublicMethod
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("testNetAccountCountAfter:" + testNetAccountCountAfter);
    logger.info("contractAccountCountAfter:" + contractAccountCountAfter);

    Assert.assertTrue(infoById.get().getResultValue() == 1);
    Assert.assertEquals(contractResult.REVERT, infoById.get().getReceipt().getResult());
    Assert.assertEquals(
        "REVERT opcode executed",
        ByteArray.toStr(infoById.get().getResMessage().toByteArray()));
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertEquals(testNetAccountCountBefore, testNetAccountCountAfter);
    Assert.assertEquals(contractAccountCountBefore, contractAccountCountAfter);

    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);


  }


  @Test(enabled = true, description = "TransferToken to nonexistent target and "
      + "insufficient tokenBalance", groups = {"contract", "daily"})
  public void test7TransferTokenNonexistentTarget() {

    Account info;

    AccountResourceMessage resourceInfo = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
  Long testNetAccountCountBefore = PublicMethod
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
  Long contractAccountCountBefore = PublicMethod
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("testNetAccountCountBefore:" + testNetAccountCountBefore);
    logger.info("contractAccountCountBefore:" + contractAccountCountBefore);
  String txid = "";
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] nonexistentAddress = ecKey2.getAddress();
  String num =
        "\"100000000000" + "\",\"" + Base58.encode58Check(nonexistentAddress) + "\",\""
            + assetAccountId
            .toStringUtf8() + "\"";
    txid = PublicMethod.triggerContract(contractAddress,
        "testTransferTokenNonexistentTarget(uint256,address,trcToken)", num, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infoById:" + infoById);
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

    Account infoafter = PublicMethod.queryAccount(contractExcKey, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull1);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
  Long testNetAccountCountAfter = PublicMethod
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
  Long contractAccountCountAfter = PublicMethod
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("testNetAccountCountAfter:" + testNetAccountCountAfter);
    logger.info("contractAccountCountAfter:" + contractAccountCountAfter);

    Assert.assertTrue(infoById.get().getResultValue() == 1);
    Assert.assertEquals(contractResult.REVERT, infoById.get().getReceipt().getResult());
    Assert.assertEquals(
        "REVERT opcode executed",
        ByteArray.toStr(infoById.get().getResMessage().toByteArray()));
    Assert.assertEquals(afterBalance + fee, beforeBalance.longValue());
    Assert.assertEquals(testNetAccountCountBefore, testNetAccountCountAfter);
    Assert.assertEquals(contractAccountCountBefore, contractAccountCountAfter);

    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Assert.assertNotEquals(10000000, energyUsageTotal);
  Long nonexistentAddressAccount = PublicMethod
        .getAssetIssueValue(nonexistentAddress, assetAccountId, blockingStubFull1);
    Assert.assertEquals(0L, nonexistentAddressAccount.longValue());


  }


  @Test(enabled = true, description = "TransferToken to myself and insufficient tokenBalance", groups = {"contract", "daily"})
  public void test8TransferTokenSelf() {

    Account info;

    AccountResourceMessage resourceInfo = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
  Long testNetAccountCountBefore = PublicMethod
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
  Long contractAccountCountBefore = PublicMethod
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("testNetAccountCountBefore:" + testNetAccountCountBefore);
    logger.info("contractAccountCountBefore:" + contractAccountCountBefore);
  String txid = "";
  String num = "1000000000000000" + ",\"" + assetAccountId.toStringUtf8() + "\"";
    txid = PublicMethod.triggerContract(contractAddress,
        "testTransferTokenSelf(uint256,trcToken)", num, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infoById:" + infoById);
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

    Account infoafter = PublicMethod.queryAccount(contractExcKey, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull1);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
  Long testNetAccountCountAfter = PublicMethod
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
  Long contractAccountCountAfter = PublicMethod
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("testNetAccountCountAfter:" + testNetAccountCountAfter);
    logger.info("contractAccountCountAfter:" + contractAccountCountAfter);

    Assert.assertTrue(infoById.get().getResultValue() == 1);
    Assert.assertEquals(contractResult.REVERT, infoById.get().getReceipt().getResult());
    Assert.assertEquals(
        "REVERT opcode executed",
        ByteArray.toStr(infoById.get().getResMessage().toByteArray()));
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertEquals(testNetAccountCountBefore, testNetAccountCountAfter);
    Assert.assertEquals(contractAccountCountBefore, contractAccountCountAfter);

    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);

    Assert.assertNotEquals(10000000, energyUsageTotal);

  }

  @Test(enabled = true, description = "TransferToken to nonexistent target, but revert", groups = {"contract", "daily"})
  public void test9TransferTokenNonexistentTargetRevert() {
    Account info;

    AccountResourceMessage resourceInfo = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
  Long testNetAccountCountBefore = PublicMethod
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
  Long contractAccountCountBefore = PublicMethod
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("testNetAccountCountBefore:" + testNetAccountCountBefore);
    logger.info("contractAccountCountBefore:" + contractAccountCountBefore);
  String txid = "";
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] nonexistentAddress = ecKey2.getAddress();
  String num =
        "\"1" + "\",\"" + Base58.encode58Check(nonexistentAddress) + "\",\"" + assetAccountId
            .toStringUtf8() + "\"";
    txid = PublicMethod.triggerContract(contractAddress,
        "testTransferTokenRevert(uint256,address,trcToken)", num, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infoById:" + infoById);
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

    Account infoafter = PublicMethod.queryAccount(contractExcKey, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull1);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
  Long testNetAccountCountAfter = PublicMethod
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
  Long contractAccountCountAfter = PublicMethod
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("testNetAccountCountAfter:" + testNetAccountCountAfter);
    logger.info("contractAccountCountAfter:" + contractAccountCountAfter);

    Assert.assertEquals(1, infoById.get().getResultValue());

    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertEquals(testNetAccountCountBefore, testNetAccountCountAfter);
    Assert.assertEquals(contractAccountCountBefore, contractAccountCountAfter);

    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Assert.assertTrue(energyUsageTotal > EnergyCost.getNewAcctCall());
  Long nonexistentAddressAccount = PublicMethod
        .getAssetIssueValue(nonexistentAddress, assetAccountId, blockingStubFull1);
    Assert.assertEquals(0L, nonexistentAddressAccount.longValue());
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod
        .freeResource(contractExcAddress, contractExcKey, testNetAccountAddress, blockingStubFull);    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }  }


}
