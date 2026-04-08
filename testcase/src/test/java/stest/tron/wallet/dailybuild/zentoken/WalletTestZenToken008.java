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
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.ShieldAddressInfo;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;


@Slf4j
public class WalletTestZenToken008 extends TronBaseTest {

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
  private Long costTokenAmount = 1 * zenTokenFee + 1;
  private Long sendTokenAmount = 1 * zenTokenFee;

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


  }

  @Test(enabled = false,
      description = "Public send 1 token to shield transaction", groups = {"daily", "shield"})
  public void test1Shield2ShieldTransaction() {
    sendShieldAddressInfo = PublicMethod.generateShieldAddress();
    sendShieldAddress = sendShieldAddressInfo.get().getAddress();
    logger.info("sendShieldAddressInfo:" + sendShieldAddressInfo);
    memo = "Shield 1 token memo in " + System.currentTimeMillis();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, sendShieldAddress,
        "1", memo);
    Assert.assertFalse(PublicMethod.sendShieldCoin(zenTokenOwnerAddress, sendTokenAmount, null,
        null, shieldOutList, null, 0, zenTokenOwnerKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.sendShieldCoin(zenTokenOwnerAddress, costTokenAmount, null,
        null, shieldOutList, null, 0, zenTokenOwnerKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    notes = PublicMethod.listShieldNote(sendShieldAddressInfo, blockingStubFull);
    sendNote = notes.getNoteTxs(0).getNote();
    Assert.assertTrue(sendNote.getValue() == 1);

  }

  @Test(enabled = false,
      description = "Shield send 0 token to shield transaction", groups = {"daily", "shield"})
  public void test2Shield2ShieldTransaction() {
    Assert.assertTrue(PublicMethod.transferAsset(zenTokenOwnerAddress, tokenId,
        zenTokenFee * 2, foundationZenTokenAddress, foundationZenTokenKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long afterAssetBalance = PublicMethod.getAssetIssueValue(zenTokenOwnerAddress,
        PublicMethod.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
        blockingStubFull);

    logger.info("token balance is " + afterAssetBalance);
    sendShieldAddressInfo = PublicMethod.generateShieldAddress();
    sendShieldAddress = sendShieldAddressInfo.get().getAddress();
    logger.info("sendShieldAddressInfo:" + sendShieldAddressInfo);
    memo = "Shield costFee token memo in " + System.currentTimeMillis();
    shieldOutList.clear();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, sendShieldAddress,
        "" + zenTokenFee, memo);
  //logger.info();
    Assert.assertTrue(PublicMethod.sendShieldCoin(zenTokenOwnerAddress, zenTokenFee * 2, null,
        null, shieldOutList, null, 0, zenTokenOwnerKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    receiverShieldAddressInfo = PublicMethod.generateShieldAddress();
    receiverShieldAddress = receiverShieldAddressInfo.get().getAddress();

    shieldOutList.clear();
    memo = "Send shield to receiver shield memo in" + System.currentTimeMillis();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, receiverShieldAddress,
        "0", memo);
  //Wrong proof
    Assert.assertFalse(PublicMethod.sendShieldCoin(
        null, 0,
        sendShieldAddressInfo.get(), notes.getNoteTxs(0),
        shieldOutList,
        null, 0,
        zenTokenOwnerKey, blockingStubFull));
  //Amount is -1
    notes = PublicMethod.listShieldNote(sendShieldAddressInfo, blockingStubFull);
    shieldOutList.clear();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, receiverShieldAddress,
        "-1", memo);
    Assert.assertFalse(PublicMethod.sendShieldCoin(
        null, 0,
        sendShieldAddressInfo.get(), notes.getNoteTxs(0),
        shieldOutList,
        null, 0,
        zenTokenOwnerKey, blockingStubFull));

    notes = PublicMethod.listShieldNote(sendShieldAddressInfo, blockingStubFull);
    shieldOutList.clear();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, receiverShieldAddress,
        "0", memo);
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
    Assert.assertTrue(receiverNote.getValue() == 0);
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