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
import stest.tron.wallet.common.client.utils.PublicMethodForMultiSign;
import stest.tron.wallet.common.client.utils.ShieldAddressInfo;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class WalletTestZenToken009 extends TronBaseTest {  Optional<ShieldAddressInfo> shieldAddressInfo;
  String shieldAddress;
  Optional<ShieldAddressInfo> receiverAddressInfo;
  String receiverAddress;
  List<Note> shieldOutList = new ArrayList<>();
  List<Long> shieldInputList = new ArrayList<>();
  DecryptNotes notes;
  String memo;
  Note note;
  String[] permissionKeyString = new String[2];
  String[] ownerKeyString = new String[2];
  String accountPermissionJson = "";
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] manager1Address = ecKey1.getAddress();
  String manager1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] manager2Address = ecKey2.getAddress();
  String manager2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] ownerAddress = ecKey3.getAddress();
  String ownerKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
  ECKey ecKey4 = new ECKey(Utils.getRandom());
  byte[] zenTokenOwnerAddress = ecKey4.getAddress();
  String zenTokenOwnerKey = ByteArray.toHexString(ecKey4.getPrivKeyBytes());
  private String foundationZenTokenKey = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTokenOwnerKey");
  byte[] foundationZenTokenAddress = PublicMethod.getFinalAddress(foundationZenTokenKey);
  private String zenTokenId = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTokenId");
  private byte[] tokenId = zenTokenId.getBytes();
  private Long zenTokenFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.zenTokenFee");
  private Long costTokenAmount = 5 * zenTokenFee;
  private Long sendTokenAmount = 3 * zenTokenFee;
  private long multiSignFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.multiSignFee");
  private long updateAccountPermissionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.updateAccountPermissionFee");

  /**
   * constructor.
   */


  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(foundationZenTokenKey);
    PublicMethod.printAddress(zenTokenOwnerKey);    long needCoin = updateAccountPermissionFee * 1 + multiSignFee * 3;
    Assert.assertTrue(
        PublicMethod.sendcoin(zenTokenOwnerAddress, needCoin + 2048000000L, foundationAddress, foundationKey,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    permissionKeyString[0] = manager1Key;
    permissionKeyString[1] = manager2Key;
    ownerKeyString[0] = zenTokenOwnerKey;
    ownerKeyString[1] = manager1Key;
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(zenTokenOwnerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(manager2Key) + "\",\"weight\":1}"
            + "]}]}";

    logger.info(accountPermissionJson);
    Assert.assertTrue(PublicMethodForMultiSign
        .accountPermissionUpdate(accountPermissionJson, zenTokenOwnerAddress, zenTokenOwnerKey,
            blockingStubFull, ownerKeyString));
    Assert.assertTrue(PublicMethod.transferAsset(zenTokenOwnerAddress, tokenId,
        costTokenAmount, foundationZenTokenAddress, foundationZenTokenKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = false,
      description = "Public to shield transaction with multisign", groups = {"daily", "shield"})
  public void test1Public2ShieldTransaction() {
    //Args.setFullNodeAllowShieldedTransaction(true);
    shieldAddressInfo = PublicMethod.generateShieldAddress();
    shieldAddress = shieldAddressInfo.get().getAddress();
    logger.info("shieldAddress:" + shieldAddress);
  final Long beforeAssetBalance = PublicMethod.getAssetIssueValue(zenTokenOwnerAddress,
        PublicMethod.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
        blockingStubFull);
  final Long beforeBalance = PublicMethod
        .queryAccount(zenTokenOwnerAddress, blockingStubFull).getBalance();
  final Long beforeNetUsed = PublicMethod
        .getAccountResource(zenTokenOwnerAddress, blockingStubFull).getFreeNetUsed();

    memo = "aaaaaaa";

    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, shieldAddress,
        "" + (sendTokenAmount - zenTokenFee), memo);

    Assert.assertTrue(PublicMethodForMultiSign.sendShieldCoin(
        zenTokenOwnerAddress, sendTokenAmount,
        null, null,
        shieldOutList,
        null, 0,
        zenTokenOwnerKey, blockingStubFull, 0, ownerKeyString));

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long afterAssetBalance = PublicMethod.getAssetIssueValue(zenTokenOwnerAddress,
        PublicMethod.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
        blockingStubFull);
  Long afterNetUsed = PublicMethod.getAccountResource(zenTokenOwnerAddress, blockingStubFull)
        .getFreeNetUsed();
    Assert.assertTrue(beforeAssetBalance - afterAssetBalance == sendTokenAmount);
    logger.info("Before net:" + beforeNetUsed);
    logger.info("After net:" + afterNetUsed);
    Assert.assertEquals(beforeNetUsed, afterNetUsed);
  final Long afterBalance = PublicMethod
        .queryAccount(zenTokenOwnerAddress, blockingStubFull).getBalance();
    Assert.assertTrue(beforeBalance - afterBalance == multiSignFee);
    notes = PublicMethod.listShieldNote(shieldAddressInfo, blockingStubFull);
    note = notes.getNoteTxs(0).getNote();
  Long receiverShieldTokenAmount = note.getValue();
    Assert.assertTrue(receiverShieldTokenAmount == sendTokenAmount - zenTokenFee);
    Assert.assertEquals(memo, PublicMethod.getMemo(note));
  }

  @Test(enabled = false,
      description = "When from is shield,sign this transaction is forbidden", groups = {"daily", "shield"})
  public void test2ShieldFromShouldNotSign() {
    receiverAddressInfo = PublicMethod.generateShieldAddress();
    receiverAddress = shieldAddressInfo.get().getAddress();
    logger.info("receiver address:" + shieldAddress);

    notes = PublicMethod.listShieldNote(shieldAddressInfo, blockingStubFull);
    note = notes.getNoteTxs(0).getNote();

    shieldOutList.clear();
    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, receiverAddress,
        "" + (note.getValue() - zenTokenFee), memo);

    Assert.assertFalse(PublicMethodForMultiSign.sendShieldCoin(
        null, 321321,
        shieldAddressInfo.get(), notes.getNoteTxs(0),
        shieldOutList,
        null, 0,
        zenTokenOwnerKey, blockingStubFull, 0, ownerKeyString));

    Assert.assertFalse(PublicMethod.sendShieldCoin(
        null, 321321,
        shieldAddressInfo.get(), notes.getNoteTxs(0),
        shieldOutList,
        null, 0,
        zenTokenOwnerKey, blockingStubFull));

    Assert.assertFalse(PublicMethod.getSpendResult(shieldAddressInfo.get(),
        notes.getNoteTxs(0), blockingStubFull).getResult());


  }

  /**
   * constructor.
   */

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    PublicMethodForMultiSign.transferAsset(foundationZenTokenAddress, tokenId,
        PublicMethod.getAssetIssueValue(zenTokenOwnerAddress,
            PublicMethod.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
            blockingStubFull), zenTokenOwnerAddress,
        zenTokenOwnerKey, blockingStubFull, ownerKeyString);  }
}