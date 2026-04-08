package stest.tron.wallet.dailybuild.zentoken;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.DecryptNotes;
import org.tron.api.GrpcAPI.Note;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.ShieldAddressInfo;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class WalletTestZenToken005 extends TronBaseTest {  List<Note> shieldOutList = new ArrayList<>();
  DecryptNotes notes;
  Note note;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] zenTokenOwnerAddress = ecKey1.getAddress();
  String zenTokenOwnerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] receiverPublicAddress = ecKey2.getAddress();
  String receiverPublicKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  private String foundationZenTokenKey = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTokenOwnerKey");
  byte[] foundationZenTokenAddress = PublicMethod.getFinalAddress(foundationZenTokenKey);
  private String zenTokenId = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTokenId");
  private byte[] tokenId = zenTokenId.getBytes();
  private Long zenTokenFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.zenTokenFee");
  private Long costTokenAmount = 10 * zenTokenFee;

  /**
   * constructor.
   */


  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(foundationZenTokenKey);
    PublicMethod.printAddress(zenTokenOwnerKey);    Assert.assertTrue(PublicMethod.transferAsset(zenTokenOwnerAddress, tokenId,
        costTokenAmount, foundationZenTokenAddress, foundationZenTokenKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.sendcoin(receiverPublicAddress, 1000000L,
        foundationAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  //Args.setFullNodeAllowShieldedTransaction(true);
  }

  @Test(enabled = false,
      description = "The receiver shield address can't more then 2", groups = {"daily", "shield"})
  public void test1ReceiverShieldAddressCanNotMoreThenTwo() {
    Optional<ShieldAddressInfo> shieldAddressInfo1 = PublicMethod.generateShieldAddress();
  String shieldAddress1 = shieldAddressInfo1.get().getAddress();
    Optional<ShieldAddressInfo> shieldAddressInfo2 = PublicMethod.generateShieldAddress();
  String shieldAddress2 = shieldAddressInfo2.get().getAddress();
    Optional<ShieldAddressInfo> shieldAddressInfo3 = PublicMethod.generateShieldAddress();
  String shieldAddress3 = shieldAddressInfo3.get().getAddress();
    logger.info("shieldAddress1:" + shieldAddress1);
    logger.info("shieldAddress2:" + shieldAddress2);
    logger.info("shieldAddress3:" + shieldAddress3);
  Long sendToShiledAddress1Amount = 3 * zenTokenFee;
  Long sendToShiledAddress2Amount = 2 * zenTokenFee;
  Long sendToShiledAddress3Amount = costTokenAmount - sendToShiledAddress1Amount
        - sendToShiledAddress2Amount - zenTokenFee;
  String memo1 = "Shield to  shield address1 transaction";
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, shieldAddress1,
        "" + sendToShiledAddress1Amount, memo1);
  String memo2 = "Shield to  shield address2 transaction";
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, shieldAddress2,
        "" + sendToShiledAddress2Amount, memo2);
  String memo3 = "Shield to  shield address3 transaction";
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, shieldAddress3,
        "" + sendToShiledAddress3Amount, memo3);

    Assert.assertFalse(PublicMethod.sendShieldCoin(
        zenTokenOwnerAddress, costTokenAmount,
        null, null,
        shieldOutList,
        null, 0,
        zenTokenOwnerKey, blockingStubFull));
  }

  @Test(enabled = false,
      description = "The receiver can't only one public address", groups = {"daily", "shield"})
  public void test2ReceiverPublicCanNotOnlyOnePublic() {
    Assert.assertTrue(PublicMethod.transferAsset(zenTokenOwnerAddress, tokenId,
        costTokenAmount, foundationZenTokenAddress, foundationZenTokenKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    shieldOutList.clear();
    Assert.assertFalse(PublicMethod.sendShieldCoin(
        zenTokenOwnerAddress, costTokenAmount,
        null, null,
        shieldOutList,
        receiverPublicAddress, costTokenAmount - zenTokenFee,
        zenTokenOwnerKey, blockingStubFull));
  }

  @Test(enabled = false,
      description = "Public send amount must equal receiver amount + shieldFee", groups = {"daily", "shield"})
  public void test3SendAmountMustEqualReceiverAmountPlusShieldFee() {
    Assert.assertTrue(PublicMethod.transferAsset(zenTokenOwnerAddress, tokenId,
        costTokenAmount, foundationZenTokenAddress, foundationZenTokenKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<ShieldAddressInfo> shieldAddressInfo1 = PublicMethod.generateShieldAddress();
  String shieldAddress1 = shieldAddressInfo1.get().getAddress();
    Optional<ShieldAddressInfo> shieldAddressInfo2 = PublicMethod.generateShieldAddress();
  String shieldAddress2 = shieldAddressInfo2.get().getAddress();
    logger.info("shieldAddress1:" + shieldAddress1);
    logger.info("shieldAddress2:" + shieldAddress2);
  Long sendToShiledAddress1Amount = 1 * zenTokenFee;
  Long sendToShiledAddress2Amount = 2 * zenTokenFee;

    shieldOutList.clear();
  String memo1 = "Public to  shield address1 transaction";
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, shieldAddress1,
        "" + sendToShiledAddress1Amount, memo1);
  String memo2 = "Public to  shield address2 transaction";
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, shieldAddress2,
        "" + sendToShiledAddress2Amount, memo2);
  //Public receiver amount is wrong
    Long sendToPublicAddressAmount = costTokenAmount - sendToShiledAddress1Amount
        - sendToShiledAddress2Amount - zenTokenFee;
    Assert.assertFalse(PublicMethod.sendShieldCoin(
        zenTokenOwnerAddress, costTokenAmount,
        null, null,
        shieldOutList,
        receiverPublicAddress, sendToPublicAddressAmount - 1,
        zenTokenOwnerKey, blockingStubFull));
  //Shield receiver amount is wrong
    sendToShiledAddress1Amount = 1 * zenTokenFee;
    sendToShiledAddress2Amount = 2 * zenTokenFee;
    sendToPublicAddressAmount = costTokenAmount - sendToShiledAddress1Amount
        - sendToShiledAddress2Amount - zenTokenFee;
    shieldOutList.clear();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, shieldAddress1,
        "" + (sendToShiledAddress1Amount - 1), memo1);
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, shieldAddress2,
        "" + sendToShiledAddress2Amount, memo2);
    Assert.assertFalse(PublicMethod.sendShieldCoin(
        zenTokenOwnerAddress, costTokenAmount,
        null, null,
        shieldOutList,
        receiverPublicAddress, sendToPublicAddressAmount,
        zenTokenOwnerKey, blockingStubFull));

    sendToShiledAddress1Amount = 1 * zenTokenFee;
    sendToShiledAddress2Amount = 2 * zenTokenFee;
    sendToPublicAddressAmount = costTokenAmount - sendToShiledAddress1Amount
        - sendToShiledAddress2Amount - zenTokenFee;
    shieldOutList.clear();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, shieldAddress1,
        "" + sendToShiledAddress1Amount, memo1);
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, shieldAddress2,
        "" + sendToShiledAddress2Amount, memo2);
    Assert.assertTrue(PublicMethod.sendShieldCoin(
        zenTokenOwnerAddress, costTokenAmount,
        null, null,
        shieldOutList,
        receiverPublicAddress, sendToPublicAddressAmount,
        zenTokenOwnerKey, blockingStubFull));
  }

  /**
   * constructor.
   */

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    PublicMethod.transferAsset(foundationZenTokenAddress, tokenId,
        PublicMethod.getAssetIssueValue(zenTokenOwnerAddress,
            PublicMethod.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
            blockingStubFull), zenTokenOwnerAddress, zenTokenOwnerKey, blockingStubFull);  }
}