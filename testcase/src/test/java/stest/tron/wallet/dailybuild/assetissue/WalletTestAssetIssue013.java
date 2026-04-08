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
public class WalletTestAssetIssue013 extends TronBaseTest {

  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = now;
  private static final long sendAmount = 10000000000L;
  private static final long netCostMeasure = 200L;
  private static String name = "AssetIssue013_" + Long.toString(now);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethod.getFinalAddress(testKey003);
  Long freeAssetNetLimit = 300L;
  Long publicFreeAssetNetLimit = 3000L;
  String description = "for case assetissue013";
  String url = "https://stest.assetissue013.url";
  //get account
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] asset013Address = ecKey1.getAddress();
  String testKeyForAssetIssue013 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] transferAssetAddress = ecKey2.getAddress();
  String transferAssetCreateKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {  }

  @Test(enabled = true, description = "Use transfer net when token owner has no enough net", groups = {"daily"})
  public void testWhenNoEnoughFreeAssetNetLimitUseTransferNet() {

    //get account
    ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] asset013Address = ecKey1.getAddress();
  final String testKeyForAssetIssue013 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  final byte[] transferAssetAddress = ecKey2.getAddress();
  final String transferAssetCreateKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    logger.info(testKeyForAssetIssue013);
    logger.info(transferAssetCreateKey);

    Assert.assertTrue(PublicMethod
        .sendcoin(asset013Address, sendAmount, foundationAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethod
        .freezeBalance(asset013Address, 100000000L, 3, testKeyForAssetIssue013,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long start = System.currentTimeMillis() + 2000;
  Long end = System.currentTimeMillis() + 1000000000;
    Assert.assertTrue(PublicMethod
        .createAssetIssue(asset013Address, name, totalSupply, 1, 1, start, end, 1, description,
            url, freeAssetNetLimit, publicFreeAssetNetLimit, 1L, 1L, testKeyForAssetIssue013,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account getAssetIdFromThisAccount;
    getAssetIdFromThisAccount = PublicMethod.queryAccount(asset013Address, blockingStubFull);
    ByteString assetAccountId = getAssetIdFromThisAccount.getAssetIssuedID();
  //Transfer asset to an account.
    Assert.assertTrue(PublicMethod.transferAsset(
        transferAssetAddress, assetAccountId.toByteArray(),
        10000000L, asset013Address, testKeyForAssetIssue013, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  //Transfer send some asset issue to default account, to test if this
    // transaction use the creator net.
    Assert.assertTrue(PublicMethod.transferAsset(toAddress, assetAccountId.toByteArray(), 1L,
        transferAssetAddress, transferAssetCreateKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  //Before use transfer net, query the net used from creator and transfer.
    AccountNetMessage assetCreatorNet = PublicMethod
        .getAccountNet(asset013Address, blockingStubFull);
    AccountNetMessage assetTransferNet = PublicMethod
        .getAccountNet(transferAssetAddress, blockingStubFull);
  Long creatorBeforeNetUsed = assetCreatorNet.getNetUsed();
  Long transferBeforeFreeNetUsed = assetTransferNet.getFreeNetUsed();
    logger.info(Long.toString(creatorBeforeNetUsed));
    logger.info(Long.toString(transferBeforeFreeNetUsed));
  //Transfer send some asset issue to default account, to test if this
    // transaction use the transaction free net.
    Assert.assertTrue(PublicMethod.transferAsset(toAddress, assetAccountId.toByteArray(), 1L,
        transferAssetAddress, transferAssetCreateKey, blockingStubFull));
    assetCreatorNet = PublicMethod
        .getAccountNet(asset013Address, blockingStubFull);
    assetTransferNet = PublicMethod
        .getAccountNet(transferAssetAddress, blockingStubFull);
  Long creatorAfterNetUsed = assetCreatorNet.getNetUsed();
  Long transferAfterFreeNetUsed = assetTransferNet.getFreeNetUsed();
    logger.info(Long.toString(creatorAfterNetUsed));
    logger.info(Long.toString(transferAfterFreeNetUsed));

    Assert.assertTrue(creatorAfterNetUsed - creatorBeforeNetUsed < netCostMeasure);
    Assert.assertTrue(transferAfterFreeNetUsed - transferBeforeFreeNetUsed > netCostMeasure);

    PublicMethod
        .freeResource(asset013Address, testKeyForAssetIssue013, foundationAddress, blockingStubFull);
    PublicMethod.unFreezeBalance(asset013Address, testKeyForAssetIssue013, 0, asset013Address,
        blockingStubFull);
  }

  /**
   * constructor.
   */

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {  }
}


