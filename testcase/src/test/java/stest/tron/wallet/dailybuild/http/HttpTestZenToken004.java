package stest.tron.wallet.dailybuild.http;

import com.alibaba.fastjson.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.Note;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.HttpMethod;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.ShieldAddressInfo;
import stest.tron.wallet.common.client.utils.ShieldNoteInfo;
import stest.tron.wallet.common.client.utils.Utils;

@Slf4j
public class HttpTestZenToken004 {

  Optional<ShieldAddressInfo> sendShieldAddressInfo;
  Optional<ShieldAddressInfo> receiverShieldAddressInfo1;
  Optional<ShieldAddressInfo> receiverShieldAddressInfo2;
  Optional<ShieldAddressInfo> receiverShieldAddressInfo3;
  Optional<ShieldAddressInfo> receiverShieldAddressInfo4;
  Optional<ShieldAddressInfo> receiverShieldAddressInfo5;
  String sendShieldAddress;
  String receiverShieldAddress1;
  String receiverShieldAddress2;
  String receiverShieldAddress3;
  String receiverShieldAddress4;
  String receiverShieldAddress5;
  List<Note> shieldOutList = new ArrayList<>();
  String memo1;
  String memo2;
  String memo3;
  String memo4;
  String memo5;
  ShieldNoteInfo sendNote;
  ShieldNoteInfo receiverNote1;
  ShieldNoteInfo receiverNote2;
  ShieldNoteInfo receiverNote3;
  ShieldNoteInfo receiverNote4;
  ShieldNoteInfo receiverNote5;
  String assetIssueId;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] receiverPublicAddress = ecKey1.getAddress();
  String receiverPublicKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(0);
  private String httpSolidityNode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(2);
  private String foundationZenTokenKey = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTokenOwnerKey");
  byte[] foundationZenTokenAddress = PublicMethod.getFinalAddress(foundationZenTokenKey);
  private Long zenTokenFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.zenTokenFee");
  private Long zenTokenWhenCreateNewAddress = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.zenTokenWhenCreateNewAddress");
  private Long sendTokenAmount = 18 * zenTokenFee;
  private JSONObject responseContent;
  private HttpResponse response;

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(foundationZenTokenKey);
    //Args.setFullNodeAllowShieldedTransaction(true);
  }

  @Test(enabled = false, description = "Shield to two shield transaction by http", groups = {"daily", "serial"})
  public void test01ShieldToTwoShieldTransaction() {
    sendShieldAddressInfo = HttpMethod.generateShieldAddress(httpnode);
    sendShieldAddress = sendShieldAddressInfo.get().getAddress();
    logger.info("sendShieldAddress:" + sendShieldAddress);
    String memo = "Shield memo in " + System.currentTimeMillis();
    shieldOutList = HttpMethod
        .addShieldOutputList(httpnode, shieldOutList, sendShieldAddress, "" + sendTokenAmount,
            memo);
    response = HttpMethod
        .sendShieldCoin(httpnode, foundationZenTokenAddress, sendTokenAmount + zenTokenFee, null,
            null, shieldOutList, null, 0, foundationZenTokenKey);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    HttpMethod.waitToProduceOneBlock(httpnode);
    sendNote = HttpMethod.scanNoteByIvk(httpnode, sendShieldAddressInfo.get()).get(0);

    receiverShieldAddressInfo1 = HttpMethod.generateShieldAddress(httpnode);
    receiverShieldAddressInfo2 = HttpMethod.generateShieldAddress(httpnode);
    receiverShieldAddress1 = receiverShieldAddressInfo1.get().getAddress();
    receiverShieldAddress2 = receiverShieldAddressInfo2.get().getAddress();
    logger.info("receiverShieldAddress1:" + receiverShieldAddress1);
    logger.info("receiverShieldAddress2:" + receiverShieldAddress2);
    memo1 = "Shield memo1 in " + System.currentTimeMillis();
    memo2 = "Shield memo2 in " + System.currentTimeMillis();
    Long sendToShiledAddress1Amount = 1 * zenTokenFee;
    Long sendToShiledAddress2Amount = sendTokenAmount - sendToShiledAddress1Amount - zenTokenFee;
    shieldOutList.clear();
    shieldOutList = HttpMethod.addShieldOutputList(httpnode, shieldOutList, receiverShieldAddress1,
        "" + sendToShiledAddress1Amount, memo1);
    shieldOutList = HttpMethod.addShieldOutputList(httpnode, shieldOutList, receiverShieldAddress2,
        "" + sendToShiledAddress2Amount, memo2);

    response = HttpMethod
        .sendShieldCoin(httpnode, null, 0, sendShieldAddressInfo.get(), sendNote, shieldOutList,
            null, 0, null);
    org.junit.Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);

    HttpMethod.waitToProduceOneBlock(httpnode);

    receiverNote1 = HttpMethod.scanNoteByIvk(httpnode, receiverShieldAddressInfo1.get()).get(0);
    receiverNote2 = HttpMethod.scanNoteByIvk(httpnode, receiverShieldAddressInfo2.get()).get(0);
    Assert.assertTrue(receiverNote1.getValue() == sendToShiledAddress1Amount);
    Assert.assertTrue(receiverNote2.getValue() == sendToShiledAddress2Amount);
    Assert.assertEquals(memo1.getBytes(), receiverNote1.getMemo());
    Assert.assertEquals(memo2.getBytes(), receiverNote2.getMemo());

    Assert.assertTrue(HttpMethod.getSpendResult(httpnode, sendShieldAddressInfo.get(), sendNote));
  }

  @Test(enabled = false, description = "Shield to one public and one shield transaction by http", groups = {"daily", "serial"})
  public void test02ShieldToOnePublicAndOneShieldTransaction() {
    sendShieldAddressInfo = HttpMethod.generateShieldAddress(httpnode);
    sendShieldAddress = sendShieldAddressInfo.get().getAddress();
    logger.info("sendShieldAddress:" + sendShieldAddress);
    String memo = "Shield memo in " + System.currentTimeMillis();
    shieldOutList.clear();
    shieldOutList = HttpMethod
        .addShieldOutputList(httpnode, shieldOutList, sendShieldAddress, "" + sendTokenAmount,
            memo);
    response = HttpMethod
        .sendShieldCoin(httpnode, foundationZenTokenAddress, sendTokenAmount + zenTokenFee, null,
            null, shieldOutList, null, 0, foundationZenTokenKey);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    HttpMethod.waitToProduceOneBlock(httpnode);
    sendNote = HttpMethod.scanNoteByIvk(httpnode, sendShieldAddressInfo.get()).get(0);

    receiverShieldAddressInfo3 = HttpMethod.generateShieldAddress(httpnode);
    receiverShieldAddress3 = receiverShieldAddressInfo3.get().getAddress();

    response = HttpMethod.getAccount(httpnode, foundationZenTokenAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    assetIssueId = responseContent.getString("asset_issued_ID");

    final Long beforeAssetBalance = HttpMethod
        .getAssetIssueValue(httpnode, receiverPublicAddress, assetIssueId);
    response = HttpMethod.getAccountReource(httpnode, receiverPublicAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    final Long beforeNetUsed = responseContent.getLong("freeNetUsed");

    shieldOutList.clear();
    Long sendToPublicAddressAmount = 1 * zenTokenFee;
    Long sendToShiledAddressAmount =
        sendTokenAmount - sendToPublicAddressAmount - zenTokenWhenCreateNewAddress;
    memo3 = "Send shield to receiver shield memo in" + System.currentTimeMillis();
    shieldOutList = HttpMethod.addShieldOutputList(httpnode, shieldOutList, receiverShieldAddress3,
        "" + sendToShiledAddressAmount, memo3);

    PublicMethod.printAddress(receiverPublicKey);
    response = HttpMethod
        .sendShieldCoin(httpnode, null, 0, sendShieldAddressInfo.get(), sendNote, shieldOutList,
            receiverPublicAddress, sendToPublicAddressAmount, null);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    HttpMethod.waitToProduceOneBlock(httpnode);

    final Long afterAssetBalance = HttpMethod
        .getAssetIssueValue(httpnode, receiverPublicAddress, assetIssueId);
    response = HttpMethod.getAccountReource(httpnode, receiverPublicAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    final Long afterNetUsed = responseContent.getLong("freeNetUsed");

    Assert.assertTrue(afterAssetBalance - beforeAssetBalance == sendToPublicAddressAmount);
    Assert.assertTrue(beforeNetUsed == afterNetUsed);

    receiverNote3 = HttpMethod.scanNoteByIvk(httpnode, receiverShieldAddressInfo3.get()).get(0);
    Assert.assertTrue(receiverNote3.getValue() == sendToShiledAddressAmount);
    Assert.assertEquals(memo3.getBytes(), receiverNote3.getMemo());

    Assert.assertTrue(HttpMethod.getSpendResult(httpnode, sendShieldAddressInfo.get(), sendNote));

    HttpMethod.waitToProduceOneBlockFromSolidity(httpnode, httpSolidityNode);
    Assert.assertTrue(HttpMethod
        .getSpendResultFromSolidity(httpnode, httpSolidityNode, sendShieldAddressInfo.get(),
            sendNote));
    Assert.assertFalse(HttpMethod
        .getSpendResultFromSolidity(httpnode, httpSolidityNode, receiverShieldAddressInfo3.get(),
            receiverNote3));

    Assert.assertTrue(
        HttpMethod.scanAndMarkNoteByIvk(httpnode, sendShieldAddressInfo.get()).get(0).getIsSpend());
    Assert.assertFalse(
        HttpMethod.scanAndMarkNoteByIvk(httpnode, receiverShieldAddressInfo3.get()).get(0)
            .getIsSpend());
  }

  @Test(enabled = false, description = "Shield to one public and two shield transaction by http", groups = {"daily", "serial"})
  public void test03ShieldToOnePublicAndTwoShieldTransaction() {
    sendShieldAddressInfo = HttpMethod.generateShieldAddress(httpnode);
    sendShieldAddress = sendShieldAddressInfo.get().getAddress();
    logger.info("sendShieldAddress:" + sendShieldAddress);
    String memo = "Shield memo in " + System.currentTimeMillis();
    shieldOutList.clear();
    shieldOutList = HttpMethod
        .addShieldOutputList(httpnode, shieldOutList, sendShieldAddress, "" + sendTokenAmount,
            memo);
    response = HttpMethod
        .sendShieldCoin(httpnode, foundationZenTokenAddress, sendTokenAmount + zenTokenFee, null,
            null, shieldOutList, null, 0, foundationZenTokenKey);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    HttpMethod.waitToProduceOneBlock(httpnode);
    sendNote = HttpMethod.scanNoteByIvk(httpnode, sendShieldAddressInfo.get()).get(0);

    receiverShieldAddressInfo4 = HttpMethod.generateShieldAddress(httpnode);
    receiverShieldAddress4 = receiverShieldAddressInfo4.get().getAddress();
    receiverShieldAddressInfo5 = HttpMethod.generateShieldAddress(httpnode);
    receiverShieldAddress5 = receiverShieldAddressInfo5.get().getAddress();

    final Long beforeAssetBalance = HttpMethod
        .getAssetIssueValue(httpnode, receiverPublicAddress, assetIssueId);
    response = HttpMethod.getAccountReource(httpnode, receiverPublicAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    final Long beforeNetUsed = responseContent.getLong("freeNetUsed");

    shieldOutList.clear();
    Long sendToPublicAddressAmount = 1 * zenTokenFee;
    Long sendToShiledAddress1Amount = 2 * zenTokenFee;
    Long sendToShiledAddress2Amount =
        sendTokenAmount - sendToPublicAddressAmount - sendToShiledAddress1Amount - zenTokenFee;
    memo4 = "Send shield to receiver shield memo in" + System.currentTimeMillis();
    memo5 = "Send shield to receiver shield memo in" + System.currentTimeMillis();
    shieldOutList = HttpMethod.addShieldOutputList(httpnode, shieldOutList, receiverShieldAddress4,
        "" + sendToShiledAddress1Amount, memo4);
    shieldOutList = HttpMethod.addShieldOutputList(httpnode, shieldOutList, receiverShieldAddress5,
        "" + sendToShiledAddress2Amount, memo5);

    PublicMethod.printAddress(receiverPublicKey);
    response = HttpMethod
        .sendShieldCoin(httpnode, null, 0, sendShieldAddressInfo.get(), sendNote, shieldOutList,
            receiverPublicAddress, sendToPublicAddressAmount, null);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    HttpMethod.waitToProduceOneBlock(httpnode);

    final Long afterAssetBalance = HttpMethod
        .getAssetIssueValue(httpnode, receiverPublicAddress, assetIssueId);
    response = HttpMethod.getAccountReource(httpnode, receiverPublicAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    final Long afterNetUsed = responseContent.getLong("freeNetUsed");

    Assert.assertTrue(afterAssetBalance - beforeAssetBalance == sendToPublicAddressAmount);
    Assert.assertTrue(beforeNetUsed == afterNetUsed);

    receiverNote4 = HttpMethod.scanNoteByIvk(httpnode, receiverShieldAddressInfo4.get()).get(0);
    Assert.assertTrue(receiverNote4.getValue() == sendToShiledAddress1Amount);
    Assert.assertEquals(memo4.getBytes(), receiverNote4.getMemo());

    receiverNote5 = HttpMethod.scanNoteByIvk(httpnode, receiverShieldAddressInfo5.get()).get(0);
    Assert.assertTrue(receiverNote5.getValue() == sendToShiledAddress2Amount);
    Assert.assertEquals(memo5.getBytes(), receiverNote5.getMemo());

    Assert.assertTrue(HttpMethod.getSpendResult(httpnode, sendShieldAddressInfo.get(), sendNote));
  }

  /**
   * constructor.
   */
  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    final Long assetBalance = HttpMethod
        .getAssetIssueValue(httpnode, receiverPublicAddress, assetIssueId);
    HttpMethod
        .transferAsset(httpnode, receiverPublicAddress, foundationZenTokenAddress, assetIssueId,
            assetBalance, receiverPublicKey);
  }
}