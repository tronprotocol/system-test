package stest.tron.wallet.dailybuild.assetissue;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountNetMessage;
import org.tron.protos.Protocol.Account;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class WalletTestAssetIssue015 extends TronBaseTest {

  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = now;
  private static final long sendAmount = 10000000000L;
  private static final long netCostMeasure = 200L;
  private static String name = "AssetIssue015_" + Long.toString(now);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethod.getFinalAddress(testKey003);
  Long freeAssetNetLimit = 30000L;
  Long publicFreeAssetNetLimit = 30000L;
  String description = "for case assetissue015";
  String url = "https://stest.assetissue015.url";
  ByteString assetAccountId;
  //get account
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] asset015Address = ecKey1.getAddress();
  String testKeyForAssetIssue015 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] transferAssetAddress = ecKey2.getAddress();
  String transferAssetCreateKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] newAddress = ecKey3.getAddress();
  String testKeyForNewAddress = ByteArray.toHexString(ecKey3.getPrivKeyBytes());

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    logger.info(testKeyForAssetIssue015);
    logger.info(transferAssetCreateKey);
    logger.info(testKeyForNewAddress);  }

  @Test(enabled = true, description = "Use transfer net when token owner has not enough bandwidth", groups = {"daily"})
  public void atestWhenCreatorHasNoEnoughBandwidthUseTransferNet() {
    ecKey1 = new ECKey(Utils.getRandom());
    asset015Address = ecKey1.getAddress();
    testKeyForAssetIssue015 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    ecKey2 = new ECKey(Utils.getRandom());
    transferAssetAddress = ecKey2.getAddress();
    transferAssetCreateKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    ecKey3 = new ECKey(Utils.getRandom());
    newAddress = ecKey3.getAddress();
    testKeyForNewAddress = ByteArray.toHexString(ecKey3.getPrivKeyBytes());

    Assert.assertTrue(PublicMethod
        .sendcoin(asset015Address, sendAmount, foundationAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long start = System.currentTimeMillis() + 2000;
  Long end = System.currentTimeMillis() + 1000000000;
    Assert.assertTrue(PublicMethod
        .createAssetIssue(asset015Address, name, totalSupply, 1, 1, start, end, 1, description,
            url, freeAssetNetLimit, publicFreeAssetNetLimit, 1L, 1L, testKeyForAssetIssue015,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account getAssetIdFromThisAccount;
    getAssetIdFromThisAccount = PublicMethod.queryAccount(asset015Address, blockingStubFull);
    assetAccountId = getAssetIdFromThisAccount.getAssetIssuedID();
  //Transfer asset to an account.
    Assert.assertTrue(PublicMethod
        .transferAsset(transferAssetAddress, assetAccountId.toByteArray(), 10000000L,
            asset015Address, testKeyForAssetIssue015, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  //Before use transfer net, query the net used from creator and transfer.
    AccountNetMessage assetCreatorNet = PublicMethod
        .getAccountNet(asset015Address, blockingStubFull);
    AccountNetMessage assetTransferNet = PublicMethod
        .getAccountNet(transferAssetAddress, blockingStubFull);
  Long creatorBeforeFreeNetUsed = assetCreatorNet.getFreeNetUsed();
  Long transferBeforeFreeNetUsed = assetTransferNet.getFreeNetUsed();
    logger.info(Long.toString(creatorBeforeFreeNetUsed));
    logger.info(Long.toString(transferBeforeFreeNetUsed));
  //Transfer send some asset issue to default account, to test if this
    // transaction use the transaction free net.
    Assert.assertTrue(PublicMethod.transferAsset(toAddress, assetAccountId.toByteArray(), 1L,
        transferAssetAddress, transferAssetCreateKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    assetCreatorNet = PublicMethod
        .getAccountNet(asset015Address, blockingStubFull);
    assetTransferNet = PublicMethod
        .getAccountNet(transferAssetAddress, blockingStubFull);
  Long creatorAfterFreeNetUsed = assetCreatorNet.getFreeNetUsed();
  Long transferAfterFreeNetUsed = assetTransferNet.getFreeNetUsed();
    logger.info(Long.toString(creatorAfterFreeNetUsed));
    logger.info(Long.toString(transferAfterFreeNetUsed));

    Assert.assertTrue(creatorAfterFreeNetUsed - creatorBeforeFreeNetUsed < netCostMeasure);
    Assert.assertTrue(transferAfterFreeNetUsed - transferBeforeFreeNetUsed > netCostMeasure);
  }

  @Test(enabled = true, description = "Use balance when transfer has not enough net", groups = {"daily"})
  public void btestWhenTransferHasNoEnoughBandwidthUseBalance() {
    Integer i = 0;
    AccountNetMessage assetTransferNet = PublicMethod
        .getAccountNet(transferAssetAddress, blockingStubFull);
    while (assetTransferNet.getNetUsed() < 1300 && i++ < 10) {
      PublicMethod.transferAsset(toAddress, assetAccountId.toByteArray(), 1L,
          transferAssetAddress, transferAssetCreateKey, blockingStubFull);
      assetTransferNet = PublicMethod
          .getAccountNet(transferAssetAddress, blockingStubFull);
    }

    logger.info(Long.toString(assetTransferNet.getFreeNetUsed()));
    Assert.assertTrue(assetTransferNet.getFreeNetUsed() >= 1300);

    Assert.assertTrue(PublicMethod.sendcoin(transferAssetAddress,
        20000000, foundationAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account transferAccount = PublicMethod.queryAccount(transferAssetCreateKey, blockingStubFull);
  Long beforeBalance = transferAccount.getBalance();
    logger.info(Long.toString(beforeBalance));

    Assert.assertTrue(PublicMethod.transferAsset(toAddress, assetAccountId.toByteArray(), 1L,
        transferAssetAddress, transferAssetCreateKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    transferAccount = PublicMethod.queryAccount(transferAssetCreateKey, blockingStubFull);
  Long afterBalance = transferAccount.getBalance();
    logger.info(Long.toString(afterBalance));

    Assert.assertTrue(beforeBalance - afterBalance > 2000);
  }

  @Test(enabled = true, description = "Transfer asset use bandwidth when freeze balance", groups = {"daily"})
  public void ctestWhenFreezeBalanceUseNet() {
    Assert.assertTrue(PublicMethod.freezeBalance(transferAssetAddress, 5000000,
        3, transferAssetCreateKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    AccountNetMessage assetTransferNet = PublicMethod
        .getAccountNet(transferAssetAddress, blockingStubFull);
    Account transferAccount = PublicMethod.queryAccount(transferAssetCreateKey, blockingStubFull);
  final Long transferNetUsedBefore = assetTransferNet.getNetUsed();
  final Long transferBalanceBefore = transferAccount.getBalance();
    logger.info("before  " + Long.toString(transferBalanceBefore));

    Assert.assertTrue(PublicMethod.transferAsset(toAddress, assetAccountId.toByteArray(), 1L,
        transferAssetAddress, transferAssetCreateKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    assetTransferNet = PublicMethod
        .getAccountNet(transferAssetAddress, blockingStubFull);
    transferAccount = PublicMethod.queryAccount(transferAssetCreateKey, blockingStubFull);
  final Long transferNetUsedAfter = assetTransferNet.getNetUsed();
  final Long transferBalanceAfter = transferAccount.getBalance();
    logger.info("after " + Long.toString(transferBalanceAfter));

    Assert.assertTrue(transferBalanceAfter - transferBalanceBefore == 0);
    Assert.assertTrue(transferNetUsedAfter - transferNetUsedBefore > 200);


  }


  /**
   * constructor.
   */

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    PublicMethod
        .freeResource(asset015Address, testKeyForAssetIssue015, foundationAddress, blockingStubFull);
    PublicMethod
        .freeResource(transferAssetAddress, transferAssetCreateKey, foundationAddress, blockingStubFull);  }
}


