package stest.tron.wallet.dailybuild.zentoken;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.DecryptNotes;
import org.tron.api.GrpcAPI.DiversifierMessage;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.ExpandedSpendingKeyMessage;
import org.tron.api.GrpcAPI.IncomingViewingKeyDiversifierMessage;
import org.tron.api.GrpcAPI.IncomingViewingKeyMessage;
import org.tron.api.GrpcAPI.Note;
import org.tron.api.GrpcAPI.PaymentAddressMessage;
import org.tron.api.GrpcAPI.ViewingKeyMessage;
import org.tron.api.WalletSolidityGrpc;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.ShieldAddressInfo;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.zen.address.DiversifierT;

@Slf4j
public class WalletTestZenToken007 extends TronBaseTest {

  private static ByteString assetAccountId = null;  Optional<ShieldAddressInfo> sendShieldAddressInfo1;
  Optional<ShieldAddressInfo> sendShieldAddressInfo2;
  Optional<ShieldAddressInfo> sendShieldAddressInfo3;
  Optional<ShieldAddressInfo> receiverShieldAddressInfo;
  String sendShieldAddress1;
  String sendShieldAddress2;
  String sendShieldAddress3;
  String receiverShieldAddress1;
  String receiverShieldAddress2;
  String receiverShieldAddress3;
  List<Note> shieldOutList = new ArrayList<>();
  DecryptNotes notes;
  String memo1;
  String memo2;
  String memo3;
  Note sendNote1;
  Note sendNote2;
  Note sendNote3;
  Note receiverNote1;
  Note receiverNote2;
  Note receiverNote3;
  BytesMessage ak;
  BytesMessage nk;
  BytesMessage sk;
  ExpandedSpendingKeyMessage expandedSpendingKeyMessage;
  DiversifierMessage diversifierMessage1;
  DiversifierMessage diversifierMessage2;
  DiversifierMessage diversifierMessage3;
  IncomingViewingKeyMessage ivk;
  ShieldAddressInfo addressInfo1 = new ShieldAddressInfo();
  ShieldAddressInfo addressInfo2 = new ShieldAddressInfo();
  ShieldAddressInfo addressInfo3 = new ShieldAddressInfo();
  Optional<ShieldAddressInfo> receiverAddressInfo1;
  Optional<ShieldAddressInfo> receiverAddressInfo2;
  Optional<ShieldAddressInfo> receiverAddressInfo3;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] zenTokenOwnerAddress1 = ecKey1.getAddress();
  String zenTokenOwnerKey1 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] zenTokenOwnerAddress2 = ecKey2.getAddress();
  String zenTokenOwnerKey2 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] zenTokenOwnerAddress3 = ecKey3.getAddress();
  String zenTokenOwnerKey3 = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
  ECKey ecKey4 = new ECKey(Utils.getRandom());
  byte[] zenTokenOwnerAddress4 = ecKey4.getAddress();
  String zenTokenOwnerKey4 = ByteArray.toHexString(ecKey4.getPrivKeyBytes());
  private ManagedChannel channelSolidity1 = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity1 = null;
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);
  private String soliditynode1 = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(1);
  private String foundationZenTokenKey = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTokenOwnerKey");
  byte[] foundationZenTokenAddress = PublicMethod.getFinalAddress(foundationZenTokenKey);
  private String zenTokenId = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTokenId");
  private byte[] tokenId = zenTokenId.getBytes();
  private Long zenTokenFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.zenTokenFee");
  private Long costTokenAmount = 10 * zenTokenFee;
  private Long sendTokenAmount = 8 * zenTokenFee;

  /**
   * constructor.
   */
  

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    initSolidityChannel();
    PublicMethod.printAddress(foundationZenTokenKey);
    PublicMethod.printAddress(zenTokenOwnerKey1);
    PublicMethod.printAddress(zenTokenOwnerKey2);
    PublicMethod.printAddress(zenTokenOwnerKey3);
    PublicMethod.printAddress(zenTokenOwnerKey4);    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext()
        .build();
    channelSolidity1 = ManagedChannelBuilder.forTarget(soliditynode1)
        .usePlaintext()
        .build();
    blockingStubSolidity1 = WalletSolidityGrpc.newBlockingStub(channelSolidity1);

    Assert.assertTrue(PublicMethod.transferAsset(zenTokenOwnerAddress1, tokenId,
        costTokenAmount, foundationZenTokenAddress, foundationZenTokenKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.transferAsset(zenTokenOwnerAddress2, tokenId,
        costTokenAmount, foundationZenTokenAddress, foundationZenTokenKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.transferAsset(zenTokenOwnerAddress3, tokenId,
        costTokenAmount, foundationZenTokenAddress, foundationZenTokenKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.transferAsset(zenTokenOwnerAddress4, tokenId,
        costTokenAmount, foundationZenTokenAddress, foundationZenTokenKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  //Args.setFullNodeAllowShieldedTransaction(true);
    sendShieldAddressInfo1 = PublicMethod.generateShieldAddress();
    sendShieldAddressInfo2 = PublicMethod.generateShieldAddress();
    sendShieldAddressInfo3 = PublicMethod.generateShieldAddress();
    sendShieldAddress1 = sendShieldAddressInfo1.get().getAddress();
    sendShieldAddress2 = sendShieldAddressInfo2.get().getAddress();
    sendShieldAddress3 = sendShieldAddressInfo3.get().getAddress();
    logger.info("sendShieldAddressInfo1:" + sendShieldAddressInfo1);
    logger.info("sendShieldAddressInfo2:" + sendShieldAddressInfo2);
    logger.info("sendShieldAddressInfo3:" + sendShieldAddressInfo3);
    memo1 = "Shield memo1 in " + System.currentTimeMillis();
    memo2 = "Shield memo2 in " + System.currentTimeMillis();
    memo3 = "Shield memo3 in " + System.currentTimeMillis();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, sendShieldAddress1,
        "" + (sendTokenAmount - zenTokenFee), memo1);
    Assert.assertTrue(PublicMethod.sendShieldCoin(zenTokenOwnerAddress1, sendTokenAmount, null,
        null, shieldOutList, null, 0, zenTokenOwnerKey1, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    shieldOutList.clear();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, sendShieldAddress2,
        "" + (sendTokenAmount - zenTokenFee), memo2);
    Assert.assertTrue(PublicMethod.sendShieldCoin(zenTokenOwnerAddress2, sendTokenAmount, null,
        null, shieldOutList, null, 0, zenTokenOwnerKey2, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    shieldOutList.clear();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, sendShieldAddress3,
        "" + (sendTokenAmount - zenTokenFee), memo3);
    Assert.assertTrue(PublicMethod.sendShieldCoin(zenTokenOwnerAddress3, sendTokenAmount, null,
        null, shieldOutList, null, 0, zenTokenOwnerKey3, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    notes = PublicMethod.listShieldNote(sendShieldAddressInfo1, blockingStubFull);
    sendNote1 = notes.getNoteTxs(0).getNote();
    notes = PublicMethod.listShieldNote(sendShieldAddressInfo2, blockingStubFull);
    sendNote2 = notes.getNoteTxs(0).getNote();
    notes = PublicMethod.listShieldNote(sendShieldAddressInfo3, blockingStubFull);
    sendNote3 = notes.getNoteTxs(0).getNote();

  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "Get spending key", groups = {"daily", "shield"})
  public void test01GetSpendingKey() {
    sk = blockingStubFull.getSpendingKey(EmptyMessage.newBuilder().build());
    logger.info("sk: " + ByteArray.toHexString(sk.getValue().toByteArray()));

  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "Get diversifier", groups = {"daily", "shield"})
  public void test02GetDiversifier() {
    diversifierMessage1 = blockingStubFull.getDiversifier(EmptyMessage.newBuilder().build());
    logger.info("d1: " + ByteArray.toHexString(diversifierMessage1.getD().toByteArray()));
    diversifierMessage2 = blockingStubFull.getDiversifier(EmptyMessage.newBuilder().build());
    logger.info("d2: " + ByteArray.toHexString(diversifierMessage2.getD().toByteArray()));
    diversifierMessage3 = blockingStubFull.getDiversifier(EmptyMessage.newBuilder().build());
    logger.info("d3: " + ByteArray.toHexString(diversifierMessage3.getD().toByteArray()));

  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "Get expanded spending key", groups = {"daily", "shield"})
  public void test03GetExpandedSpendingKey() {
    expandedSpendingKeyMessage = blockingStubFull.getExpandedSpendingKey(sk);
    logger.info("ask: " + ByteArray.toHexString(expandedSpendingKeyMessage.getAsk().toByteArray()));
    logger.info("nsk: " + ByteArray.toHexString(expandedSpendingKeyMessage.getNsk().toByteArray()));
    logger.info("ovk: " + ByteArray.toHexString(expandedSpendingKeyMessage.getOvk().toByteArray()));

  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "Get AK from ASK", groups = {"daily", "shield"})
  public void test04GetAkFromAsk() {
    BytesMessage.Builder askBuilder = BytesMessage.newBuilder();
    askBuilder.setValue(expandedSpendingKeyMessage.getAsk());
    ak = blockingStubFull.getAkFromAsk(askBuilder.build());
    logger.info("ak: " + ByteArray.toHexString(ak.getValue().toByteArray()));
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "Get Nk from Nsk", groups = {"daily", "shield"})
  public void test05GetNkFromNsk() {
    BytesMessage.Builder nskBuilder = BytesMessage.newBuilder();
    nskBuilder.setValue(expandedSpendingKeyMessage.getNsk());
    nk = blockingStubFull.getNkFromNsk(nskBuilder.build());
    logger.info("nk: " + ByteArray.toHexString(nk.getValue().toByteArray()));
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "Get incoming viewing Key", groups = {"daily", "shield"})
  public void test06GetIncomingViewingKey() {
    ViewingKeyMessage.Builder viewBuilder = ViewingKeyMessage.newBuilder();
    viewBuilder.setAk(ak.getValue());
    viewBuilder.setNk(nk.getValue());
    ivk = blockingStubFull.getIncomingViewingKey(viewBuilder.build());
    logger.info("ivk: " + ByteArray.toHexString(ivk.getIvk().toByteArray()));
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "Get Zen Payment Address", groups = {"daily", "shield"})
  public void test07GetZenPaymentAddress() {
    IncomingViewingKeyDiversifierMessage.Builder builder =
        IncomingViewingKeyDiversifierMessage.newBuilder();
    builder.setD(diversifierMessage1);
    builder.setIvk(ivk);
    PaymentAddressMessage addressMessage = blockingStubFull.getZenPaymentAddress(builder.build());
    System.out.println("pkd1: " + ByteArray.toHexString(addressMessage.getPkD().toByteArray()));
    System.out.println("address1: " + addressMessage.getPaymentAddress());
    addressInfo1.setSk(sk.getValue().toByteArray());
    addressInfo1.setD(new DiversifierT(diversifierMessage1.getD().toByteArray()));
    addressInfo1.setIvk(ivk.getIvk().toByteArray());
    addressInfo1.setOvk(expandedSpendingKeyMessage.getOvk().toByteArray());
    addressInfo1.setPkD(addressMessage.getPkD().toByteArray());
    receiverAddressInfo1 = Optional.of(addressInfo1);

    builder.clear();
    builder = IncomingViewingKeyDiversifierMessage.newBuilder();
    builder.setD(diversifierMessage2);
    builder.setIvk(ivk);
    addressMessage = blockingStubFull.getZenPaymentAddress(builder.build());
    System.out.println("pkd2: " + ByteArray.toHexString(addressMessage.getPkD().toByteArray()));
    System.out.println("address2: " + addressMessage.getPaymentAddress());
    addressInfo2.setSk(sk.getValue().toByteArray());
    addressInfo2.setD(new DiversifierT(diversifierMessage2.getD().toByteArray()));
    addressInfo2.setIvk(ivk.getIvk().toByteArray());
    addressInfo2.setOvk(expandedSpendingKeyMessage.getOvk().toByteArray());
    addressInfo2.setPkD(addressMessage.getPkD().toByteArray());
    receiverAddressInfo2 = Optional.of(addressInfo2);

    builder.clear();
    builder = IncomingViewingKeyDiversifierMessage.newBuilder();
    builder.setD(diversifierMessage3);
    builder.setIvk(ivk);
    addressMessage = blockingStubFull.getZenPaymentAddress(builder.build());
    System.out.println("pkd3: " + ByteArray.toHexString(addressMessage.getPkD().toByteArray()));
    System.out.println("address3: " + addressMessage.getPaymentAddress());
    addressInfo3.setSk(sk.getValue().toByteArray());
    addressInfo3.setD(new DiversifierT(diversifierMessage3.getD().toByteArray()));
    addressInfo3.setIvk(ivk.getIvk().toByteArray());
    addressInfo3.setOvk(expandedSpendingKeyMessage.getOvk().toByteArray());
    addressInfo3.setPkD(addressMessage.getPkD().toByteArray());
    receiverAddressInfo3 = Optional.of(addressInfo3);


  }

  @Test(enabled = false, description = "Shield to shield transaction", groups = {"daily", "shield"})
  public void test08Shield2ShieldTransaction() {
    //S to S address1
    receiverShieldAddress1 = receiverAddressInfo1.get().getAddress();
    shieldOutList.clear();
    ;
    memo1 = "Send shield to receiver1 shield memo in" + System.currentTimeMillis();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, receiverShieldAddress1,
        "" + (sendNote1.getValue() - zenTokenFee), memo1);
    notes = PublicMethod.listShieldNote(sendShieldAddressInfo1, blockingStubFull);
    Assert.assertTrue(PublicMethod.sendShieldCoin(
        null, 0,
        sendShieldAddressInfo1.get(), notes.getNoteTxs(0),
        shieldOutList,
        null, 0,
        zenTokenOwnerKey1, blockingStubFull));
  //S to S address2
    receiverShieldAddress2 = receiverAddressInfo2.get().getAddress();
    shieldOutList.clear();
    ;
    memo2 = "Send shield2 to receiver shield memo in" + System.currentTimeMillis();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, receiverShieldAddress2,
        "" + (sendNote2.getValue() - zenTokenFee), memo2);
    notes = PublicMethod.listShieldNote(sendShieldAddressInfo2, blockingStubFull);
    Assert.assertTrue(PublicMethod.sendShieldCoin(
        null, 0,
        sendShieldAddressInfo2.get(), notes.getNoteTxs(0),
        shieldOutList,
        null, 0,
        zenTokenOwnerKey2, blockingStubFull));
  //S to S address3
    receiverShieldAddress3 = receiverAddressInfo3.get().getAddress();
    shieldOutList.clear();
    ;
    memo3 = "Send shield3 to receiver shield memo in" + System.currentTimeMillis();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, receiverShieldAddress3,
        "" + (sendNote3.getValue() - zenTokenFee), memo3);
    notes = PublicMethod.listShieldNote(sendShieldAddressInfo3, blockingStubFull);
    Assert.assertTrue(PublicMethod.sendShieldCoin(
        null, 0,
        sendShieldAddressInfo3.get(), notes.getNoteTxs(0),
        shieldOutList,
        null, 0,
        zenTokenOwnerKey3, blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  //Same sk and different d can produce different
    // shield address,the notes can scan out by same ivk.
    notes = PublicMethod.getShieldNotesByIvk(receiverAddressInfo1, blockingStubFull);
    Assert.assertTrue(notes.getNoteTxsCount() == 3);

    receiverNote1 = notes.getNoteTxs(0).getNote();
    logger.info("Receiver note1:" + receiverNote1.toString());
    Assert.assertTrue(receiverNote1.getValue() == sendNote1.getValue() - zenTokenFee);
    receiverNote2 = notes.getNoteTxs(1).getNote();
    logger.info("Receiver note2:" + receiverNote2.toString());
    Assert.assertTrue(receiverNote2.getValue() == sendNote2.getValue() - zenTokenFee);
    receiverNote3 = notes.getNoteTxs(2).getNote();
    logger.info("Receiver note3:" + receiverNote3.toString());
    Assert.assertTrue(receiverNote3.getValue() == sendNote3.getValue() - zenTokenFee);
  }

  @Test(enabled = false,
      description = "Shield to shield transaction without ask", groups = {"daily", "shield"})
  public void test09Shield2ShieldTransactionWithoutAsk() {
    //Same sk and different d can produce different shield address,
    // the notes can use by scan from same ovk.
    sendShieldAddressInfo1 = PublicMethod.generateShieldAddress();
    sendShieldAddress1 = sendShieldAddressInfo1.get().getAddress();
    sendShieldAddressInfo2 = PublicMethod.generateShieldAddress();
    sendShieldAddress2 = sendShieldAddressInfo2.get().getAddress();
    sendShieldAddressInfo3 = PublicMethod.generateShieldAddress();
    sendShieldAddress3 = sendShieldAddressInfo3.get().getAddress();

    notes = PublicMethod.getShieldNotesByIvk(receiverAddressInfo3, blockingStubFull);
    receiverNote1 = notes.getNoteTxs(0).getNote();
    receiverNote2 = notes.getNoteTxs(1).getNote();
    receiverNote3 = notes.getNoteTxs(2).getNote();
    shieldOutList.clear();
    ;
    memo1 = "Send shield address 1 without ask" + System.currentTimeMillis();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, sendShieldAddress1,
        "" + (receiverNote1.getValue() - zenTokenFee), memo1);

    Assert.assertTrue(PublicMethod.sendShieldCoinWithoutAsk(
        null, 0,
        receiverAddressInfo1.get(), notes.getNoteTxs(0),
        shieldOutList,
        null, 0,
        zenTokenOwnerKey1, blockingStubFull));

    shieldOutList.clear();
    ;
    memo2 = "Send shield address 2 without ask" + System.currentTimeMillis();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, sendShieldAddress2,
        "" + (receiverNote2.getValue() - zenTokenFee), memo2);
    Assert.assertTrue(PublicMethod.sendShieldCoinWithoutAsk(
        null, 0,
        receiverAddressInfo2.get(), notes.getNoteTxs(1),
        shieldOutList,
        null, 0,
        zenTokenOwnerKey2, blockingStubFull));

    shieldOutList.clear();
    ;
    memo3 = "Send shield address 3 without ask" + System.currentTimeMillis();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, sendShieldAddress3,
        "" + (receiverNote3.getValue() - zenTokenFee), memo3);
    Assert.assertTrue(PublicMethod.sendShieldCoin(
        null, 0,
        receiverAddressInfo3.get(), notes.getNoteTxs(2),
        shieldOutList,
        null, 0,
        zenTokenOwnerKey3, blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    notes = PublicMethod.getShieldNotesByOvk(receiverAddressInfo3, blockingStubFull);
    logger.info("notes count:" + notes.getNoteTxsCount());
    Assert.assertTrue(notes.getNoteTxsCount() == 3);
    sendNote1 = notes.getNoteTxs(0).getNote();
    logger.info("Receiver1 note:" + sendNote1.toString());
    Assert.assertTrue(sendNote1.getValue() == receiverNote1.getValue() - zenTokenFee);
    Assert.assertEquals(memo1, PublicMethod.getMemo(sendNote1));

    sendNote2 = notes.getNoteTxs(1).getNote();
    logger.info("Receiver2 note:" + sendNote2.toString());
    Assert.assertTrue(sendNote2.getValue() == receiverNote2.getValue() - zenTokenFee);
    Assert.assertEquals(memo2, PublicMethod.getMemo(sendNote2));

    sendNote3 = notes.getNoteTxs(2).getNote();
    logger.info("Receiver3 note:" + sendNote3.toString());
    Assert.assertTrue(sendNote3.getValue() == receiverNote3.getValue() - zenTokenFee);
    Assert.assertEquals(memo3, PublicMethod.getMemo(sendNote3));
  }

  @Test(enabled = false, description = "Get shield Nulltifier", groups = {"daily", "shield"})
  public void test10GetShieldNulltifier() {
    notes = PublicMethod.listShieldNote(sendShieldAddressInfo1, blockingStubFull);
    Assert.assertEquals(PublicMethod.getShieldNullifier(sendShieldAddressInfo1.get(),
        notes.getNoteTxs(0), blockingStubFull).length(), 64);
    notes = PublicMethod.listShieldNote(receiverAddressInfo1, blockingStubFull);
    Assert.assertEquals(PublicMethod.getShieldNullifier(receiverAddressInfo1.get(),
        notes.getNoteTxs(0), blockingStubFull).length(), 64);

    Assert.assertTrue(PublicMethod.getSpendResult(receiverAddressInfo1.get(),
        notes.getNoteTxs(0), blockingStubFull).getResult());
  }

  @Test(enabled = false,
      description = "Same sk transfer shield address note is spent", groups = {"daily", "shield"})
  public void test11SameSkTransferShieldAddressNoteCanSpent() {
    notes = PublicMethod.getShieldNotesByIvk(receiverAddressInfo2, blockingStubFull);

    receiverNote1 = notes.getNoteTxs(0).getNote();
    shieldOutList.clear();
    memo1 = "Send shield address 1 without ask" + System.currentTimeMillis();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, sendShieldAddress1,
        "" + (receiverNote1.getValue() - zenTokenFee), memo1);
    Assert.assertFalse(PublicMethod.sendShieldCoinWithoutAsk(
        null, 0,
        receiverAddressInfo1.get(), notes.getNoteTxs(0),
        shieldOutList,
        null, 0,
        zenTokenOwnerKey1, blockingStubFull));

    Assert.assertTrue(PublicMethod.getSpendResult(receiverAddressInfo1.get(),
        notes.getNoteTxs(0), blockingStubFull).getResult());
    Assert.assertTrue(PublicMethod.getSpendResult(receiverAddressInfo2.get(),
        notes.getNoteTxs(1), blockingStubFull).getResult());
    Assert.assertTrue(PublicMethod.getSpendResult(receiverAddressInfo3.get(),
        notes.getNoteTxs(2), blockingStubFull).getResult());
  }

  @Test(enabled = false, description = "Same sk transfer two shield address,"
      + "in one transaction send to these shield transaction", groups = {"daily", "shield"})
  public void test12SameSkTransferTwoShieldAddressInOneTransaction() {
    shieldOutList.clear();
    memo1 = "Send to first shield address " + System.currentTimeMillis();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, receiverShieldAddress1,
        "" + zenTokenFee, memo1);
    memo2 = "Send to second shield address " + System.currentTimeMillis();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, receiverShieldAddress2,
        "" + (costTokenAmount - 2 * zenTokenFee), memo2);
    logger.info("address1 receiver amount:" + zenTokenFee);
    logger.info("address2 receiver amount:" + (costTokenAmount - 2 * zenTokenFee));
    Assert.assertTrue(PublicMethod.sendShieldCoinWithoutAsk(
        zenTokenOwnerAddress4, costTokenAmount,
        null, null,
        shieldOutList,
        null, 0,
        zenTokenOwnerKey4, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    notes = PublicMethod.getShieldNotesByIvk(receiverAddressInfo2, blockingStubFull);
    Assert.assertTrue(notes.getNoteTxsCount() == 5);
    Assert.assertTrue(notes.getNoteTxs(3).getNote().getValue() == zenTokenFee);
    Assert.assertTrue(notes.getNoteTxs(4).getNote().getValue()
        == (costTokenAmount - 2 * zenTokenFee));
    Assert.assertEquals(PublicMethod.getMemo(notes.getNoteTxs(3).getNote()), memo1);
    Assert.assertEquals(PublicMethod.getMemo(notes.getNoteTxs(4).getNote()), memo2);

    shieldOutList.clear();
    ;
    receiverNote1 = notes.getNoteTxs(3).getNote();
    receiverNote2 = notes.getNoteTxs(4).getNote();
    memo1 = "Send shield address 1 without ask" + System.currentTimeMillis();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, sendShieldAddress1,
        "" + (receiverNote1.getValue() - zenTokenFee), memo1);
    Assert.assertTrue(PublicMethod.sendShieldCoinWithoutAsk(
        null, 0,
        receiverAddressInfo1.get(), notes.getNoteTxs(3),
        shieldOutList,
        null, 0,
        zenTokenOwnerKey1, blockingStubFull));

    shieldOutList.clear();
    ;
    memo2 = "Send shield address 2 without ask" + System.currentTimeMillis();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, sendShieldAddress2,
        "" + (receiverNote2.getValue() - zenTokenFee), memo2);
    Assert.assertTrue(PublicMethod.sendShieldCoinWithoutAsk(
        null, 0,
        receiverAddressInfo2.get(), notes.getNoteTxs(4),
        shieldOutList,
        null, 0,
        zenTokenOwnerKey2, blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    notes = PublicMethod.getShieldNotesByIvk(sendShieldAddressInfo1, blockingStubFull);
    sendNote1 = notes.getNoteTxs(0).getNote();
    shieldOutList.clear();
    memo2 = "Send receiver a note and spend it" + System.currentTimeMillis();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, sendShieldAddress2,
        "" + (sendNote1.getValue() - zenTokenFee), memo2);
    Assert.assertTrue(PublicMethod.sendShieldCoinWithoutAsk(
        null, 0,
        sendShieldAddressInfo1.get(), notes.getNoteTxs(0),
        shieldOutList,
        null, 0,
        zenTokenOwnerKey2, blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    notes = PublicMethod.getShieldNotesByIvk(receiverAddressInfo2, blockingStubFull);

    Assert.assertTrue(PublicMethod.getSpendResult(receiverAddressInfo1.get(),
        notes.getNoteTxs(3), blockingStubFull).getResult());
    Assert.assertTrue(PublicMethod.getSpendResult(receiverAddressInfo2.get(),
        notes.getNoteTxs(4), blockingStubFull).getResult());

    notes = PublicMethod.getShieldNotesByOvk(receiverAddressInfo1, blockingStubFull);
    Assert.assertTrue(PublicMethod.getSpendResult(sendShieldAddressInfo1.get(),
        notes.getNoteTxs(0), blockingStubFull).getResult());
    Assert.assertFalse(PublicMethod.getSpendResult(receiverAddressInfo2.get(),
        notes.getNoteTxs(1), blockingStubFull).getResult());
    Assert.assertFalse(PublicMethod.getSpendResult(receiverAddressInfo3.get(),
        notes.getNoteTxs(2), blockingStubFull).getResult());
    Assert.assertFalse(PublicMethod.getSpendResult(receiverAddressInfo1.get(),
        notes.getNoteTxs(3), blockingStubFull).getResult());
    Assert.assertFalse(PublicMethod.getSpendResult(receiverAddressInfo2.get(),
        notes.getNoteTxs(4), blockingStubFull).getResult());
  //Send shield coin without ask when there is no output shield address
    shieldOutList.clear();
    memo2 = "Send receiver a note and spend it" + System.currentTimeMillis();

    Assert.assertTrue(PublicMethod.sendShieldCoinWithoutAsk(
        null, 0,
        sendShieldAddressInfo2.get(), notes.getNoteTxs(1),
        shieldOutList,
        zenTokenOwnerAddress1, notes.getNoteTxs(1).getNote().getValue() - zenTokenFee,
        zenTokenOwnerKey2, blockingStubFull));

    shieldOutList.clear();
    memo2 = "Send receiver a note and spend it" + System.currentTimeMillis();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, sendShieldAddress2,
        "0", memo2);
    Assert.assertTrue(PublicMethod.sendShieldCoinWithoutAsk(
        null, 0,
        sendShieldAddressInfo3.get(), notes.getNoteTxs(2),
        shieldOutList,
        zenTokenOwnerAddress1, notes.getNoteTxs(2).getNote().getValue() - zenTokenFee,
        zenTokenOwnerKey2, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    notes = PublicMethod.getShieldNotesByIvk(receiverAddressInfo2, blockingStubFull);
    Assert.assertTrue(PublicMethod.getSpendResult(receiverAddressInfo2.get(),
        notes.getNoteTxs(1), blockingStubFull).getResult());
    Assert.assertTrue(PublicMethod.getSpendResult(receiverAddressInfo3.get(),
        notes.getNoteTxs(2), blockingStubFull).getResult());


  }


  /**
   * constructor.
   */
  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    PublicMethod.transferAsset(foundationZenTokenAddress, tokenId,
        PublicMethod.getAssetIssueValue(zenTokenOwnerAddress1,
            PublicMethod.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
            blockingStubFull), zenTokenOwnerAddress1, zenTokenOwnerKey1, blockingStubFull);
    PublicMethod.transferAsset(foundationZenTokenAddress, tokenId,
        PublicMethod.getAssetIssueValue(zenTokenOwnerAddress2,
            PublicMethod.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
            blockingStubFull), zenTokenOwnerAddress2, zenTokenOwnerKey2, blockingStubFull);
    PublicMethod.transferAsset(foundationZenTokenAddress, tokenId,
        PublicMethod.getAssetIssueValue(zenTokenOwnerAddress3,
            PublicMethod.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
            blockingStubFull), zenTokenOwnerAddress3, zenTokenOwnerKey3, blockingStubFull);    if (channelSolidity1 != null) {
      channelSolidity1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}