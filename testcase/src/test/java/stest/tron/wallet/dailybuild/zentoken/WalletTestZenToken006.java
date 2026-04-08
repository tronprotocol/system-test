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
public class WalletTestZenToken006 extends TronBaseTest {  Optional<ShieldAddressInfo> shieldAddressInfo;
  String shieldAddress;
  List<Note> shieldOutList = new ArrayList<>();
  List<Long> shieldInputList = new ArrayList<>();
  DecryptNotes notes;
  String memo;
  Note note;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] zenTokenOwnerAddress = ecKey1.getAddress();
  String zenTokenOwnerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private String foundationZenTokenKey = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTokenOwnerKey");
  byte[] foundationZenTokenAddress = PublicMethod.getFinalAddress(foundationZenTokenKey);
  private String zenTokenId = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTokenId");
  private byte[] tokenId = zenTokenId.getBytes();
  private Long zenTokenFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.zenTokenFee");
  private Long costTokenAmount = 10 * zenTokenFee;
  private Long sendTokenAmount = 3 * zenTokenFee;

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
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  //Args.setFullNodeAllowShieldedTransaction(true);
  }

  @Test(enabled = false, description = "Shield note memo is one char", groups = {"daily", "shield"})
  public void test1ShieldMemoIsOneChar() {
    shieldAddressInfo = PublicMethod.generateShieldAddress();
    shieldAddress = shieldAddressInfo.get().getAddress();
    logger.info("shieldAddress:" + shieldAddress);
  //One char.
    memo = ".";
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, shieldAddress,
        "" + zenTokenFee, memo);
    Assert.assertTrue(PublicMethod.sendShieldCoin(
        zenTokenOwnerAddress, zenTokenFee * 2,
        null, null,
        shieldOutList,
        null, 0,
        zenTokenOwnerKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    notes = PublicMethod.listShieldNote(shieldAddressInfo, blockingStubFull);
    note = notes.getNoteTxs(0).getNote();
  Long receiverShieldTokenAmount = note.getValue();
    Assert.assertEquals(receiverShieldTokenAmount, zenTokenFee);
    Assert.assertEquals(memo, PublicMethod.getMemo(note));
  }

  @Test(enabled = false, description = "Shield note memo is 512 char", groups = {"daily", "shield"})
  public void test2ShieldMemoIs512Char() {
    shieldAddressInfo = PublicMethod.generateShieldAddress();
    shieldAddress = shieldAddressInfo.get().getAddress();
    logger.info("shieldAddress:" + shieldAddress);
  //512 char.
    memo = "1234567812345678123456781234567812345678123456781234567812345678123456781234567812"
        + "345678123456781234567812345678123456781234567812345678123456781234567812345678123456"
        + "781234567812345678123456781234567812345678123456781234567812345678123456781234567812"
        + "345678123456781234567812345678123456781234567812345678123456781234567812345678123456"
        + "781234567812345678123456781234567812345678123456781234567812345678123456781234567812"
        + "345678123456781234567812345678123456781234567812345678123456781234567812345678123456"
        + "7812345678";
    shieldOutList.clear();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, shieldAddress,
        "" + zenTokenFee, memo);
    Assert.assertTrue(PublicMethod.sendShieldCoin(
        zenTokenOwnerAddress, zenTokenFee * 2,
        null, null,
        shieldOutList,
        null, 0,
        zenTokenOwnerKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    notes = PublicMethod.listShieldNote(shieldAddressInfo, blockingStubFull);
    note = notes.getNoteTxs(0).getNote();
  Long receiverShieldTokenAmount = note.getValue();
    Assert.assertEquals(receiverShieldTokenAmount, zenTokenFee);
    Assert.assertEquals(memo, PublicMethod.getMemo(note));

    Assert.assertFalse(PublicMethod.sendShieldCoin(
        zenTokenOwnerAddress, zenTokenFee * 2,
        shieldAddressInfo.get(), notes.getNoteTxs(0),
        shieldOutList,
        null, 0,
        zenTokenOwnerKey, blockingStubFull));
  }

  @Test(enabled = false, description = "Shield note memo is 514 char", groups = {"daily", "shield"})
  public void test3ShieldMemoIs513Char() {
    shieldAddressInfo = PublicMethod.generateShieldAddress();
    shieldAddress = shieldAddressInfo.get().getAddress();
    logger.info("shieldAddress:" + shieldAddress);
  //514 char.
    memo = "-1234567812345678123456781234567812345678123456781234567812345678123456781234567812"
        + "345678123456781234567812345678123456781234567812345678123456781234567812345678123456"
        + "781234567812345678123456781234567812345678123456781234567812345678123456781234567812"
        + "345678123456781234567812345678123456781234567812345678123456781234567812345678123456"
        + "781234567812345678123456781234567812345678123456781234567812345678123456781234567812"
        + "345678123456781234567812345678123456781234567812345678123456781234567812345678123456"
        + "7812345678";
    shieldOutList.clear();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, shieldAddress,
        "" + zenTokenFee, memo);
    Assert.assertTrue(PublicMethod.sendShieldCoin(
        zenTokenOwnerAddress, zenTokenFee * 2,
        null, null,
        shieldOutList,
        null, 0,
        zenTokenOwnerKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    notes = PublicMethod.listShieldNote(shieldAddressInfo, blockingStubFull);
    note = notes.getNoteTxs(0).getNote();
  Long receiverShieldTokenAmount = note.getValue();
    Assert.assertEquals(receiverShieldTokenAmount, zenTokenFee);
    logger.info(PublicMethod.getMemo(note));
    Assert.assertTrue(PublicMethod.getMemo(note).length() == 512);
    Assert.assertEquals(PublicMethod.getMemo(note), memo.substring(0, 512));
  }

  @Test(enabled = false, description = "Shield note memo is empty", groups = {"daily", "shield"})
  public void test4ShieldMemoIsEmpty() {
    shieldAddressInfo = PublicMethod.generateShieldAddress();
    shieldAddress = shieldAddressInfo.get().getAddress();
    logger.info("shieldAddress:" + shieldAddress);
  //Empty memo
    memo = "";
    shieldOutList.clear();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, shieldAddress,
        "" + zenTokenFee, memo);
    Assert.assertTrue(PublicMethod.sendShieldCoin(
        zenTokenOwnerAddress, 2 * zenTokenFee,
        null, null,
        shieldOutList,
        null, 0,
        zenTokenOwnerKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    notes = PublicMethod.listShieldNote(shieldAddressInfo, blockingStubFull);
    note = notes.getNoteTxs(0).getNote();
  Long receiverShieldTokenAmount = note.getValue();
    Assert.assertEquals(receiverShieldTokenAmount, zenTokenFee);
    Assert.assertEquals(memo, PublicMethod.getMemo(note));
  //Shield send to it self
    memo = "";
    shieldOutList.clear();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, shieldAddress,
        "0", memo);
    Assert.assertTrue(PublicMethod.sendShieldCoin(
        null, 0,
        shieldAddressInfo.get(),
        PublicMethod.listShieldNote(shieldAddressInfo, blockingStubFull).getNoteTxs(0),
        shieldOutList,
        null, 0,
        zenTokenOwnerKey, blockingStubFull));
  }


  @Test(enabled = false, description = "Shield note memo is empty", groups = {"daily", "shield"})
  public void test5ShieldMemoIsEmpty() {
    shieldAddressInfo = PublicMethod.generateShieldAddress();
    shieldAddress = shieldAddressInfo.get().getAddress();
    logger.info("shieldAddress:" + shieldAddress);

    memo = "{\n"
        + "  note {\n"
        + "    value: 49957\n"
        + "    payment_address: \"ztron1f42n7h0l3p8mlaq0d0rxdkhq"
        + "n6xuq49xhvj593wfduy24kn3xrmxfpqt8lnmh9ysnu5nzt3zgzx\"\n"
        + "    rcm: \"\\210x\\256\\211\\256v\\0344\\267\\240\\375\\377xs\\3"
        + "50\\3558^Y\\200i0$S\\312KK\\326l\\234J\\b\"\n"
        + "    memo: \"System.exit(1);\"\n"
        + "  }\n"
        + "  txid: \"\\215\\332\\304\\241\\362\\vbt\\250\\364\\353\\30"
        + "7\\'o\\275\\313ya*)\\320>\\001\\262B%\\371\\'\\005w\\354\\200\"\n"
        + "}";
    shieldOutList.clear();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, shieldAddress,
        "" + zenTokenFee, memo);
    Assert.assertTrue(PublicMethod.sendShieldCoin(
        zenTokenOwnerAddress, 2 * zenTokenFee,
        null, null,
        shieldOutList,
        null, 0,
        zenTokenOwnerKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    notes = PublicMethod.listShieldNote(shieldAddressInfo, blockingStubFull);
    note = notes.getNoteTxs(0).getNote();
  Long receiverShieldTokenAmount = note.getValue();
    Assert.assertEquals(receiverShieldTokenAmount, zenTokenFee);
    Assert.assertEquals(memo, PublicMethod.getMemo(note));


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