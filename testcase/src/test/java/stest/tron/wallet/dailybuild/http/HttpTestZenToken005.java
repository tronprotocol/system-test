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
public class HttpTestZenToken005 {

  Optional<ShieldAddressInfo> sendShieldAddressInfo;
  Optional<ShieldAddressInfo> receiverShieldAddressInfo;
  String sendShieldAddress;
  String receiverShieldAddress;
  List<Note> shieldOutList = new ArrayList<>();
  String memo1;
  String memo2;
  ShieldNoteInfo sendNote;
  ShieldNoteInfo receiveNote;
  String assetIssueId;

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
  private Long zenTokenFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.zenTokenFee");
  private Long sendTokenAmount = 7 * zenTokenFee;
  private JSONObject responseContent;
  private HttpResponse response;

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(foundationZenTokenKey);
    PublicMethod.printAddress(zenTokenOwnerKey);
    response = HttpMethod
        .transferAsset(httpnode, foundationZenTokenAddress, zenTokenOwnerAddress, zenTokenId,
            sendTokenAmount, foundationZenTokenKey);
    org.junit.Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    //Args.setFullNodeAllowShieldedTransaction(true);
    sendShieldAddressInfo = HttpMethod.generateShieldAddress(httpnode);
    sendShieldAddress = sendShieldAddressInfo.get().getAddress();
    logger.info("sendShieldAddress:" + sendShieldAddress);
    memo1 = "Shield memo1 in " + System.currentTimeMillis();
    shieldOutList = HttpMethod.addShieldOutputList(httpnode, shieldOutList, sendShieldAddress,
        "" + (sendTokenAmount - zenTokenFee), memo1);

    response = HttpMethod
        .sendShieldCoin(httpnode, zenTokenOwnerAddress, sendTokenAmount, null, null, shieldOutList,
            null, 0, zenTokenOwnerKey);
    org.junit.Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);

    shieldOutList.clear();
    HttpMethod.waitToProduceOneBlock(httpnode);
    sendNote = HttpMethod.scanNoteByIvk(httpnode, sendShieldAddressInfo.get()).get(0);
  }

  @Test(enabled = false, description = "Shield to shield transaction without ask by http", groups = {"daily", "serial"})
  public void test01ShieldToShieldWithoutAskTransaction() {
    receiverShieldAddressInfo = HttpMethod.generateShieldAddress(httpnode);
    receiverShieldAddress = receiverShieldAddressInfo.get().getAddress();

    shieldOutList.clear();
    memo2 = "Send shield to receiver shield memo in" + System.currentTimeMillis();
    shieldOutList = HttpMethod.addShieldOutputList(httpnode, shieldOutList, receiverShieldAddress,
        "" + (sendNote.getValue() - zenTokenFee), memo2);

    HttpMethod.waitToProduceOneBlockFromSolidity(httpnode, httpSolidityNode);
    response = HttpMethod
        .sendShieldCoinWithoutAsk(httpnode, httpSolidityNode, httpPbftNode, null, 0,
            sendShieldAddressInfo.get(), sendNote, shieldOutList, null, 0, null);
    org.junit.Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    logger.info("response:" + response);
    responseContent = HttpMethod.parseResponseContent(response);
    logger.info("responseContent:" + responseContent);
    HttpMethod.printJsonContent(responseContent);
    HttpMethod.waitToProduceOneBlock(httpnode);

    receiveNote = HttpMethod.scanNoteByIvk(httpnode, receiverShieldAddressInfo.get()).get(0);

    Assert.assertTrue(receiveNote.getValue() == sendNote.getValue() - zenTokenFee);
    Assert.assertEquals(ByteArray.toHexString(memo2.getBytes()),
        ByteArray.toHexString(receiveNote.getMemo()));

    Assert.assertTrue(HttpMethod.getSpendResult(httpnode, sendShieldAddressInfo.get(), sendNote));
  }

  @Test(enabled = false, description = "Get merkle tree voucher info by http", groups = {"daily", "serial"})
  public void test02GetMerkleTreeVoucherInfo() {
    HttpMethod.waitToProduceOneBlock(httpnode);
    response = HttpMethod
        .getMerkleTreeVoucherInfo(httpnode, sendNote.getTrxId(), sendNote.getIndex(), 1);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.toJSONString().contains("tree"));
    Assert.assertTrue(responseContent.toJSONString().contains("rt"));
    Assert.assertTrue(responseContent.toJSONString().contains("paths"));

    response = HttpMethod
        .getMerkleTreeVoucherInfo(httpnode, receiveNote.getTrxId(), receiveNote.getIndex(), 1000);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.toJSONString().contains(
        "synBlockNum is too large, cmBlockNum plus synBlockNum must be <= latestBlockNumber"));
  }

  @Test(enabled = false, description = "Get merkle tree voucher info by http from solidity", groups = {"daily", "serial"})
  public void test03GetMerkleTreeVoucherInfoFromSolidity() {
    HttpMethod.waitToProduceOneBlock(httpnode);
    response = HttpMethod
        .getMerkleTreeVoucherInfoFromSolidity(httpSolidityNode, sendNote.getTrxId(),
            sendNote.getIndex(), 1);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.toJSONString().contains("tree"));
    Assert.assertTrue(responseContent.toJSONString().contains("rt"));
    Assert.assertTrue(responseContent.toJSONString().contains("paths"));

    response = HttpMethod
        .getMerkleTreeVoucherInfoFromSolidity(httpSolidityNode, receiveNote.getTrxId(),
            receiveNote.getIndex(), 1000);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.toJSONString().contains(
        "synBlockNum is too large, cmBlockNum plus synBlockNum must be <= latestBlockNumber"));
  }

  /**
   * constructor.
   */
  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    response = HttpMethod.getAccount(httpnode, foundationZenTokenAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    assetIssueId = responseContent.getString("asset_issued_ID");
    final Long assetBalance = HttpMethod
        .getAssetIssueValue(httpnode, zenTokenOwnerAddress, assetIssueId);
    HttpMethod
        .transferAsset(httpnode, zenTokenOwnerAddress, foundationZenTokenAddress, assetIssueId,
            assetBalance, zenTokenOwnerKey);
  }
}