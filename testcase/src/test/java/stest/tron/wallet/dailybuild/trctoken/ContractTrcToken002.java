package stest.tron.wallet.dailybuild.trctoken;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Utils;

@Slf4j
public class ContractTrcToken002 {

  private static final long now = System.currentTimeMillis();
  private static final long TotalSupply = 1000L;
  private static String tokenName = "testAssetIssue_" + Long.toString(now);
  private static ByteString assetAccountId = null;
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
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
  public void beforeClass() {

    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext()
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    PublicMethed.printAddress(dev001Key);
    PublicMethed.printAddress(user001Key);
  }

  @Test(enabled = true, description = "TriggerContract with correct tokenValue and tokenId")
  public void deployTransferTokenContract() {

    Assert.assertTrue(PublicMethed.sendcoin(dev001Address, 3100_000_000L, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(user001Address, 300_000_000L, fromAddress,
        testKey002, blockingStubFull));

    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress,
        PublicMethed.getFreezeBalanceCount(dev001Address, dev001Key, 70000L,
            blockingStubFull), 0, 1,
        ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress, 10_000_000L + PublicMethed.randomFreezeAmount.getAndAdd(1),
        0, 0, ByteString.copyFrom(dev001Address),
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    long start = System.currentTimeMillis() + 2000;
    long end = System.currentTimeMillis() + 1000000000;
    //Create a new AssetIssue success.
    Assert.assertTrue(PublicMethed.createAssetIssue(dev001Address, tokenName, TotalSupply, 1,
        10000, start, end, 1, description, url, 100000L,
        100000L, 1L, 1L, dev001Key, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    assetAccountId = PublicMethed.queryAccount(dev001Address, blockingStubFull).getAssetIssuedID();
    logger.info("The token name: " + tokenName);
    logger.info("The token ID: " + assetAccountId.toStringUtf8());

    //before deploy, check account resource
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(dev001Address,
        blockingStubFull);
    long energyLimit = accountResource.getEnergyLimit();
    long energyUsage = accountResource.getEnergyUsed();
    long balanceBefore = PublicMethed.queryAccount(dev001Key, blockingStubFull).getBalance();
    Long devAssetCountBefore = PublicMethed.getAssetIssueValue(
        dev001Address, assetAccountId, blockingStubFull);

    logger.info("before energyLimit is " + energyLimit);
    logger.info("before energyUsage is " + energyUsage);
    logger.info("before balanceBefore is " + balanceBefore);
    logger.info("before AssetId: " + assetAccountId.toStringUtf8()
        + ", devAssetCountBefore: " + devAssetCountBefore);

    String filePath = "./src/test/resources/soliditycode/contractTrcToken002.sol";
    String contractName = "tokenTest";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    String tokenId = assetAccountId.toStringUtf8();
    long tokenValue = 200;
    long callValue = 0;

    String transferTokenTxid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            maxFeeLimit, callValue, 0, 10000,
            tokenId, tokenValue, null, dev001Key,
            dev001Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(transferTokenTxid, blockingStubFull);
    logger.info("Deploy energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());

    if (transferTokenTxid == null || infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage());
    }

    transferTokenContractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethed.getContract(transferTokenContractAddress,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    accountResource = PublicMethed.getAccountResource(dev001Address, blockingStubFull);
    energyLimit = accountResource.getEnergyLimit();
    energyUsage = accountResource.getEnergyUsed();
    long balanceAfter = PublicMethed.queryAccount(dev001Key, blockingStubFull).getBalance();
    Long devAssetCountAfter = PublicMethed.getAssetIssueValue(
        dev001Address, assetAccountId, blockingStubFull);

    logger.info("after energyLimit is " + energyLimit);
    logger.info("after energyUsage is " + energyUsage);
    logger.info("after balanceAfter is " + balanceAfter);
    logger.info("after AssetId: " + assetAccountId.toStringUtf8()
        + ", devAssetCountAfter: " + devAssetCountAfter);

    Assert.assertFalse(PublicMethed.transferAsset(transferTokenContractAddress,
        assetAccountId.toByteArray(), 100L, dev001Address, dev001Key, blockingStubFull));

    Long contractAssetCount = PublicMethed.getAssetIssueValue(transferTokenContractAddress,
        assetAccountId, blockingStubFull);
    logger.info("Contract has AssetId: " + assetAccountId.toStringUtf8()
        + ", Count: " + contractAssetCount);

    Assert.assertEquals(Long.valueOf(200), Long.valueOf(devAssetCountBefore - devAssetCountAfter));
    Assert.assertEquals(Long.valueOf(200), contractAssetCount);

    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress,
        PublicMethed.getFreezeBalanceCount(user001Address, user001Key, 50000L,
            blockingStubFull), 0, 1,
        ByteString.copyFrom(user001Address), testKey002, blockingStubFull));


    Assert.assertTrue(PublicMethed.transferAsset(user001Address,
        assetAccountId.toByteArray(), 10L, dev001Address, dev001Key, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    accountResource = PublicMethed.getAccountResource(dev001Address,
        blockingStubFull);
    long devEnergyLimitBefore = accountResource.getEnergyLimit();
    long devEnergyUsageBefore = accountResource.getEnergyUsed();
    long devBalanceBefore = PublicMethed.queryAccount(dev001Address, blockingStubFull).getBalance();

    logger.info("before trigger, devEnergyLimitBefore is " + devEnergyLimitBefore);
    logger.info("before trigger, devEnergyUsageBefore is " + devEnergyUsageBefore);
    logger.info("before trigger, devBalanceBefore is " + devBalanceBefore);

    accountResource = PublicMethed.getAccountResource(user001Address, blockingStubFull);
    long userEnergyLimitBefore = accountResource.getEnergyLimit();
    long userEnergyUsageBefore = accountResource.getEnergyUsed();
    long userBalanceBefore = PublicMethed.queryAccount(user001Address, blockingStubFull)
        .getBalance();

    logger.info("before trigger, userEnergyLimitBefore is " + userEnergyLimitBefore);
    logger.info("before trigger, userEnergyUsageBefore is " + userEnergyUsageBefore);
    logger.info("before trigger, userBalanceBefore is " + userBalanceBefore);


    PublicMethed
        .sendcoin(transferTokenContractAddress, 5000000, fromAddress, testKey002, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    tokenId = assetAccountId.toStringUtf8();
    tokenValue = 10;
    callValue = 0;

    final String triggerTxid = PublicMethed.triggerContract(transferTokenContractAddress,
        "msgTokenValueAndTokenIdTest()", "#", false, callValue,
        1000000000L, tokenId, tokenValue, user001Address, user001Key,
        blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    accountResource = PublicMethed.getAccountResource(dev001Address, blockingStubFull);
    long devEnergyLimitAfter = accountResource.getEnergyLimit();
    long devEnergyUsageAfter = accountResource.getEnergyUsed();
    long devBalanceAfter = PublicMethed.queryAccount(dev001Address, blockingStubFull).getBalance();

    logger.info("after trigger, devEnergyLimitAfter is " + devEnergyLimitAfter);
    logger.info("after trigger, devEnergyUsageAfter is " + devEnergyUsageAfter);
    logger.info("after trigger, devBalanceAfter is " + devBalanceAfter);

    accountResource = PublicMethed.getAccountResource(user001Address, blockingStubFull);
    long userEnergyLimitAfter = accountResource.getEnergyLimit();
    long userEnergyUsageAfter = accountResource.getEnergyUsed();
    long userBalanceAfter = PublicMethed.queryAccount(user001Address,
        blockingStubFull).getBalance();

    logger.info("after trigger, userEnergyLimitAfter is " + userEnergyLimitAfter);
    logger.info("after trigger, userEnergyUsageAfter is " + userEnergyUsageAfter);
    logger.info("after trigger, userBalanceAfter is " + userBalanceAfter);

    infoById = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    logger.info("Trigger energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());

    if (triggerTxid == null || infoById.get().getResultValue() != 0) {
      Assert.fail("transaction failed with message: " + infoById.get().getResMessage());
    }

    List<String> retList = PublicMethed
        .getStrings(infoById.get().getContractResult(0).toByteArray());

    Long msgId = ByteArray.toLong(ByteArray.fromHexString(retList.get(0)));
    Long msgTokenValue = ByteArray.toLong(ByteArray.fromHexString(retList.get(1)));
    Long msgCallValue = ByteArray.toLong(ByteArray.fromHexString(retList.get(2)));

    logger.info("msgId: " + msgId);
    logger.info("msgTokenValue: " + msgTokenValue);
    logger.info("msgCallValue: " + msgCallValue);

    Assert.assertEquals(msgId.toString(), tokenId);
    Assert.assertEquals(Long.valueOf(msgTokenValue), Long.valueOf(tokenValue));
    Assert.assertEquals(Long.valueOf(msgCallValue), Long.valueOf(callValue));
    contractAssetCount = PublicMethed.getAssetIssueValue(transferTokenContractAddress,
            assetAccountId, blockingStubFull);
    logger.info("after user trigger Contract has AssetId: " + assetAccountId.toStringUtf8()
            + ", Count: " + contractAssetCount);

  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethed.freedResource(dev001Address, dev001Key, fromAddress, blockingStubFull);
    PublicMethed.freedResource(user001Address, user001Key, fromAddress, blockingStubFull);
    PublicMethed.unFreezeBalance(fromAddress, testKey002, 0, dev001Address, blockingStubFull);
    PublicMethed.unFreezeBalance(fromAddress, testKey002, 1, user001Address, blockingStubFull);
    PublicMethed.unFreezeBalance(fromAddress, testKey002, 1, dev001Address, blockingStubFull);

    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


