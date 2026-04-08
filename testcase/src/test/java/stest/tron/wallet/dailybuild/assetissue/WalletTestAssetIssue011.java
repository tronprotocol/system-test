package stest.tron.wallet.dailybuild.assetissue;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.protos.Protocol.Account;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class WalletTestAssetIssue011 extends TronBaseTest {

  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = now;
  private static final long sendAmount = 10000000000L;
  private static final String updateMostLongName = Long.toString(now) + "w234567890123456789";
  private static String name = "testAssetIssue011_" + Long.toString(now);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethod.getFinalAddress(testKey003);
  Long freeAssetNetLimit = 10000L;
  Long publicFreeAssetNetLimit = 10000L;
  String description = "just-test";
  String url = "https://github.com/tronprotocol/wallet-cli/";
  //get account
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] asset011Address = ecKey1.getAddress();
  String testKeyForAssetIssue011 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] transferAssetCreateAddress = ecKey2.getAddress();
  String transferAssetCreateKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(testKeyForAssetIssue011);
    PublicMethod.printAddress(transferAssetCreateKey);  }

  @Test(enabled = true, description = "Transfer asset to create account", groups = {"daily"})
  public void testTransferAssetCreateAccount() {
    //get account
    ecKey1 = new ECKey(Utils.getRandom());
    asset011Address = ecKey1.getAddress();
    testKeyForAssetIssue011 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    ecKey2 = new ECKey(Utils.getRandom());
    transferAssetCreateAddress = ecKey2.getAddress();
    transferAssetCreateKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    Assert.assertTrue(PublicMethod
        .sendcoin(asset011Address, sendAmount, foundationAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethod
        .freezeBalance(asset011Address, 100000000L, 3, testKeyForAssetIssue011,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long start = System.currentTimeMillis() + 2000;
  Long end = System.currentTimeMillis() + 1000000000;
    Assert.assertTrue(PublicMethod
        .createAssetIssue(asset011Address, name, totalSupply, 1, 1, start, end, 1, description,
            url, freeAssetNetLimit, publicFreeAssetNetLimit, 1L, 1L, testKeyForAssetIssue011,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account getAssetIdFromThisAccount;
    getAssetIdFromThisAccount = PublicMethod.queryAccount(asset011Address, blockingStubFull);
    ByteString assetAccountId = getAssetIdFromThisAccount.getAssetIssuedID();
  //Transfer asset to create an account.
    Assert.assertTrue(PublicMethod
        .transferAsset(transferAssetCreateAddress, assetAccountId.toByteArray(), 1L,
            asset011Address, testKeyForAssetIssue011, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Account queryTransferAssetAccount = PublicMethod
        .queryAccount(transferAssetCreateKey, blockingStubFull);
    Assert.assertTrue(queryTransferAssetAccount.getAssetV2Count() == 1);
    Assert.assertTrue(PublicMethod.updateAccount(asset011Address, Long.toString(now)
        .getBytes(), testKeyForAssetIssue011, blockingStubFull));
    Assert.assertTrue(PublicMethod.updateAccount(transferAssetCreateAddress, updateMostLongName
        .getBytes(), transferAssetCreateKey, blockingStubFull));
    queryTransferAssetAccount = PublicMethod.queryAccount(transferAssetCreateKey, blockingStubFull);
    Assert.assertFalse(queryTransferAssetAccount.getAccountName().isEmpty());
    PublicMethod
        .freeResource(asset011Address, testKeyForAssetIssue011, foundationAddress, blockingStubFull);
    PublicMethod.unFreezeBalance(asset011Address, testKeyForAssetIssue011, 0, asset011Address,
        blockingStubFull);

  }

  /**
   * constructor.
   */

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {  }
}


