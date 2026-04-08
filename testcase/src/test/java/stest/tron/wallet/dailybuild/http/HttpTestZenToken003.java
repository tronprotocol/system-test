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
public class HttpTestZenToken003 {

  Optional<ShieldAddressInfo> receiverShieldAddressInfo1;
  Optional<ShieldAddressInfo> receiverShieldAddressInfo2;
  Optional<ShieldAddressInfo> receiverShieldAddressInfo3;
  Optional<ShieldAddressInfo> receiverShieldAddressInfo4;
  Optional<ShieldAddressInfo> receiverShieldAddressInfo5;
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
  ShieldNoteInfo receiverNote1;
  ShieldNoteInfo receiverNote2;
  ShieldNoteInfo receiverNote3;
  ShieldNoteInfo receiverNote4;
  ShieldNoteInfo receiverNote5;
  String assetIssueId;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] zenTokenOwnerAddress = ecKey1.getAddress();
  String zenTokenOwnerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] receiverPublicAddress = ecKey2.getAddress();
  String receiverPublicKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(0);
  private String foundationZenTokenKey = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTokenOwnerKey");
  byte[] foundationZenTokenAddress = PublicMethod.getFinalAddress(foundationZenTokenKey);
  private String zenTokenId = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTokenId");
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
    PublicMethod.printAddress(zenTokenOwnerKey);
    //Args.setFullNodeAllowShieldedTransaction(true);

  }

  @Test(enabled = false, description = "Public to two shield transaction by http", groups = {"daily", "serial"})
  public void test01PublicToTwoShieldTransaction() {
    response = HttpMethod
        .transferAsset(httpnode, foundationZenTokenAddress, zenTokenOwnerAddress, zenTokenId,
            sendTokenAmount, foundationZenTokenKey);
    HttpMethod.waitToProduceOneBlock(httpnode);

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
    shieldOutList = HttpMethod.addShieldOutputList(httpnode, shieldOutList, receiverShieldAddress1,
        "" + sendToShiledAddress1Amount, memo1);
    shieldOutList = HttpMethod.addShieldOutputList(httpnode, shieldOutList, receiverShieldAddress2,
        "" + sendToShiledAddress2Amount, memo2);

    response = HttpMethod.getAccount(httpnode, foundationZenTokenAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    assetIssueId = responseContent.getString("asset_issued_ID");
    final Long beforeAssetBalance = HttpMethod
        .getAssetIssueValue(httpnode, zenTokenOwnerAddress, assetIssueId);

    response = HttpMethod.getAccountReource(httpnode, zenTokenOwnerAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    final Long beforeNetUsed = responseContent.getLong("freeNetUsed");

    response = HttpMethod
        .sendShieldCoin(httpnode, zenTokenOwnerAddress, sendTokenAmount, null, null, shieldOutList,
            null, 0, zenTokenOwnerKey);
    org.junit.Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);

    HttpMethod.waitToProduceOneBlock(httpnode);
    Long afterAssetBalance = HttpMethod
        .getAssetIssueValue(httpnode, zenTokenOwnerAddress, assetIssueId);
    response = HttpMethod.getAccountReource(httpnode, zenTokenOwnerAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    Long afterNetUsed = responseContent.getLong("freeNetUsed");
    Assert.assertTrue(beforeAssetBalance - afterAssetBalance == sendTokenAmount);
    Assert.assertTrue(beforeNetUsed == afterNetUsed);

    receiverNote1 = HttpMethod.scanNoteByIvk(httpnode, receiverShieldAddressInfo1.get()).get(0);
    receiverNote2 = HttpMethod.scanNoteByIvk(httpnode, receiverShieldAddressInfo2.get()).get(0);
    Assert.assertTrue(receiverNote1.getValue() == sendToShiledAddress1Amount);
    Assert.assertTrue(receiverNote2.getValue() == sendToShiledAddress2Amount);
    Assert.assertEquals(memo1.getBytes(), receiverNote1.getMemo());
    Assert.assertEquals(memo2.getBytes(), receiverNote2.getMemo());
  }

  @Test(enabled = false, description = "Public to one public and one shield transaction by http", groups = {"daily", "serial"})
  public void test02ShieldToOneShieldAndOnePublicTransaction() {
    response = HttpMethod
        .transferAsset(httpnode, foundationZenTokenAddress, zenTokenOwnerAddress, zenTokenId,
            sendTokenAmount, foundationZenTokenKey);
    HttpMethod.waitToProduceOneBlock(httpnode);

    receiverShieldAddressInfo3 = HttpMethod.generateShieldAddress(httpnode);
    receiverShieldAddress3 = receiverShieldAddressInfo3.get().getAddress();

    final Long beforeAssetBalanceSendAddress = HttpMethod
        .getAssetIssueValue(httpnode, zenTokenOwnerAddress, assetIssueId);
    response = HttpMethod.getAccountReource(httpnode, zenTokenOwnerAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    final Long beforeNetUsedSendAddress = responseContent.getLong("freeNetUsed");
    response = HttpMethod.getAccount(httpnode, zenTokenOwnerAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    Long beforeBalanceSendAddress = responseContent.getLong("balance");

    final Long beforeAssetBalanceReceiverAddress = HttpMethod
        .getAssetIssueValue(httpnode, receiverPublicAddress, assetIssueId);
    response = HttpMethod.getAccountReource(httpnode, receiverPublicAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    final Long beforeNetUsedReceiverAddress = responseContent.getLong("freeNetUsed");

    shieldOutList.clear();
    Long sendToPublicAddressAmount = 1 * zenTokenFee;
    Long sendToShiledAddressAmount =
        sendTokenAmount - sendToPublicAddressAmount - zenTokenWhenCreateNewAddress;
    memo3 = "Send shield to receiver shield memo in" + System.currentTimeMillis();
    shieldOutList = HttpMethod.addShieldOutputList(httpnode, shieldOutList, receiverShieldAddress3,
        "" + sendToShiledAddressAmount, memo3);

    PublicMethod.printAddress(receiverPublicKey);
    response = HttpMethod
        .sendShieldCoin(httpnode, zenTokenOwnerAddress, sendTokenAmount, null, null, shieldOutList,
            receiverPublicAddress, sendToPublicAddressAmount, zenTokenOwnerKey);
    org.junit.Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    HttpMethod.waitToProduceOneBlock(httpnode);

    Long afterAssetBalanceSendAddress = HttpMethod
        .getAssetIssueValue(httpnode, zenTokenOwnerAddress, assetIssueId);
    response = HttpMethod.getAccountReource(httpnode, zenTokenOwnerAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    Long afterNetUsedSendAddress = responseContent.getLong("freeNetUsed");
    response = HttpMethod.getAccount(httpnode, zenTokenOwnerAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    Long afterBalanceSendAddress = responseContent.getLong("balance");

    final Long afterAssetBalanceReceiverAddress = HttpMethod
        .getAssetIssueValue(httpnode, receiverPublicAddress, assetIssueId);
    response = HttpMethod.getAccountReource(httpnode, receiverPublicAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    final Long afterNetUsedReceiverAddress = responseContent.getLong("freeNetUsed");

    Assert.assertTrue(
        beforeAssetBalanceSendAddress - afterAssetBalanceSendAddress == sendTokenAmount);
    Assert.assertTrue(beforeNetUsedSendAddress == afterNetUsedSendAddress);
    Assert.assertTrue(beforeBalanceSendAddress == afterBalanceSendAddress);

    Assert.assertTrue(afterAssetBalanceReceiverAddress - beforeAssetBalanceReceiverAddress
        == sendToPublicAddressAmount);
    Assert.assertTrue(beforeNetUsedReceiverAddress == afterNetUsedReceiverAddress);

    receiverNote3 = HttpMethod.scanNoteByIvk(httpnode, receiverShieldAddressInfo3.get()).get(0);

    Assert.assertTrue(receiverNote3.getValue() == sendToShiledAddressAmount);
    Assert.assertEquals(memo3.getBytes(), receiverNote3.getMemo());
  }

  @Test(enabled = false, description = "Public to one public and two shield transaction by http", groups = {"daily", "serial"})
  public void test03ShieldToOneShieldAndTwoPublicTransaction() {
    response = HttpMethod
        .transferAsset(httpnode, foundationZenTokenAddress, zenTokenOwnerAddress, zenTokenId,
            sendTokenAmount, foundationZenTokenKey);
    HttpMethod.waitToProduceOneBlock(httpnode);

    receiverShieldAddressInfo4 = HttpMethod.generateShieldAddress(httpnode);
    receiverShieldAddress4 = receiverShieldAddressInfo4.get().getAddress();
    receiverShieldAddressInfo5 = HttpMethod.generateShieldAddress(httpnode);
    receiverShieldAddress5 = receiverShieldAddressInfo5.get().getAddress();

    final Long beforeAssetBalanceSendAddress = HttpMethod
        .getAssetIssueValue(httpnode, zenTokenOwnerAddress, assetIssueId);
    response = HttpMethod.getAccountReource(httpnode, zenTokenOwnerAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    final Long beforeNetUsedSendAddress = responseContent.getLong("freeNetUsed");
    response = HttpMethod.getAccount(httpnode, zenTokenOwnerAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    Long beforeBalanceSendAddress = responseContent.getLong("balance");

    final Long beforeAssetBalanceReceiverAddress = HttpMethod
        .getAssetIssueValue(httpnode, receiverPublicAddress, assetIssueId);
    response = HttpMethod.getAccountReource(httpnode, receiverPublicAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    final Long beforeNetUsedReceiverAddress = responseContent.getLong("freeNetUsed");

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
        .sendShieldCoin(httpnode, zenTokenOwnerAddress, sendTokenAmount, null, null, shieldOutList,
            receiverPublicAddress, sendToPublicAddressAmount, zenTokenOwnerKey);
    org.junit.Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    HttpMethod.waitToProduceOneBlock(httpnode);

    Long afterAssetBalanceSendAddress = HttpMethod
        .getAssetIssueValue(httpnode, zenTokenOwnerAddress, assetIssueId);
    response = HttpMethod.getAccountReource(httpnode, zenTokenOwnerAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    Long afterNetUsedSendAddress = responseContent.getLong("freeNetUsed");
    response = HttpMethod.getAccount(httpnode, zenTokenOwnerAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    Long afterBalanceSendAddress = responseContent.getLong("balance");

    final Long afterAssetBalanceReceiverAddress = HttpMethod
        .getAssetIssueValue(httpnode, receiverPublicAddress, assetIssueId);
    response = HttpMethod.getAccountReource(httpnode, receiverPublicAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    final Long afterNetUsedReceiverAddress = responseContent.getLong("freeNetUsed");

    Assert.assertTrue(
        beforeAssetBalanceSendAddress - afterAssetBalanceSendAddress == sendTokenAmount);
    Assert.assertTrue(beforeNetUsedSendAddress == afterNetUsedSendAddress);
    Assert.assertTrue(beforeBalanceSendAddress == afterBalanceSendAddress);

    Assert.assertTrue(afterAssetBalanceReceiverAddress - beforeAssetBalanceReceiverAddress
        == sendToPublicAddressAmount);
    Assert.assertTrue(beforeNetUsedReceiverAddress == afterNetUsedReceiverAddress);

    receiverNote4 = HttpMethod.scanNoteByIvk(httpnode, receiverShieldAddressInfo4.get()).get(0);
    Assert.assertTrue(receiverNote4.getValue() == sendToShiledAddress1Amount);
    Assert.assertEquals(memo4.getBytes(), receiverNote4.getMemo());

    receiverNote5 = HttpMethod.scanNoteByIvk(httpnode, receiverShieldAddressInfo5.get()).get(0);
    Assert.assertTrue(receiverNote5.getValue() == sendToShiledAddress2Amount);
    Assert.assertEquals(memo5.getBytes(), receiverNote5.getMemo());
  }

  /**
   * constructor.
   */
  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    final Long assetBalance1 = HttpMethod
        .getAssetIssueValue(httpnode, zenTokenOwnerAddress, assetIssueId);
    HttpMethod
        .transferAsset(httpnode, zenTokenOwnerAddress, foundationZenTokenAddress, assetIssueId,
            assetBalance1, zenTokenOwnerKey);

    final Long assetBalance2 = HttpMethod
        .getAssetIssueValue(httpnode, receiverPublicAddress, assetIssueId);
    HttpMethod
        .transferAsset(httpnode, receiverPublicAddress, foundationZenTokenAddress, assetIssueId,
            assetBalance2, receiverPublicKey);
  }
}