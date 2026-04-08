package stest.tron.wallet.dailybuild.zentoken;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.DecryptNotes;
import org.tron.api.GrpcAPI.Note;
import org.tron.api.GrpcAPI.SpendResult;
import org.tron.protos.Protocol.Account;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.ShieldAddressInfo;
import stest.tron.wallet.common.client.utils.Utils;
import io.grpc.ManagedChannelBuilder;
import org.tron.api.WalletGrpc;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class WalletTestZenToken001 extends TronBaseTest {  Optional<ShieldAddressInfo> shieldAddressInfo;
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
  private Long costTokenAmount = 8 * zenTokenFee;
  private Long sendTokenAmount = 3 * zenTokenFee;

  /**
   * constructor.
   */
  @BeforeSuite(enabled = false)
  public void beforeSuite() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext().build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    logger.info("enter this");
    if (PublicMethod.queryAccount(foundationZenTokenKey, blockingStubFull).getCreateTime() == 0) {
      PublicMethod.sendcoin(foundationZenTokenAddress, 20480000000000L, foundationAddress,
          foundationKey, blockingStubFull);
      PublicMethod.waitProduceNextBlock(blockingStubFull);
  String name = "shieldToken";
  Long start = System.currentTimeMillis() + 20000;
  Long end = System.currentTimeMillis() + 10000000000L;
  Long totalSupply = 15000000000000001L;
  String description = "This asset issue is use for exchange transaction stress";
  String url = "This asset issue is use for exchange transaction stress";
      PublicMethod.createAssetIssue(foundationZenTokenAddress, name, totalSupply, 1, 1,
          start, end, 1, description, url, 1000L, 1000L,
          1L, 1L, foundationZenTokenKey, blockingStubFull);
      PublicMethod.waitProduceNextBlock(blockingStubFull);
      Account getAssetIdFromThisAccount =
          PublicMethod.queryAccount(foundationZenTokenAddress, blockingStubFull);
      ByteString assetAccountId = getAssetIdFromThisAccount.getAssetIssuedID();
      logger.info("AssetId:" + assetAccountId.toString());
    }
  }

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(foundationZenTokenKey);
    PublicMethod.printAddress(zenTokenOwnerKey);    Assert.assertTrue(PublicMethod.transferAsset(zenTokenOwnerAddress, tokenId,
        costTokenAmount, foundationZenTokenAddress, foundationZenTokenKey,
        blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = false, description = "Public to shield transaction", groups = {"daily", "shield"})
  public void test1Public2ShieldTransaction() {
    //Args.setFullNodeAllowShieldedTransaction(true);
    shieldAddressInfo = PublicMethod.generateShieldAddress();
    shieldAddress = shieldAddressInfo.get().getAddress();
    logger.info("shieldAddress:" + shieldAddress);
  final Long beforeAssetBalance = PublicMethod.getAssetIssueValue(zenTokenOwnerAddress,
        PublicMethod.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
        blockingStubFull);
  final Long beforeNetUsed = PublicMethod
        .getAccountResource(zenTokenOwnerAddress, blockingStubFull).getFreeNetUsed();

    memo = "aaaaaaa";

    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, shieldAddress,
        "" + (sendTokenAmount - zenTokenFee), memo);

    Assert.assertTrue(PublicMethod.sendShieldCoin(
        zenTokenOwnerAddress, sendTokenAmount,
        null, null,
        shieldOutList,
        null, 0,
        zenTokenOwnerKey, blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long afterAssetBalance = PublicMethod.getAssetIssueValue(zenTokenOwnerAddress,
        PublicMethod.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
        blockingStubFull);
  Long afterNetUsed = PublicMethod.getAccountResource(zenTokenOwnerAddress, blockingStubFull)
        .getFreeNetUsed();
    Assert.assertTrue(beforeAssetBalance - afterAssetBalance == sendTokenAmount);
    Assert.assertTrue(beforeNetUsed == afterNetUsed);
    notes = PublicMethod.listShieldNote(shieldAddressInfo, blockingStubFull);
    note = notes.getNoteTxs(0).getNote();
  Long receiverShieldTokenAmount = note.getValue();
    Assert.assertTrue(receiverShieldTokenAmount == sendTokenAmount - zenTokenFee);
    Assert.assertEquals(memo, PublicMethod.getMemo(note));
  }

  @Test(enabled = false, description = "Shield to public transaction", groups = {"daily", "shield"})
  public void test2Shield2PublicTransaction() {
    note = notes.getNoteTxs(0).getNote();
    SpendResult result = PublicMethod.getSpendResult(shieldAddressInfo.get(),
        notes.getNoteTxs(0), blockingStubFull);
    Assert.assertTrue(!result.getResult());

    shieldOutList.clear();
  final Long beforeAssetBalance = PublicMethod.getAssetIssueValue(zenTokenOwnerAddress,
        PublicMethod.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
        blockingStubFull);

    Assert.assertTrue(PublicMethod.sendShieldCoin(
        null, 0,
        shieldAddressInfo.get(), notes.getNoteTxs(0),
        shieldOutList,
        zenTokenOwnerAddress, note.getValue() - zenTokenFee,
        zenTokenOwnerKey, blockingStubFull));
  //When you want to send shield coin to public account,you should add one zero output amount cm
    /*    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, shieldAddress,
        "0", memo);
    Assert.assertTrue(PublicMethod.sendShieldCoin(
        null, 0,
        shieldAddressInfo.get(), notes.getNoteTxs(0),
        shieldOutList,
        zenTokenOwnerAddress, note.getValue() - zenTokenFee,
        zenTokenOwnerKey, blockingStubFull));*/

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    result = PublicMethod.getSpendResult(shieldAddressInfo.get(), notes.getNoteTxs(0),
        blockingStubFull);
    Assert.assertTrue(result.getResult());
  Long afterAssetBalance = PublicMethod.getAssetIssueValue(zenTokenOwnerAddress,
        PublicMethod.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
        blockingStubFull);
    Assert.assertTrue(afterAssetBalance - beforeAssetBalance == note.getValue() - zenTokenFee);
    logger.info("beforeAssetBalance:" + beforeAssetBalance);
    logger.info("afterAssetBalance :" + afterAssetBalance);
  }


  @Test(enabled = false,
      description = "Output amount can't be zero or below zero", groups = {"daily", "shield"})
  public void test3Shield2PublicAmountIsZero() {
    shieldAddressInfo = PublicMethod.generateShieldAddress();
    shieldAddress = shieldAddressInfo.get().getAddress();
    memo = "Shield to public amount is zero";
    shieldOutList.clear();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, shieldAddress,
        "" + (sendTokenAmount - zenTokenFee), memo);
    Assert.assertTrue(PublicMethod.sendShieldCoin(
        zenTokenOwnerAddress, sendTokenAmount,
        null, null,
        shieldOutList,
        null, 0,
        zenTokenOwnerKey, blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    notes = PublicMethod.listShieldNote(shieldAddressInfo, blockingStubFull);
    note = notes.getNoteTxs(0).getNote();

    shieldOutList.clear();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, shieldAddress,
        "" + (note.getValue() - zenTokenFee - (zenTokenFee - note.getValue())), memo);
    Assert.assertFalse(PublicMethod.sendShieldCoin(
        null, 0,
        shieldAddressInfo.get(), notes.getNoteTxs(0),
        shieldOutList,
        zenTokenOwnerAddress, zenTokenFee - note.getValue(),
        zenTokenOwnerKey, blockingStubFull));

    shieldOutList.clear();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, shieldAddress,
        "" + (note.getValue() - zenTokenFee), memo);

    Assert.assertFalse(PublicMethod.sendShieldCoin(
        null, 0,
        shieldAddressInfo.get(), notes.getNoteTxs(0),
        shieldOutList,
        zenTokenOwnerAddress, 0,
        zenTokenOwnerKey, blockingStubFull));

    shieldOutList.clear();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, shieldAddress,
        "" + (-zenTokenFee), memo);
    Assert.assertFalse(PublicMethod.sendShieldCoin(
        null, 0,
        shieldAddressInfo.get(), notes.getNoteTxs(0),
        shieldOutList,
        zenTokenOwnerAddress, note.getValue(),
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