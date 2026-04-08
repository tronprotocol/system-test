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
import stest.tron.wallet.common.client.utils.zen.address.DiversifierT;

@Slf4j
public class HttpTestZenToken001 {

  List<Note> shieldOutList = new ArrayList<>();
  Optional<ShieldAddressInfo> shieldAddressOptionalInfo1;
  Optional<ShieldAddressInfo> shieldAddressOptionalInfo2;
  Optional<ShieldAddressInfo> shieldAddressOptionalInfo3;
  ShieldAddressInfo shieldAddressInfo1 = new ShieldAddressInfo();
  ShieldAddressInfo shieldAddressInfo2 = new ShieldAddressInfo();
  ShieldAddressInfo shieldAddressInfo3 = new ShieldAddressInfo();
  String assetIssueId;
  ShieldNoteInfo shieldNote1;
  ShieldNoteInfo shieldNote2;
  ShieldNoteInfo shieldNote3;
  String memo;
  String sk;
  String d1;
  String d2;
  String d3;
  String ask;
  String nsk;
  String ovk;
  String ak;
  String nk;
  String ivk;
  String pkD1;
  String pkD2;
  String pkD3;
  String paymentAddress1;
  String paymentAddress2;
  String paymentAddress3;
  String rcm;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] zenTokenOwnerAddress = ecKey1.getAddress();
  String zenTokenOwnerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(0);
  private String httpSolidityNode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(2);
  private String httpPbftNode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(4);
  private String foundationZenTokenKey = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTokenOwnerKey");
  byte[] foundationZenTokenAddress = PublicMethod.getFinalAddress(foundationZenTokenKey);
  private String zenTokenId = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTokenId");
  private String tokenId = zenTokenId;
  private Long zenTokenFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.zenTokenFee");
  private Long costTokenAmount = 20 * zenTokenFee;
  private Long sendTokenAmount = 8 * zenTokenFee;
  private JSONObject responseContent;
  private HttpResponse response;

  /**
   * constructor.
   */
  @BeforeClass(enabled = false)
  public void beforeClass() {
    //Args.setFullNodeAllowShieldedTransaction(true);
    PublicMethod.printAddress(foundationZenTokenKey);
    PublicMethod.printAddress(zenTokenOwnerKey);
  }

  @Test(enabled = false, description = "Get spending key by http", groups = {"daily", "serial"})
  public void test01GetSpendingKey() {
    response = HttpMethod.getSpendingKey(httpnode);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    sk = responseContent.getString("value");
    logger.info("sk: " + sk);

  }

  @Test(enabled = false, description = "Get diversifier by http", groups = {"daily", "serial"})
  public void test02GetDiversifier() {
    response = HttpMethod.getDiversifier(httpnode);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    d1 = responseContent.getString("d");
    logger.info("d1: " + d1);

    response = HttpMethod.getDiversifier(httpnode);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    d2 = responseContent.getString("d");
    logger.info("d2: " + d2);

    response = HttpMethod.getDiversifier(httpnode);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    d3 = responseContent.getString("d");
    logger.info("d3: " + d3);
  }

  @Test(enabled = false, description = "Get expanded spending key by http", groups = {"daily", "serial"})
  public void test03GetExpandedSpendingKey() {
    response = HttpMethod.getExpandedSpendingKey(httpnode, sk);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    ask = responseContent.getString("ask");
    nsk = responseContent.getString("nsk");
    ovk = responseContent.getString("ovk");
    logger.info("ask: " + ask);
    logger.info("nsk: " + nsk);
    logger.info("ovk: " + ovk);
  }

  @Test(enabled = false, description = "Get AK from ASK by http", groups = {"daily", "serial"})
  public void test04GetAkFromAsk() {
    response = HttpMethod.getAkFromAsk(httpnode, ask);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    ak = responseContent.getString("value");
    logger.info("ak: " + ak);
  }

  @Test(enabled = false, description = "Get Nk from Nsk by http", groups = {"daily", "serial"})
  public void test05GetNkFromNsk() {
    response = HttpMethod.getNkFromNsk(httpnode, nsk);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    nk = responseContent.getString("value");
    logger.info("nk: " + nk);
  }

  @Test(enabled = false, description = "Get incoming viewing Key by http", groups = {"daily", "serial"})
  public void test06GetIncomingViewingKey() {
    response = HttpMethod.getIncomingViewingKey(httpnode, ak, nk);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    ivk = responseContent.getString("ivk");
    logger.info("ivk: " + ivk);
  }

  @Test(enabled = false, description = "Get Zen Payment Address by http", groups = {"daily", "serial"})
  public void test07GetZenPaymentAddress() {
    response = HttpMethod.getZenPaymentAddress(httpnode, ivk, d1);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    pkD1 = responseContent.getString("pkD");
    paymentAddress1 = responseContent.getString("payment_address");
    System.out.println("pkd1: " + pkD1);
    System.out.println("address1: " + paymentAddress1);
    shieldAddressInfo1.setSk(ByteArray.fromHexString(sk));
    shieldAddressInfo1.setD(new DiversifierT(ByteArray.fromHexString(d1)));
    shieldAddressInfo1.setIvk(ByteArray.fromHexString(ivk));
    shieldAddressInfo1.setOvk(ByteArray.fromHexString(ovk));
    shieldAddressInfo1.setPkD(ByteArray.fromHexString(pkD1));
    shieldAddressOptionalInfo1 = Optional.of(shieldAddressInfo1);

    response = HttpMethod.getZenPaymentAddress(httpnode, ivk, d2);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    pkD2 = responseContent.getString("pkD");
    paymentAddress2 = responseContent.getString("payment_address");
    System.out.println("pkd2: " + pkD2);
    System.out.println("address2: " + paymentAddress2);
    shieldAddressInfo2.setSk(ByteArray.fromHexString(sk));
    shieldAddressInfo2.setD(new DiversifierT(ByteArray.fromHexString(d2)));
    shieldAddressInfo2.setIvk(ByteArray.fromHexString(ivk));
    shieldAddressInfo2.setOvk(ByteArray.fromHexString(ovk));
    shieldAddressInfo2.setPkD(ByteArray.fromHexString(pkD2));
    shieldAddressOptionalInfo2 = Optional.of(shieldAddressInfo2);

    response = HttpMethod.getZenPaymentAddress(httpnode, ivk, d3);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    pkD3 = responseContent.getString("pkD");
    paymentAddress3 = responseContent.getString("payment_address");
    System.out.println("pkd3: " + pkD3);
    System.out.println("address3: " + paymentAddress3);
    shieldAddressInfo3.setSk(ByteArray.fromHexString(sk));
    shieldAddressInfo3.setD(new DiversifierT(ByteArray.fromHexString(d3)));
    shieldAddressInfo3.setIvk(ByteArray.fromHexString(ivk));
    shieldAddressInfo3.setOvk(ByteArray.fromHexString(ovk));
    shieldAddressInfo3.setPkD(ByteArray.fromHexString(pkD3));
    shieldAddressOptionalInfo3 = Optional.of(shieldAddressInfo3);
  }

  @Test(enabled = false, description = "Get rcm by http", groups = {"daily", "serial"})
  public void test08GetRcm() {
    response = HttpMethod.getRcm(httpnode);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    rcm = responseContent.getString("value");
    logger.info("rcm: " + rcm);
  }

  @Test(enabled = false, description = "Public to shield transaction withoutask by http", groups = {"daily", "serial"})
  public void test09PublicToShieldTransactionWithoutAsk() {
    response = HttpMethod
        .transferAsset(httpnode, foundationZenTokenAddress, zenTokenOwnerAddress, tokenId,
            costTokenAmount, foundationZenTokenKey);
    org.junit.Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);

    response = HttpMethod.getAccount(httpnode, foundationZenTokenAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    assetIssueId = responseContent.getString("asset_issued_ID");

    final Long beforeAssetBalance = HttpMethod
        .getAssetIssueValue(httpnode, zenTokenOwnerAddress, assetIssueId);
    response = HttpMethod.getAccountReource(httpnode, zenTokenOwnerAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    final Long beforeNetUsed = responseContent.getLong("freeNetUsed");

    String memo1 = "Shield memo11 in " + System.currentTimeMillis();
    String memo2 = "Shield memo22 in " + System.currentTimeMillis();
    Long sendSheldAddressAmount1 = zenTokenFee * 2;
    Long sendSheldAddressAmount2 = zenTokenFee * 3;
    Long sendAmount = sendSheldAddressAmount1 + sendSheldAddressAmount2 + zenTokenFee;
    shieldOutList.clear();
    shieldOutList = HttpMethod
        .addShieldOutputList(httpnode, shieldOutList, shieldAddressOptionalInfo1.get().getAddress(),
            "" + sendSheldAddressAmount1, memo1);
    shieldOutList = HttpMethod
        .addShieldOutputList(httpnode, shieldOutList, shieldAddressOptionalInfo2.get().getAddress(),
            "" + sendSheldAddressAmount2, memo2);
    HttpMethod.waitToProduceOneBlockFromSolidity(httpnode, httpSolidityNode);
    response = HttpMethod
        .sendShieldCoinWithoutAsk(httpnode, httpSolidityNode, httpPbftNode, zenTokenOwnerAddress,
            sendAmount, null, null, shieldOutList, null, 0, zenTokenOwnerKey);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);

    HttpMethod.waitToProduceOneBlock(httpnode);
    Long afterAssetBalance = HttpMethod
        .getAssetIssueValue(httpnode, zenTokenOwnerAddress, assetIssueId);

    response = HttpMethod.getAccountReource(httpnode, zenTokenOwnerAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    Long afterNetUsed = responseContent.getLong("freeNetUsed");

    Assert.assertTrue(beforeAssetBalance - afterAssetBalance == sendAmount);
    Assert.assertTrue(beforeNetUsed == afterNetUsed);

    String memo3 = "Shield memo33 in " + System.currentTimeMillis();
    Long sendSheldAddressAmount3 = costTokenAmount - sendAmount - zenTokenFee;
    shieldOutList.clear();
    shieldOutList = HttpMethod
        .addShieldOutputList(httpnode, shieldOutList, shieldAddressOptionalInfo3.get().getAddress(),
            "" + sendSheldAddressAmount3, memo3);
    HttpMethod.waitToProduceOneBlockFromSolidity(httpnode, httpSolidityNode);
    response = HttpMethod
        .sendShieldCoinWithoutAsk(httpnode, httpSolidityNode, httpPbftNode, zenTokenOwnerAddress,
            sendSheldAddressAmount3 + zenTokenFee, null, null, shieldOutList, null, 0,
            zenTokenOwnerKey);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    HttpMethod.waitToProduceOneBlock(httpnode);
//    HttpMethod.waitToProduceOneBlock(httpnode);
//    HttpMethod.waitToProduceOneBlock(httpnode);

    List<ShieldNoteInfo> shieldNoteInfoByIvkList = HttpMethod
        .scanNoteByIvk(httpnode, shieldAddressOptionalInfo1.get());
    logger.info("size are:" + shieldNoteInfoByIvkList.size());
    Assert.assertTrue(shieldNoteInfoByIvkList.size() == 3);
    List<ShieldNoteInfo> shieldNoteInfoByMarkList = HttpMethod
        .scanAndMarkNoteByIvk(httpnode, shieldAddressOptionalInfo2.get());
    Assert.assertTrue(shieldNoteInfoByMarkList.size() == 3);

    shieldNote1 = shieldNoteInfoByIvkList.get(0);
    shieldNote2 = shieldNoteInfoByIvkList.get(1);
    shieldNote3 = shieldNoteInfoByIvkList.get(2);
    Assert.assertTrue(shieldNote1.getValue() == sendSheldAddressAmount1);
    Assert.assertEquals(memo1.getBytes(), shieldNote1.getMemo());
    Assert.assertTrue(shieldNote2.getValue() == sendSheldAddressAmount2);
    Assert.assertEquals(memo2.getBytes(), shieldNote2.getMemo());
    Assert.assertTrue(shieldNote3.getValue() == sendSheldAddressAmount3);
    Assert.assertEquals(memo3.getBytes(), shieldNote3.getMemo());
    Assert.assertFalse(shieldNoteInfoByMarkList.get(0).getIsSpend());
    Assert.assertFalse(shieldNoteInfoByMarkList.get(1).getIsSpend());
    Assert.assertFalse(shieldNoteInfoByMarkList.get(2).getIsSpend());
  }

  @Test(enabled = false, description = "Shield to shield transaction withoutask by http", groups = {"daily", "serial"})
  public void test10ShieldToShieldTransactionWithoutAsk() {
    Optional<ShieldAddressInfo> receiverShieldAddressInfo1 = HttpMethod
        .generateShieldAddress(httpnode);
    String receiverShieldAddress1 = receiverShieldAddressInfo1.get().getAddress();
    logger.info("receiverShieldAddress1:" + receiverShieldAddress1);
    Optional<ShieldAddressInfo> receiverShieldAddressInfo2 = HttpMethod
        .generateShieldAddress(httpnode);
    String receiverShieldAddress2 = receiverShieldAddressInfo2.get().getAddress();
    logger.info("receiverShieldAddress2:" + receiverShieldAddress2);
    Optional<ShieldAddressInfo> receiverShieldAddressInfo3 = HttpMethod
        .generateShieldAddress(httpnode);
    String receiverShieldAddress3 = receiverShieldAddressInfo3.get().getAddress();
    logger.info("receiverShieldAddress3:" + receiverShieldAddress3);

    shieldOutList.clear();
    String receiverMemo1 = "Shield memo1 in " + System.currentTimeMillis();
    shieldOutList = HttpMethod.addShieldOutputList(httpnode, shieldOutList, receiverShieldAddress1,
        "" + (shieldNote1.getValue() - zenTokenFee), receiverMemo1);
    HttpMethod.waitToProduceOneBlockFromSolidity(httpnode, httpSolidityNode);
    response = HttpMethod
        .sendShieldCoinWithoutAsk(httpnode, httpSolidityNode, httpPbftNode, null, 0,
            shieldAddressOptionalInfo1.get(), shieldNote1, shieldOutList, null, 0, null);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    /*shieldOutList.clear();
    String receiverMemo2 = "Shield memo2 in " + System.currentTimeMillis();
    shieldOutList = HttpMethod
        .addShieldOutputList(httpnode, shieldOutList, receiverShieldAddress2,
            "" + (shieldNote2.getValue() - zenTokenFee), receiverMemo2);
    response = HttpMethod
        .sendShieldCoinWithoutAsk(httpnode, null, 0, shieldAddressOptionalInfo2.get(), shieldNote2,
            shieldOutList,
            null, 0, null);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);*/
    shieldOutList.clear();
    String receiverMemo3 = "Shield memo3 in " + System.currentTimeMillis();
    shieldOutList = HttpMethod.addShieldOutputList(httpnode, shieldOutList, receiverShieldAddress3,
        "" + (shieldNote3.getValue() - zenTokenFee), receiverMemo3);
    HttpMethod.waitToProduceOneBlockFromSolidity(httpnode, httpSolidityNode);
    response = HttpMethod
        .sendShieldCoinWithoutAsk(httpnode, httpSolidityNode, httpPbftNode, null, 0,
            shieldAddressOptionalInfo3.get(), shieldNote3, shieldOutList, null, 0, null);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    HttpMethod.waitToProduceOneBlock(httpnode);
//    HttpMethod.waitToProduceOneBlock(httpnode);

    List<ShieldNoteInfo> shieldNoteInfoByOvkList = HttpMethod
        .scanNoteByOvk(httpnode, shieldAddressOptionalInfo3.get());
    Assert.assertTrue(shieldNoteInfoByOvkList.size() == 2);
    List<ShieldNoteInfo> shieldNoteInfoByMarkList = HttpMethod
        .scanAndMarkNoteByIvk(httpnode, shieldAddressOptionalInfo2.get());
    Assert.assertTrue(shieldNoteInfoByMarkList.size() == 3);

    Assert.assertTrue(
        shieldNoteInfoByOvkList.get(0).getValue() == shieldNote1.getValue() - zenTokenFee);
    Assert.assertEquals(receiverMemo1.getBytes(), shieldNoteInfoByOvkList.get(0).getMemo());
    /*Assert.assertTrue(
        shieldNoteInfoByOvkList.get(1).getValue() == shieldNote2.getValue() - zenTokenFee);
    Assert.assertEquals(receiverMemo2.getBytes(), shieldNoteInfoByOvkList.get(1).getMemo());*/
    Assert.assertTrue(
        shieldNoteInfoByOvkList.get(1).getValue() == shieldNote3.getValue() - zenTokenFee);
    Assert.assertEquals(receiverMemo3.getBytes(), shieldNoteInfoByOvkList.get(1).getMemo());
    Assert.assertTrue(shieldNoteInfoByMarkList.get(0).getIsSpend());
    Assert.assertFalse(shieldNoteInfoByMarkList.get(1).getIsSpend());
    Assert.assertTrue(shieldNoteInfoByMarkList.get(2).getIsSpend());
  }

  /**
   * constructor.
   */
  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    final Long assetBalance = HttpMethod
        .getAssetIssueValue(httpnode, zenTokenOwnerAddress, assetIssueId);
    HttpMethod
        .transferAsset(httpnode, zenTokenOwnerAddress, foundationZenTokenAddress, assetIssueId,
            assetBalance, zenTokenOwnerKey);
  }
}