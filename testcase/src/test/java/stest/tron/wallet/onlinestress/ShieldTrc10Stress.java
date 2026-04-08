package stest.tron.wallet.onlinestress;

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
public class ShieldTrc10Stress extends TronBaseTest {  List<Note> shieldOutList = new ArrayList<>();
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
  private Long costTokenAmount = 20000 * zenTokenFee;
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
    //Args.setFullNodeAllowShieldedTransaction(true);
    PublicMethod.printAddress(foundationZenTokenKey);
    PublicMethod.printAddress(zenTokenOwnerKey);
    sendShieldAddressInfo = PublicMethod.generateShieldAddress();
    sendshieldAddress = sendShieldAddressInfo.get().getAddress();
  String memo = "Use to TestZenToken004 shield address";
    List<Note> shieldOutList = new ArrayList<>();
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
    PublicMethod.waitProduceNextBlock(blockingStubFull);


  }

  @Test(enabled = true, threadPoolSize = 100, invocationCount = 100, groups = {"stress"})
  public void test1Shield2TwoShieldTransaction() {
    DecryptNotes notes;
    List<Note> shieldOutList = new ArrayList<>();

    Integer times = 100;
    while (times-- > 0) {
      notes = PublicMethod.listShieldNote(sendShieldAddressInfo, blockingStubFull);
  //logger.info("note size:" + notes.getNoteTxsCount());
  String memo1 = "Shield to  shield address1 transaction" + System.currentTimeMillis();
      shieldOutList.clear();
  Long sendToShiledAddress1Amount =
          notes.getNoteTxs(notes.getNoteTxsCount() - 1).getNote().getValue() - zenTokenFee;
      shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, sendshieldAddress,
          "" + sendToShiledAddress1Amount, memo1);

      try {
        PublicMethod.sendShieldCoin(
            null, 0,
            sendShieldAddressInfo.get(), notes.getNoteTxs(notes.getNoteTxsCount() - 1),
            shieldOutList,
            null, 0,
            zenTokenOwnerKey, blockingStubFull);
      } catch (Exception e) {
        throw e;
      }
    }

  }

  @AfterClass(enabled = false)
  public void shutdown() throws InterruptedException {
    PublicMethod.transferAsset(foundationZenTokenAddress, tokenId,
        PublicMethod.getAssetIssueValue(zenTokenOwnerAddress,
            PublicMethod.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
            blockingStubFull), zenTokenOwnerAddress, zenTokenOwnerKey, blockingStubFull);  }
}