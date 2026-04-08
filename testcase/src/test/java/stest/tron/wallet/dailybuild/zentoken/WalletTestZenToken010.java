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
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.ShieldAddressInfo;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;


@Slf4j
public class WalletTestZenToken010 extends TronBaseTest {

  private static ByteString assetAccountId = null;  Optional<ShieldAddressInfo> sendShieldAddressInfo;
  Optional<ShieldAddressInfo> receiverShieldAddressInfo;
  String sendShieldAddress;
  String receiverShieldAddress;
  List<Note> shieldOutList = new ArrayList<>();
  DecryptNotes notes;
  String memo;
  Note sendNote;
  Note receiverNote;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] zenTokenOwnerAddress = ecKey1.getAddress();
  String zenTokenOwnerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
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
  private String txid;
  private Optional<TransactionInfo> infoById;
  private Optional<Transaction> byId;

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
    PublicMethod.printAddress(zenTokenOwnerKey);    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext()
        .build();
    channelSolidity1 = ManagedChannelBuilder.forTarget(soliditynode1)
        .usePlaintext()
        .build();
    blockingStubSolidity1 = WalletSolidityGrpc.newBlockingStub(channelSolidity1);

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

  @Test(enabled = false, description = "Shield to itself transaction", groups = {"daily", "shield"})
  public void test1Shield2ShieldTransaction() {
    shieldOutList.clear();
    memo = "Send shield to itself memo1 in " + System.currentTimeMillis();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, sendShieldAddress,
        "" + zenTokenFee, memo);

    memo = "Send shield to itself memo2 in " + System.currentTimeMillis();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, sendShieldAddress,
        "" + (sendNote.getValue() - 2 * zenTokenFee), memo);
    Assert.assertTrue(PublicMethod.sendShieldCoin(
        null, 0,
        sendShieldAddressInfo.get(), notes.getNoteTxs(0),
        shieldOutList,
        null, 0,
        zenTokenOwnerKey, blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    notes = PublicMethod.getShieldNotesByIvk(sendShieldAddressInfo, blockingStubFull);
    Assert.assertTrue(notes.getNoteTxsCount() == 3);
    Assert.assertTrue(notes.getNoteTxs(1).getNote().getValue() == zenTokenFee);
    Assert.assertTrue(notes.getNoteTxs(2).getNote().getValue()
        == sendNote.getValue() - 2 * zenTokenFee);
    Assert.assertEquals(notes.getNoteTxs(1).getNote().getPaymentAddress(),
        notes.getNoteTxs(2).getNote().getPaymentAddress());
    Assert.assertEquals(notes.getNoteTxs(1).getTxid(), notes.getNoteTxs(2).getTxid());
    Assert.assertTrue(PublicMethod.getSpendResult(sendShieldAddressInfo.get(),
        notes.getNoteTxs(0), blockingStubFull).getResult());

    notes = PublicMethod.getShieldNotesByOvk(sendShieldAddressInfo, blockingStubFull);
    Assert.assertTrue(notes.getNoteTxsCount() == 2);
  }

  @Test(enabled = false, description = "From shield only have one zenToken fee", groups = {"daily", "shield"})
  public void test2Shield2ShieldTransaction() {
    sendShieldAddressInfo = PublicMethod.generateShieldAddress();
    sendShieldAddress = sendShieldAddressInfo.get().getAddress();
    logger.info("sendShieldAddressInfo:" + sendShieldAddressInfo);
    memo = "Shield memo in" + System.currentTimeMillis();
    shieldOutList.clear();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, sendShieldAddress,
        "" + zenTokenFee, memo);
    Assert.assertTrue(PublicMethod.sendShieldCoin(zenTokenOwnerAddress, 2 * zenTokenFee, null,
        null, shieldOutList, null, 0, zenTokenOwnerKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    notes = PublicMethod.listShieldNote(sendShieldAddressInfo, blockingStubFull);
    sendNote = notes.getNoteTxs(0).getNote();

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    shieldOutList.clear();
    memo = "Send shield to itself memo1 in " + System.currentTimeMillis();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, sendShieldAddress,
        "0", memo);

    memo = "Send shield to itself memo2 in " + System.currentTimeMillis();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, sendShieldAddress,
        "0", memo);
    Assert.assertTrue(PublicMethod.sendShieldCoin(
        null, 0,
        sendShieldAddressInfo.get(), notes.getNoteTxs(0),
        shieldOutList,
        null, 0,
        zenTokenOwnerKey, blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    notes = PublicMethod.getShieldNotesByIvk(sendShieldAddressInfo, blockingStubFull);
    Assert.assertTrue(notes.getNoteTxsCount() == 3);
    logger.info("index 0:" + notes.getNoteTxs(0).getNote().getValue());
    logger.info("index 1:" + notes.getNoteTxs(1).getNote().getValue());
    logger.info("index 2:" + notes.getNoteTxs(2).getNote().getValue());
    Assert.assertTrue(notes.getNoteTxs(1).getNote().getValue() == 0);
    Assert.assertTrue(notes.getNoteTxs(2).getNote().getValue() == 0);
    Assert.assertEquals(notes.getNoteTxs(1).getNote().getPaymentAddress(),
        notes.getNoteTxs(2).getNote().getPaymentAddress());
    Assert.assertEquals(notes.getNoteTxs(1).getTxid(), notes.getNoteTxs(2).getTxid());
  }

  @Test(enabled = false, description = "From public and to public is same one", groups = {"daily", "shield"})
  public void test3Public2ShieldAndPublicItselfTransaction() {
    sendShieldAddressInfo = PublicMethod.generateShieldAddress();
    sendShieldAddress = sendShieldAddressInfo.get().getAddress();

    ecKey1 = new ECKey(Utils.getRandom());
    zenTokenOwnerAddress = ecKey1.getAddress();
    zenTokenOwnerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    Assert.assertTrue(PublicMethod.transferAsset(zenTokenOwnerAddress, tokenId,
        costTokenAmount, foundationZenTokenAddress, foundationZenTokenKey, blockingStubFull));
  final Long beforeAssetBalance = PublicMethod.getAssetIssueValue(zenTokenOwnerAddress,
        PublicMethod.queryAccount(foundationZenTokenAddress, blockingStubFull).getAccountId(),
        blockingStubFull);
  final Long beforeBalance = PublicMethod.queryAccount(zenTokenOwnerAddress, blockingStubFull)
        .getBalance();
    shieldOutList.clear();
    memo = "From public and to public is same one memo in " + System.currentTimeMillis();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, sendShieldAddress,
        "" + 2 * zenTokenFee, memo);

    Assert.assertFalse(PublicMethod
        .sendShieldCoin(zenTokenOwnerAddress, costTokenAmount, null, null, shieldOutList,
            zenTokenOwnerAddress, 7 * zenTokenFee, zenTokenOwnerKey, blockingStubFull));
  Long afterAssetBalance = PublicMethod.getAssetIssueValue(zenTokenOwnerAddress,
        PublicMethod.queryAccount(foundationZenTokenAddress, blockingStubFull).getAccountId(),
        blockingStubFull);
  Long afterBalance = PublicMethod.queryAccount(zenTokenOwnerAddress, blockingStubFull)
        .getBalance();

    Assert.assertEquals(beforeAssetBalance, afterAssetBalance);
    Assert.assertEquals(beforeBalance, afterBalance);

    notes = PublicMethod.getShieldNotesByIvk(sendShieldAddressInfo, blockingStubFull);
    Assert.assertTrue(notes.getNoteTxsCount() == 0);


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
    }
  }
}