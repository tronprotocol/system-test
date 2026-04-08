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
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.ProposalEnum;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.MultiNode;

@Slf4j
@MultiNode
public class TransferFailed004 extends TronBaseTest {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethod.getFinalAddress(testNetAccountKey);
  byte[] contractAddress = null;
  byte[] contractAddress2 = null;
  byte[] contractAddress3 = null;
  private static final long now = System.currentTimeMillis();
  private static final long TotalSupply = 10000000L;
  private static ByteString assetAccountId = null;
  private static ByteString assetAccountId2 = null;
  private static String tokenName = "testAssetIssue_" + Long.toString(now);
  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress3 = ecKey3.getAddress();
  String contractExcKey3 = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] nonexistentAddress = ecKey2.getAddress();
  long energyUsageTotal = 0;
  long energyUsageTotal2 = 0;
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
    Assert.assertTrue(PublicMethod
        .sendcoin(contractExcAddress, 10000000000L, testNetAccountAddress, testNetAccountKey,
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

    assetAccountId = PublicMethod.queryAccount(contractExcAddress, blockingStubFull)
        .getAssetIssuedID();

  }

  @Test(enabled = true, description = "Suicide nonexistent target", groups = {"contract", "daily"})
  public void test1SuicideNonexistentTarget() {
    Assert.assertTrue(PublicMethod
        .sendcoin(contractExcAddress, 10000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "src/test/resources/soliditycode/TransferFailed001.sol";
  String contractName = "EnergyOfTransferFailedTest";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();

    contractAddress = PublicMethod.deployContractFallbackReceive(contractName, abi, code, "",
        maxFeeLimit, 1000000L, 100,1000L, assetAccountId.toStringUtf8(), 1000L,
        null, contractExcKey, contractExcAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
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
  String num = "\"" + Base58.encode58Check(nonexistentAddress) + "\"";

    txid = PublicMethod.triggerContract(contractAddress,
        "testSuicideNonexistentTarget(address)", num, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infobyid : --- " + infoById);
  Long fee = infoById.get().getFee();
  Long netUsed = infoById.get().getReceipt().getNetUsage();
  final Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
  final Long netFee = infoById.get().getReceipt().getNetFee();
    energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();
  Long testNetAccountCountAfter = PublicMethod
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
  Long contractAccountCountAfter = PublicMethod
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);
    logger.info("testNetAccountCountAfter:" + testNetAccountCountAfter);
    logger.info("contractAccountCountAfter:" + contractAccountCountAfter);

    Account infoafter = PublicMethod.queryAccount(contractExcKey, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull1);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertEquals(0, infoById.get().getResultValue());

    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Assert.assertEquals(testNetAccountCountBefore, testNetAccountCountAfter);
    Assert.assertEquals(0L, contractAccountCountAfter.longValue());
    Assert.assertNotEquals(10000000, energyUsageTotal);
  String assetIssueId = PublicMethod.queryAccount(contractAddress, blockingStubFull)
        .getAssetIssuedID().toStringUtf8();
    logger.info("assetIssueId: " + assetIssueId);
    Assert.assertEquals(0, assetIssueId.length());

    Account nonexistentAddressAccountTrxBalance = PublicMethod
        .queryAccount(nonexistentAddress, blockingStubFull1);
    Assert.assertEquals(1000000L, nonexistentAddressAccountTrxBalance.getBalance());
  Long nonexistentAddressAccountTokenBalance = PublicMethod
        .getAssetIssueValue(nonexistentAddress, assetAccountId, blockingStubFull1);
    Assert.assertEquals(1000L, nonexistentAddressAccountTokenBalance.longValue());
  }

  @Test(enabled = true, description = "Suicide existent target", groups = {"contract", "daily"})
  public void test2SuicideExistentTarget() {
    Assert.assertTrue(PublicMethod
        .sendcoin(contractExcAddress, 10000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
  String filePath = "src/test/resources/soliditycode/TransferFailed001.sol";
  String contractName = "EnergyOfTransferFailedTest";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();

    contractAddress2 = PublicMethod.deployContractFallbackReceive(contractName, abi, code, "",
        maxFeeLimit, 1000000L, 100,1000L, assetAccountId.toStringUtf8(),
        1000L,null, contractExcKey, contractExcAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
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

    Assert.assertTrue(PublicMethod
        .sendcoin(nonexistentAddress, 1000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String txid = "";
  String num = "\"" + Base58.encode58Check(nonexistentAddress) + "\"";
    txid = PublicMethod.triggerContract(contractAddress2,
        "testSuicideNonexistentTarget(address)", num, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
  Long fee = infoById.get().getFee();
  Long netUsed = infoById.get().getReceipt().getNetUsage();
  final Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
  final Long netFee = infoById.get().getReceipt().getNetFee();
    energyUsageTotal2 = infoById.get().getReceipt().getEnergyUsageTotal();
  Long testNetAccountCountAfter = PublicMethod
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
  Long contractAccountCountAfter = PublicMethod
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal2);
    logger.info("testNetAccountCountAfter:" + testNetAccountCountAfter);
    logger.info("contractAccountCountAfter:" + contractAccountCountAfter);

    Account infoafter = PublicMethod.queryAccount(contractExcKey, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull1);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    Account contractafter = PublicMethod.queryAccount(contractAddress2, blockingStubFull1);
    long contractBalance = contractafter.getBalance();
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(contractBalance, 0);

    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Assert.assertEquals(testNetAccountCountBefore, testNetAccountCountAfter);
    Assert.assertEquals(0L, contractAccountCountAfter.longValue());
    Assert.assertEquals(energyUsageTotal, energyUsageTotal2 + 25000);

    Account nonexistentAddressAccountTrxBalance = PublicMethod
        .queryAccount(nonexistentAddress, blockingStubFull1);
    Assert.assertEquals(3000000L, nonexistentAddressAccountTrxBalance.getBalance());
  Long nonexistentAddressAccountTokenBalance = PublicMethod
        .getAssetIssueValue(nonexistentAddress, assetAccountId, blockingStubFull1);
    Assert.assertEquals(2000L, nonexistentAddressAccountTokenBalance.longValue());

  }

  @Test(enabled = true, description = "Suicide nonexistent target, but revert", groups = {"contract", "daily"})
  public void test3SuicideNonexistentTargetRevert() {
    Assert.assertTrue(PublicMethod
        .sendcoin(contractExcAddress, 10000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
  String filePath = "src/test/resources/soliditycode/TransferFailed001.sol";
  String contractName = "EnergyOfTransferFailedTest";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();

    contractAddress3 = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  //Assert.assertTrue(PublicMethod
    //.sendcoin(contractAddress, 1000000L, testNetAccountAddress, testNetAccountKey,
    //        blockingStubFull));
  //PublicMethod.waitProduceNextBlock(blockingStubFull);
    Account info;

    AccountResourceMessage resourceInfo = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] nonexistentAddress1 = ecKey2.getAddress();
  String txid = "";
  String num = "\"" + Base58.encode58Check(nonexistentAddress1) + "\"";

    txid = PublicMethod.triggerContract(contractAddress3,
        "testSuicideRevert(address)", num, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infobyid : --- " + infoById);
  Long fee = infoById.get().getFee();
  Long netUsed = infoById.get().getReceipt().getNetUsage();
  final Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
  final Long netFee = infoById.get().getReceipt().getNetFee();
    energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();
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
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertEquals(0, infoById.get().getResultValue());

    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Assert.assertTrue(energyUsageTotal < 1000000000L);

    Account nonexistentAddressAccount = PublicMethod
        .queryAccount(nonexistentAddress1, blockingStubFull1);
    Assert.assertEquals(1000000L, nonexistentAddressAccount.getBalance());
  }

  @Test(enabled = true, description = "transfer to a suicide contract address same token", groups = {"contract", "daily"})
  public void test4transferToSuicideContractSameToken() {
    Assert.assertTrue(PublicMethod
        .sendcoin(contractExcAddress, 10000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
  String filePath = "src/test/resources/soliditycode/TransferFailed001.sol";
  String contractName = "EnergyOfTransferFailedTest";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  final byte[] contractAddress4 = PublicMethod.deployContract(contractName, abi, code, "",
        maxFeeLimit, 1000000L, 100, 1000000000L,
        assetAccountId.toStringUtf8(), 1000L,null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    AccountResourceMessage resourceInfo = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull);
    Account info = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
  String txid = "";
  String num = "1" + ",\"" + Base58.encode58Check(contractAddress) + "\"";

    txid = PublicMethod.triggerContract(contractAddress4,
        "testTransferTrxNonexistentTarget(uint256,address)", num, false,
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
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("infoById:" + infoById);
    if(PublicMethod.allowTvmSelfdestructRestrictionIsActive(blockingStubFull)) {
      Assert.assertEquals(contractResult.REVERT, infoById.get().getReceipt().getResult());
      Account nonexistentAddressAccount = PublicMethod
              .queryAccount(contractAddress, blockingStubFull1);
      Assert.assertEquals(0L, nonexistentAddressAccount.getBalance());

      num =
              "\"1" + "\",\"" + Base58.encode58Check(contractAddress) + "\",\"" + assetAccountId
                      .toStringUtf8() + "\"";
      txid = PublicMethod.triggerContract(contractAddress4,
              "testTransferTokenNonexistentTarget(uint256,address,trcToken)", num, false,
              0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
      PublicMethod.waitProduceNextBlock(blockingStubFull);
      infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
      Assert.assertEquals(contractResult.REVERT, infoById.get().getReceipt().getResult());

      Long contractAccountCountAfter = PublicMethod
              .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
      Assert.assertEquals(0L, contractAccountCountAfter.longValue());
    }else {
      Assert.assertEquals(0, infoById.get().getResultValue());
      Assert.assertEquals(contractResult.SUCCESS, infoById.get().getReceipt().getResult());

      Assert.assertTrue(afterBalance + fee == beforeBalance);
      Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
      Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
      Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
      Assert.assertNotEquals(10000000, energyUsageTotal);

      Account nonexistentAddressAccount = PublicMethod
              .queryAccount(contractAddress, blockingStubFull1);
      Assert.assertEquals(1L, nonexistentAddressAccount.getBalance());

      num =
              "\"1" + "\",\"" + Base58.encode58Check(contractAddress) + "\",\"" + assetAccountId
                      .toStringUtf8() + "\"";
      txid = PublicMethod.triggerContract(contractAddress4,
              "testTransferTokenNonexistentTarget(uint256,address,trcToken)", num, false,
              0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
      PublicMethod.waitProduceNextBlock(blockingStubFull);
      infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
      Assert.assertEquals(0, infoById.get().getResultValue());

      Long contractAccountCountAfter = PublicMethod
              .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
      Assert.assertEquals(1L, contractAccountCountAfter.longValue());
    }
  }

  @Test(enabled = true, description = "transfer to a suicide contract address different token", groups = {"contract", "daily"})
  public void test5transferToSuicideContractDifferentToken() {
    // create different token
    Assert.assertTrue(PublicMethod
        .sendcoin(contractExcAddress3, 10000_000_000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    long start = System.currentTimeMillis() + 2000;
    long end = System.currentTimeMillis() + 1000000000;
  //Create a new AssetIssue success.
    Assert
        .assertTrue(PublicMethod.createAssetIssue(contractExcAddress3, tokenName, TotalSupply, 1,
            10000, start, end, 1, description, url, 100000L,
            100000L, 1L, 1L, contractExcKey3, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    assetAccountId2 = PublicMethod.queryAccount(contractExcAddress3, blockingStubFull)
        .getAssetIssuedID();

    Assert.assertTrue(PublicMethod
        .sendcoin(contractExcAddress3, 10000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
  String filePath = "src/test/resources/soliditycode/TransferFailed001.sol";
  String contractName = "EnergyOfTransferFailedTest";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  final byte[] contractAddress4 = PublicMethod.deployContract(contractName, abi, code, "",
        maxFeeLimit, 1000000L, 100, 1000000000L,
        assetAccountId2.toStringUtf8(), 1000L,null, contractExcKey3,
        contractExcAddress3, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account info;
    AccountResourceMessage resourceInfo = PublicMethod.getAccountResource(contractExcAddress3,
        blockingStubFull);
    info = PublicMethod.queryAccount(contractExcKey3, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    if (PublicMethod.allowTvmSelfdestructRestrictionIsActive(blockingStubFull)) {
      Account nonexistentAddressAccount = PublicMethod
              .queryAccount(contractAddress, blockingStubFull1);
      Assert.assertEquals(0L, nonexistentAddressAccount.getBalance());

      String num =
              "\"1" + "\",\"" + Base58.encode58Check(contractAddress) + "\",\"" + assetAccountId2
                      .toStringUtf8() + "\"";
      String txid = PublicMethod.triggerContract(contractAddress4,
              "testTransferTokenNonexistentTarget(uint256,address,trcToken)", num, false,
              0, maxFeeLimit, contractExcAddress3, contractExcKey3, blockingStubFull);
      PublicMethod.waitProduceNextBlock(blockingStubFull);
      Optional<TransactionInfo> infoById = PublicMethod
              .getTransactionInfoById(txid, blockingStubFull);
      Assert.assertEquals(contractResult.REVERT, infoById.get().getReceipt().getResult());

      Long contractAccountCountTokenBalance = PublicMethod
              .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
      Assert.assertEquals(0L, contractAccountCountTokenBalance.longValue());
      Long contractAccountCountTokenBalance2 = PublicMethod
              .getAssetIssueValue(contractAddress, assetAccountId2, blockingStubFull);
      Assert.assertEquals(0L, contractAccountCountTokenBalance2.longValue());

      String assetIssueId = PublicMethod.queryAccount(contractAddress, blockingStubFull)
              .getAssetIssuedID().toStringUtf8();
      logger.info("assetIssueId: " + assetIssueId);
      Assert.assertEquals(0, assetIssueId.length());
    }else {
      Account nonexistentAddressAccount = PublicMethod
              .queryAccount(contractAddress, blockingStubFull1);
      Assert.assertEquals(1L, nonexistentAddressAccount.getBalance());

      String num =
              "\"1" + "\",\"" + Base58.encode58Check(contractAddress) + "\",\"" + assetAccountId2
                      .toStringUtf8() + "\"";
      String txid = PublicMethod.triggerContract(contractAddress4,
              "testTransferTokenNonexistentTarget(uint256,address,trcToken)", num, false,
              0, maxFeeLimit, contractExcAddress3, contractExcKey3, blockingStubFull);
      PublicMethod.waitProduceNextBlock(blockingStubFull);
      Optional<TransactionInfo> infoById = PublicMethod
              .getTransactionInfoById(txid, blockingStubFull);
      Assert.assertEquals(0, infoById.get().getResultValue());

      Long contractAccountCountTokenBalance = PublicMethod
              .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
      Assert.assertEquals(1L, contractAccountCountTokenBalance.longValue());
      Long contractAccountCountTokenBalance2 = PublicMethod
              .getAssetIssueValue(contractAddress, assetAccountId2, blockingStubFull);
      Assert.assertEquals(1L, contractAccountCountTokenBalance2.longValue());

      String assetIssueId = PublicMethod.queryAccount(contractAddress, blockingStubFull)
              .getAssetIssuedID().toStringUtf8();
      logger.info("assetIssueId: " + assetIssueId);
      Assert.assertEquals(0, assetIssueId.length());
    }
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
