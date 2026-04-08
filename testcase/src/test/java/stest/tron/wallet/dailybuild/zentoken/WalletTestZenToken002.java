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
import org.tron.api.GrpcAPI.DecryptNotes;
import org.tron.api.GrpcAPI.Note;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.contract.ShieldContract.IncrementalMerkleVoucherInfo;
import org.tron.protos.contract.ShieldContract.OutputPoint;
import org.tron.protos.contract.ShieldContract.OutputPointInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.ShieldAddressInfo;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;


@Slf4j
public class WalletTestZenToken002 extends TronBaseTest {

  private static ByteString assetAccountId = null;  Optional<ShieldAddressInfo> sendShieldAddressInfo;
  Optional<ShieldAddressInfo> receiverShieldAddressInfo;
  String sendShieldAddress;
  String receiverShieldAddress;
  List<Note> shieldOutList = new ArrayList<>();
  DecryptNotes notes;
  String memo;
  Note sendNote;
  Note receiverNote;
  IncrementalMerkleVoucherInfo firstMerkleVoucherInfo;
  IncrementalMerkleVoucherInfo secondMerkleVoucherInfo;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] zenTokenOwnerAddress = ecKey1.getAddress();
  String zenTokenOwnerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ManagedChannel channelSolidity1 = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity1 = null;
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);
  private String soliditynode1 = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(1);
  private String soliInPbft = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(2);
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
    initPbftChannel();
    initSolidityChannel();
    PublicMethod.printAddress(foundationZenTokenKey);
    PublicMethod.printAddress(zenTokenOwnerKey);    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext()
        .build();
    channelSolidity1 = ManagedChannelBuilder.forTarget(soliditynode1)
        .usePlaintext()
        .build();
    blockingStubSolidity1 = WalletSolidityGrpc.newBlockingStub(channelSolidity1);

    channelPbft = ManagedChannelBuilder.forTarget(soliInPbft)
        .usePlaintext()
        .build();
    Assert.assertTrue(PublicMethod.transferAsset(zenTokenOwnerAddress, tokenId,
        costTokenAmount, foundationZenTokenAddress, foundationZenTokenKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  //Args.setFullNodeAllowShieldedTransaction(true);
    sendShieldAddressInfo = PublicMethod.generateShieldAddress();
    sendShieldAddress = sendShieldAddressInfo.get().getAddress();
    logger.info("sendShieldAddressInfo:" + sendShieldAddressInfo);
    memo = "Shield memo in" + System.currentTimeMillis();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, sendShieldAddress,
        "" + (sendTokenAmount - zenTokenFee), memo);
    Assert.assertTrue(PublicMethod.sendShieldCoin(zenTokenOwnerAddress, sendTokenAmount, null,
        null, shieldOutList, null, 0, zenTokenOwnerKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    notes = PublicMethod.listShieldNote(sendShieldAddressInfo, blockingStubFull);
    sendNote = notes.getNoteTxs(0).getNote();
  }

  @Test(enabled = false, description = "Get merkle tree voucher info", groups = {"daily", "shield"})
  public void test01GetMerkleTreeVoucherInfo() {
    notes = PublicMethod.listShieldNote(sendShieldAddressInfo, blockingStubFull);
    sendNote = notes.getNoteTxs(0).getNote();
    OutputPointInfo.Builder request = OutputPointInfo.newBuilder();
  //ShieldNoteInfo noteInfo = shieldWrapper.getUtxoMapNote().get(shieldInputList.get(i));
    OutputPoint.Builder outPointBuild = OutputPoint.newBuilder();
    outPointBuild.setHash(ByteString.copyFrom(notes.getNoteTxs(0).getTxid().toByteArray()));
    outPointBuild.setIndex(notes.getNoteTxs(0).getIndex());
    request.addOutPoints(outPointBuild.build());
    firstMerkleVoucherInfo = blockingStubFull
        .getMerkleTreeVoucherInfo(request.build());
  }


  @Test(enabled = false, description = "Shield to shield transaction", groups = {"daily", "shield"})
  public void test02Shield2ShieldTransaction() {
    receiverShieldAddressInfo = PublicMethod.generateShieldAddress();
    receiverShieldAddress = receiverShieldAddressInfo.get().getAddress();

    shieldOutList.clear();
    ;
    memo = "Send shield to receiver shield memo in" + System.currentTimeMillis();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, receiverShieldAddress,
        "" + (sendNote.getValue() - zenTokenFee), memo);
    Assert.assertTrue(PublicMethod.sendShieldCoin(
        null, 0,
        sendShieldAddressInfo.get(), notes.getNoteTxs(0),
        shieldOutList,
        null, 0,
        zenTokenOwnerKey, blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    notes = PublicMethod.listShieldNote(receiverShieldAddressInfo, blockingStubFull);
    receiverNote = notes.getNoteTxs(0).getNote();
    logger.info("Receiver note:" + receiverNote.toString());
    Assert.assertTrue(receiverNote.getValue() == sendNote.getValue() - zenTokenFee);

  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "Scan note by ivk and scan not by ivk on FullNode", groups = {"daily", "shield"})
  public void test03ScanNoteByIvkAndOvk() {
    //Scan sender note by ovk equals scan receiver note by ivk on FullNode
    Note scanNoteByIvk = PublicMethod
        .getShieldNotesByIvk(receiverShieldAddressInfo, blockingStubFull).getNoteTxs(0).getNote();
    Note scanNoteByOvk = PublicMethod
        .getShieldNotesByOvk(sendShieldAddressInfo, blockingStubFull).getNoteTxs(0).getNote();
    Assert.assertEquals(scanNoteByIvk.getValue(), scanNoteByOvk.getValue());
    Assert.assertEquals(scanNoteByIvk.getMemo(), scanNoteByOvk.getMemo());
    Assert.assertEquals(scanNoteByIvk.getRcm(), scanNoteByOvk.getRcm());
    Assert.assertEquals(scanNoteByIvk.getPaymentAddress(), scanNoteByOvk.getPaymentAddress());
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "Scan note by ivk and scan not by ivk on solidity", groups = {"daily", "shield"})
  public void test04ScanNoteByIvkAndOvkOnSolidityServer() {

    //Scan sender note by ovk equals scan receiver note by ivk in Solidity
    PublicMethod.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSolidity);
    Note scanNoteByIvk = PublicMethod
        .getShieldNotesByIvkOnSolidity(receiverShieldAddressInfo, blockingStubSolidity)
        .getNoteTxs(0).getNote();
    Note scanNoteByOvk = PublicMethod
        .getShieldNotesByOvkOnSolidity(sendShieldAddressInfo, blockingStubSolidity)
        .getNoteTxs(0).getNote();
    Assert.assertEquals(scanNoteByIvk.getValue(), scanNoteByOvk.getValue());
    Assert.assertEquals(scanNoteByIvk.getMemo(), scanNoteByOvk.getMemo());
    Assert.assertEquals(scanNoteByIvk.getRcm(), scanNoteByOvk.getRcm());
    Assert.assertEquals(scanNoteByIvk.getPaymentAddress(), scanNoteByOvk.getPaymentAddress());
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "Scan note by ivk and scan not by ivk on Pbft", groups = {"daily", "shield"})
  public void test05ScanNoteByIvkAndOvkOnPbftServer() {

    //Scan sender note by ovk equals scan receiver note by ivk in Solidity
    PublicMethod.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSolidity);
    Note scanNoteByIvk = PublicMethod
        .getShieldNotesByIvkOnSolidity(receiverShieldAddressInfo, blockingStubPbft)
        .getNoteTxs(0).getNote();
    Note scanNoteByOvk = PublicMethod
        .getShieldNotesByOvkOnSolidity(sendShieldAddressInfo, blockingStubPbft)
        .getNoteTxs(0).getNote();
    Assert.assertEquals(scanNoteByIvk.getValue(), scanNoteByOvk.getValue());
    Assert.assertEquals(scanNoteByIvk.getMemo(), scanNoteByOvk.getMemo());
    Assert.assertEquals(scanNoteByIvk.getRcm(), scanNoteByOvk.getRcm());
    Assert.assertEquals(scanNoteByIvk.getPaymentAddress(), scanNoteByOvk.getPaymentAddress());
  }


  /**
   * constructor.
   */
  @Test(enabled = false, description = "Scan note by ivk and scan not by ivk on solidity", groups = {"daily", "shield"})
  public void test06ScanNoteByIvkAndOvkOnSolidityServer() {
    //Scan sender note by ovk equals scan receiver note by ivk in Solidity
    PublicMethod.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSolidity1);
    Note scanNoteByIvk = PublicMethod
        .getShieldNotesByIvkOnSolidity(receiverShieldAddressInfo, blockingStubSolidity1)
        .getNoteTxs(0).getNote();
    Note scanNoteByOvk = PublicMethod
        .getShieldNotesByOvkOnSolidity(sendShieldAddressInfo, blockingStubSolidity1)

        .getNoteTxs(0).getNote();
    Assert.assertEquals(scanNoteByIvk.getValue(), scanNoteByOvk.getValue());
    Assert.assertEquals(scanNoteByIvk.getMemo(), scanNoteByOvk.getMemo());
    Assert.assertEquals(scanNoteByIvk.getRcm(), scanNoteByOvk.getRcm());
    Assert.assertEquals(scanNoteByIvk.getPaymentAddress(), scanNoteByOvk.getPaymentAddress());
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "Query whether note is spend on solidity", groups = {"daily", "shield"})
  public void test07QueryNoteIsSpendOnSolidity() {
    notes = PublicMethod.listShieldNote(sendShieldAddressInfo, blockingStubFull);
  //Scan sender note by ovk equals scan receiver note by ivk in Solidity
    Assert.assertTrue(PublicMethod.getSpendResult(sendShieldAddressInfo.get(),
        notes.getNoteTxs(0), blockingStubFull).getResult());
    Assert.assertTrue(PublicMethod.getSpendResultOnSolidity(sendShieldAddressInfo.get(),
        notes.getNoteTxs(0), blockingStubSolidity).getResult());
    Assert.assertTrue(PublicMethod.getSpendResultOnSolidity(sendShieldAddressInfo.get(),
        notes.getNoteTxs(0), blockingStubSolidity1).getResult());
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "Query whether note is spend on PBFT", groups = {"daily", "shield"})
  public void test08QueryNoteIsSpendOnPbft() {
    notes = PublicMethod.listShieldNote(sendShieldAddressInfo, blockingStubFull);
  //Scan sender note by ovk equals scan receiver note by ivk in Solidity
    Assert.assertTrue(PublicMethod.getSpendResult(sendShieldAddressInfo.get(),
        notes.getNoteTxs(0), blockingStubFull).getResult());
    Assert.assertTrue(PublicMethod.getSpendResultOnSolidity(sendShieldAddressInfo.get(),
        notes.getNoteTxs(0), blockingStubPbft).getResult());
  }


  /**
   * constructor.
   */
  @Test(enabled = false, description = "Query note and spend status on fullnode and solidity", groups = {"daily", "shield"})
  public void test09QueryNoteAndSpendStatusOnFullnode() {
    Assert.assertFalse(
        PublicMethod.getShieldNotesAndMarkByIvk(receiverShieldAddressInfo, blockingStubFull)
            .getNoteTxs(0).getIsSpend());
    Note scanNoteByIvk = PublicMethod
        .getShieldNotesByIvk(receiverShieldAddressInfo, blockingStubFull)
        .getNoteTxs(0).getNote();
    Assert.assertEquals(scanNoteByIvk,
        PublicMethod.getShieldNotesAndMarkByIvk(receiverShieldAddressInfo, blockingStubFull)
            .getNoteTxs(0).getNote());

    Assert.assertFalse(PublicMethod
        .getShieldNotesAndMarkByIvkOnSolidity(receiverShieldAddressInfo, blockingStubSolidity)
        .getNoteTxs(0).getIsSpend());
    scanNoteByIvk = PublicMethod
        .getShieldNotesByIvkOnSolidity(receiverShieldAddressInfo, blockingStubSolidity)
        .getNoteTxs(0).getNote();
    Assert.assertEquals(scanNoteByIvk, PublicMethod
        .getShieldNotesAndMarkByIvkOnSolidity(receiverShieldAddressInfo, blockingStubSolidity)
        .getNoteTxs(0).getNote());
    Assert.assertEquals(scanNoteByIvk, PublicMethod
        .getShieldNotesAndMarkByIvkOnSolidity(receiverShieldAddressInfo, blockingStubPbft)
        .getNoteTxs(0).getNote());

    shieldOutList.clear();
    memo = "Query note and spend status on fullnode " + System.currentTimeMillis();
    notes = PublicMethod.listShieldNote(receiverShieldAddressInfo, blockingStubFull);
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, sendShieldAddress,
        "" + (notes.getNoteTxs(0).getNote().getValue() - zenTokenFee), memo);
    Assert.assertTrue(PublicMethod.sendShieldCoin(
        null, 0,
        receiverShieldAddressInfo.get(), notes.getNoteTxs(0),
        shieldOutList,
        null, 0,
        zenTokenOwnerKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSolidity);

    Assert.assertTrue(
        PublicMethod.getShieldNotesAndMarkByIvk(receiverShieldAddressInfo, blockingStubFull)
            .getNoteTxs(0).getIsSpend());

    Assert.assertTrue(PublicMethod
        .getShieldNotesAndMarkByIvkOnSolidity(receiverShieldAddressInfo, blockingStubSolidity)
        .getNoteTxs(0).getIsSpend());
  }

  @Test(enabled = false, description = "Get merkle tree voucher info", groups = {"daily", "shield"})
  public void test10GetMerkleTreeVoucherInfo() {
    notes = PublicMethod.listShieldNote(sendShieldAddressInfo, blockingStubFull);
    sendNote = notes.getNoteTxs(0).getNote();
    OutputPointInfo.Builder request = OutputPointInfo.newBuilder();
  //ShieldNoteInfo noteInfo = shieldWrapper.getUtxoMapNote().get(shieldInputList.get(i));
    OutputPoint.Builder outPointBuild = OutputPoint.newBuilder();
    outPointBuild.setHash(ByteString.copyFrom(notes.getNoteTxs(0).getTxid().toByteArray()));
    outPointBuild.setIndex(notes.getNoteTxs(0).getIndex());
    request.addOutPoints(outPointBuild.build());
    secondMerkleVoucherInfo = blockingStubFull
        .getMerkleTreeVoucherInfo(request.build());

    Assert.assertEquals(firstMerkleVoucherInfo, secondMerkleVoucherInfo);
  }

  @Test(enabled = false, description = "Get merkle tree voucher info from Solidity", groups = {"daily", "shield"})
  public void test11GetMerkleTreeVoucherInfoFromSolidity() {
    notes = PublicMethod.listShieldNote(sendShieldAddressInfo, blockingStubFull);
    sendNote = notes.getNoteTxs(0).getNote();
    OutputPointInfo.Builder request = OutputPointInfo.newBuilder();
  //ShieldNoteInfo noteInfo = shieldWrapper.getUtxoMapNote().get(shieldInputList.get(i));
    OutputPoint.Builder outPointBuild = OutputPoint.newBuilder();
    outPointBuild.setHash(ByteString.copyFrom(notes.getNoteTxs(0).getTxid().toByteArray()));
    outPointBuild.setIndex(notes.getNoteTxs(0).getIndex());
    request.addOutPoints(outPointBuild.build());
    secondMerkleVoucherInfo = blockingStubSolidity
        .getMerkleTreeVoucherInfo(request.build());

    Assert.assertEquals(firstMerkleVoucherInfo, secondMerkleVoucherInfo);
  }

  @Test(enabled = false, description = "Get merkle tree voucher info from Pbft", groups = {"daily", "shield"})
  public void test12GetMerkleTreeVoucherInfoFromPbft() {
    notes = PublicMethod.listShieldNote(sendShieldAddressInfo, blockingStubFull);
    sendNote = notes.getNoteTxs(0).getNote();
    OutputPointInfo.Builder request = OutputPointInfo.newBuilder();
  //ShieldNoteInfo noteInfo = shieldWrapper.getUtxoMapNote().get(shieldInputList.get(i));
    OutputPoint.Builder outPointBuild = OutputPoint.newBuilder();
    outPointBuild.setHash(ByteString.copyFrom(notes.getNoteTxs(0).getTxid().toByteArray()));
    outPointBuild.setIndex(notes.getNoteTxs(0).getIndex());
    request.addOutPoints(outPointBuild.build());
    secondMerkleVoucherInfo = blockingStubPbft
        .getMerkleTreeVoucherInfo(request.build());

    Assert.assertEquals(firstMerkleVoucherInfo, secondMerkleVoucherInfo);
  }


  /**
   * constructor.
   */

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    PublicMethod.transferAsset(foundationZenTokenAddress, tokenId,
        PublicMethod.getAssetIssueValue(zenTokenOwnerAddress,
            PublicMethod.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
            blockingStubFull), zenTokenOwnerAddress, zenTokenOwnerKey, blockingStubFull);    if (channelSolidity1 != null) {
      channelSolidity1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }  }
}