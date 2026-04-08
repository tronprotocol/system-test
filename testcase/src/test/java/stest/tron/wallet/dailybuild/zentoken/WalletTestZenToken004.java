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
public class WalletTestZenToken004 extends TronBaseTest {  List<Note> shieldOutList = new ArrayList<>();
  DecryptNotes notes;
  Note note;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] zenTokenOwnerAddress = ecKey1.getAddress();
  String zenTokenOwnerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] receiverPublicAddress = ecKey2.getAddress();
  String receiverPublicKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  Optional<ShieldAddressInfo> sendShieldAddressInfo;
  String sendshieldAddress;
  private String foundationZenTokenKey = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTokenOwnerKey");
  byte[] foundationZenTokenAddress = PublicMethod.getFinalAddress(foundationZenTokenKey);
  private String zenTokenId = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTokenId");
  private byte[] tokenId = zenTokenId.getBytes();
  private Long zenTokenFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.zenTokenFee");
  private Long costTokenAmount = 20 * zenTokenFee;
  private Long zenTokenWhenCreateNewAddress = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.zenTokenWhenCreateNewAddress");

  /**
   * constructor.
   */
  

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(foundationZenTokenKey);
    PublicMethod.printAddress(zenTokenOwnerKey);
  //Args.setFullNodeAllowShieldedTransaction(true);
    Assert.assertTrue(PublicMethod.sendcoin(receiverPublicAddress, 1000000L,
        foundationAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = false, description = "Shield to two shield transaction", groups = {"daily", "shield"})
  public void test1Shield2TwoShieldTransaction() {
    sendShieldAddressInfo = PublicMethod.generateShieldAddress();
    sendshieldAddress = sendShieldAddressInfo.get().getAddress();
  String memo = "Use to TestZenToken004 shield address";
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, sendshieldAddress,
        "" + costTokenAmount, memo);
    Assert.assertTrue(PublicMethod.sendShieldCoin(
        foundationZenTokenAddress, costTokenAmount + zenTokenFee,
        null, null,
        shieldOutList,
        null, 0,
        foundationZenTokenKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    notes = PublicMethod.listShieldNote(sendShieldAddressInfo, blockingStubFull);

    Optional<ShieldAddressInfo> shieldAddressInfo1 = PublicMethod.generateShieldAddress();
  String shieldAddress1 = shieldAddressInfo1.get().getAddress();
    Optional<ShieldAddressInfo> shieldAddressInfo2 = PublicMethod.generateShieldAddress();
  String shieldAddress2 = shieldAddressInfo2.get().getAddress();
    logger.info("shieldAddress1:" + shieldAddress1);
    logger.info("shieldAddress2:" + shieldAddress2);
  Long sendToShiledAddress1Amount = 3 * zenTokenFee;
  Long sendToShiledAddress2Amount = costTokenAmount - sendToShiledAddress1Amount - zenTokenFee;
  String memo1 = "Shield to  shield address1 transaction";
    shieldOutList.clear();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, shieldAddress1,
        "" + sendToShiledAddress1Amount, memo1);
  String memo2 = "Shield to  shield address2 transaction";
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, shieldAddress2,
        "" + sendToShiledAddress2Amount, memo2);

    Assert.assertTrue(PublicMethod.sendShieldCoin(
        null, 0,
        sendShieldAddressInfo.get(), notes.getNoteTxs(0),
        shieldOutList,
        null, 0,
        zenTokenOwnerKey, blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    notes = PublicMethod.listShieldNote(sendShieldAddressInfo, blockingStubFull);
    Assert.assertTrue(PublicMethod.getSpendResult(sendShieldAddressInfo.get(),
        notes.getNoteTxs(0), blockingStubFull).getResult());

    notes = PublicMethod.listShieldNote(shieldAddressInfo1, blockingStubFull);
    note = notes.getNoteTxs(0).getNote();
  Long receiverShieldTokenAmount1 = note.getValue();
    logger.info("receiverShieldTokenAmount1:" + receiverShieldTokenAmount1);
    logger.info("sendToShiledAddress1Amount:" + sendToShiledAddress1Amount);
    Assert.assertEquals(receiverShieldTokenAmount1, sendToShiledAddress1Amount);
    Assert.assertEquals(memo1, PublicMethod.getMemo(note));

    notes = PublicMethod.listShieldNote(shieldAddressInfo2, blockingStubFull);
    note = notes.getNoteTxs(0).getNote();
  Long receiverShieldTokenAmount2 = note.getValue();
    Assert.assertEquals(receiverShieldTokenAmount2, sendToShiledAddress2Amount);
    Assert.assertEquals(memo2, PublicMethod.getMemo(note));

  }

  @Test(enabled = false,
      description = "Shield to one public and one shield transaction", groups = {"daily", "shield"})
  public void test2Shield2OneShieldAndOnePublicTransaction() {
    sendShieldAddressInfo = PublicMethod.generateShieldAddress();
    sendshieldAddress = sendShieldAddressInfo.get().getAddress();
  String memo = "Use to TestZenToken004 shield address";
    shieldOutList.clear();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, sendshieldAddress,
        "" + costTokenAmount, memo);
    Assert.assertTrue(PublicMethod.sendShieldCoin(
        foundationZenTokenAddress, costTokenAmount + zenTokenFee,
        null, null,
        shieldOutList,
        null, 0,
        foundationZenTokenKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    notes = PublicMethod.listShieldNote(sendShieldAddressInfo, blockingStubFull);

    Optional<ShieldAddressInfo> shieldAddressInfo1 = PublicMethod.generateShieldAddress();
  String shieldAddress1 = shieldAddressInfo1.get().getAddress();
    logger.info("shieldAddress1:" + shieldAddress1);
  Long sendToShiledAddress1Amount = 1 * zenTokenFee;
  Long sendToPublicAddressAmount = costTokenAmount - sendToShiledAddress1Amount - zenTokenFee;
    shieldOutList.clear();
  String memo1 = "Shield to  shield address1 transaction";
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, shieldAddress1,
        "" + sendToShiledAddress1Amount, memo1);

    Assert.assertTrue(PublicMethod.sendShieldCoin(
        null, 0,
        sendShieldAddressInfo.get(), notes.getNoteTxs(0),
        shieldOutList,
        receiverPublicAddress, sendToPublicAddressAmount,
        zenTokenOwnerKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    notes = PublicMethod.listShieldNote(sendShieldAddressInfo, blockingStubFull);
    Assert.assertTrue(PublicMethod.getSpendResult(sendShieldAddressInfo.get(),
        notes.getNoteTxs(0), blockingStubFull).getResult());

    notes = PublicMethod.listShieldNote(shieldAddressInfo1, blockingStubFull);
    note = notes.getNoteTxs(0).getNote();
  Long receiverShieldTokenAmount1 = note.getValue();
    logger.info("receiverShieldTokenAmount1:" + receiverShieldTokenAmount1);
    logger.info("sendToShiledAddress1Amount:" + sendToShiledAddress1Amount);
    Assert.assertEquals(receiverShieldTokenAmount1, sendToShiledAddress1Amount);
    Assert.assertEquals(memo1, PublicMethod.getMemo(note));
  Long afterReceiverPublicAssetBalance = PublicMethod.getAssetIssueValue(receiverPublicAddress,
        PublicMethod.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
        blockingStubFull);
    Assert.assertEquals(afterReceiverPublicAssetBalance, sendToPublicAddressAmount);
  }

  @Test(enabled = false,
      description = "Shield to one public and two shield transaction", groups = {"daily", "shield"})
  public void test3Public2OneShieldAndOnePublicTransaction() {
    sendShieldAddressInfo = PublicMethod.generateShieldAddress();
    sendshieldAddress = sendShieldAddressInfo.get().getAddress();
  String memo = "Use to TestZenToken004 shield address";
    shieldOutList.clear();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, sendshieldAddress,
        "" + costTokenAmount, memo);
    Assert.assertTrue(PublicMethod.sendShieldCoin(
        foundationZenTokenAddress, costTokenAmount + zenTokenFee,
        null, null,
        shieldOutList,
        null, 0,
        foundationZenTokenKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    notes = PublicMethod.listShieldNote(sendShieldAddressInfo, blockingStubFull);

    Optional<ShieldAddressInfo> shieldAddressInfo1 = PublicMethod.generateShieldAddress();
  String shieldAddress1 = shieldAddressInfo1.get().getAddress();
    Optional<ShieldAddressInfo> shieldAddressInfo2 = PublicMethod.generateShieldAddress();
  String shieldAddress2 = shieldAddressInfo2.get().getAddress();
    logger.info("shieldAddress1:" + shieldAddress1);
    logger.info("shieldAddress2:" + shieldAddress2);
  Long sendToShiledAddress1Amount = 3 * zenTokenFee;
  Long sendToShiledAddress2Amount = 4 * zenTokenFee;
  final Long sendToPublicAddressAmount = costTokenAmount - sendToShiledAddress1Amount
        - sendToShiledAddress2Amount - zenTokenWhenCreateNewAddress;
    shieldOutList.clear();
  String memo1 = "Shield to  shield address1 transaction";
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, shieldAddress1,
        "" + sendToShiledAddress1Amount, memo1);
  String memo2 = "Shield to  shield address2 transaction";
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, shieldAddress2,
        "" + sendToShiledAddress2Amount, memo2);
  //When receiver public address don't active,the fee is 1000000
    ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] notActivePublicAddress = ecKey3.getAddress();

    Assert.assertTrue(PublicMethod.sendShieldCoin(
        null, 0,
        sendShieldAddressInfo.get(), notes.getNoteTxs(0),
        shieldOutList,
        notActivePublicAddress, sendToPublicAddressAmount,
        zenTokenOwnerKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    notes = PublicMethod.listShieldNote(sendShieldAddressInfo, blockingStubFull);
    Assert.assertTrue(PublicMethod.getSpendResult(sendShieldAddressInfo.get(),
        notes.getNoteTxs(0), blockingStubFull).getResult());

    notes = PublicMethod.listShieldNote(shieldAddressInfo1, blockingStubFull);
    note = notes.getNoteTxs(0).getNote();
  Long receiverShieldTokenAmount1 = note.getValue();
    Assert.assertEquals(receiverShieldTokenAmount1, sendToShiledAddress1Amount);
    Assert.assertEquals(memo1, PublicMethod.getMemo(note));

    notes = PublicMethod.listShieldNote(shieldAddressInfo2, blockingStubFull);
    note = notes.getNoteTxs(0).getNote();
  Long receiverShieldTokenAmount2 = note.getValue();
    Assert.assertEquals(receiverShieldTokenAmount2, sendToShiledAddress2Amount);
    Assert.assertEquals(memo2, PublicMethod.getMemo(note));
  final Long afterNotActivePublicAssetBalance = PublicMethod
        .getAssetIssueValue(notActivePublicAddress,
            PublicMethod.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
            blockingStubFull);
    logger.info("afterNotActivePublicAssetBalance:" + afterNotActivePublicAssetBalance);
    logger.info("sendToPublicAddressAmount:" + sendToPublicAddressAmount);
    Assert.assertEquals(afterNotActivePublicAssetBalance, sendToPublicAddressAmount);
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