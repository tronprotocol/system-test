package stest.tron.wallet.onlinestress;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.HttpMethod;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;

@Slf4j
public class HttpStressTest {

  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = 10000000000000000L;
  static Integer connectionTimeout = Configuration.getByPath("testng.conf")
      .getInt("defaultParameter.httpConnectionTimeout");
  static Integer soTimeout = Configuration.getByPath("testng.conf")
      .getInt("defaultParameter.httpSoTimeout");
  private static String name = "testAssetIssue002_" + Long.toString(now);
  private static String assetIssueId1;
  private static String assetIssueId2;
  private static Integer exchangeId;
  private static Long beforeInjectBalance;
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethod.getFinalAddress(testKey002);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] exchangeOwnerAddress = ecKey1.getAddress();
  String exchangeOwnerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] asset2Address = ecKey2.getAddress();
  String asset2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  Long amount = 2048000000L;
  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");
  private JSONObject responseContent;
  private HttpResponse response;
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(0);

  /**
   * constructor.
   */
  @Test(enabled = true, threadPoolSize = 10, invocationCount = 10, groups = {"stress"})
  public void test4InjectExchange() {
    final long now = System.currentTimeMillis();
    final long totalSupply = 10000000000000000L;
    Long beforeInjectBalance;
    HttpClient httpClient = new DefaultHttpClient();
    HttpPost httppost;
    httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT,
        connectionTimeout);
    httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, soTimeout);
    httppost = new HttpPost(url);
    httppost.setHeader("Content-type", "application/json; charset=utf-8");
    httppost.setHeader("Connection", "Close");

    response = HttpMethod
        .sendCoin(httpnode, fromAddress, exchangeOwnerAddress, amount, testKey002);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    response = HttpMethod.sendCoin(httpnode, fromAddress, asset2Address, amount, testKey002);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    //Create an asset issue
    response = HttpMethod.assetIssue(httpnode, exchangeOwnerAddress, name, name, totalSupply, 1, 1,
        System.currentTimeMillis() + 5000, System.currentTimeMillis() + 50000000,
        2, 3, description, url, 1000L, 1000L, exchangeOwnerKey);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    response = HttpMethod.assetIssue(httpnode, asset2Address, name, name, totalSupply, 1, 1,
        System.currentTimeMillis() + 5000, System.currentTimeMillis() + 50000000,
        2, 3, description, url, 1000L, 1000L, asset2Key);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    response = HttpMethod.getAccount(httpnode, exchangeOwnerAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    assetIssueId1 = responseContent.getString("asset_issued_ID");
    Assert.assertTrue(Integer.parseInt(assetIssueId1) > 1000000);
    HttpMethod.waitToProduceOneBlock(httpnode);
    response = HttpMethod.getAccount(httpnode, asset2Address);
    responseContent = HttpMethod.parseResponseContent(response);
    assetIssueId2 = responseContent.getString("asset_issued_ID");
    Assert.assertTrue(Integer.parseInt(assetIssueId2) > 1000000);

    response = HttpMethod
        .transferAsset(httpnode, asset2Address, exchangeOwnerAddress, assetIssueId2,
            100000000000000L, asset2Key);
    Assert.assertTrue(HttpMethod.verificationResult(response));

    HttpMethod.waitToProduceOneBlock(httpnode);
    HttpMethod.waitToProduceOneBlock(httpnode);

    //Create exchange.
    response = HttpMethod.exchangeCreate(httpnode, exchangeOwnerAddress, assetIssueId1,
        50000000000000L, assetIssueId2, 50000000000000L, exchangeOwnerKey);
    Assert.assertTrue(HttpMethod.verificationResult(response));

    HttpMethod.waitToProduceOneBlock(httpnode);
    HttpMethod.waitToProduceOneBlock(httpnode);
    response = HttpMethod.listExchanges(httpnode);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    JSONArray jsonArray = JSONArray.parseArray(responseContent.getString("exchanges"));
    Assert.assertTrue(jsonArray.size() >= 1);
    exchangeId = jsonArray.size();

    response = HttpMethod.getExchangeById(httpnode, exchangeId);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);

    Integer times = 0;
    while (times++ <= 10000) {
      HttpMethod.sendCoin(httpnode, fromAddress, exchangeOwnerAddress, 100L, testKey002);
      HttpMethod.sendCoin(httpnode, fromAddress, asset2Address, 100L, testKey002);
      //Inject exchange.
      HttpMethod.exchangeInject(httpnode, exchangeOwnerAddress, exchangeId, assetIssueId1,
          10L, exchangeOwnerKey);
      HttpMethod.exchangeWithdraw(httpnode, exchangeOwnerAddress, exchangeId, assetIssueId1,
          10L, exchangeOwnerKey);
      HttpMethod.exchangeTransaction(httpnode, exchangeOwnerAddress, exchangeId, assetIssueId1,
          100L, 1L, exchangeOwnerKey);
      HttpMethod.exchangeTransaction(httpnode, exchangeOwnerAddress, exchangeId, assetIssueId2,
          100L, 1L, exchangeOwnerKey);
      HttpMethod.transferAsset(httpnode, asset2Address, exchangeOwnerAddress, assetIssueId2,
          1L, asset2Key);
      HttpMethod.transferAsset(httpnode, exchangeOwnerAddress, asset2Address, assetIssueId1,
          1L, exchangeOwnerKey);
      HttpMethod.participateAssetIssue(httpnode, exchangeOwnerAddress, asset2Address,
          assetIssueId1, 1L, asset2Key);
      HttpMethod.participateAssetIssue(httpnode, asset2Address, exchangeOwnerAddress,
          assetIssueId2, 1L, exchangeOwnerKey);
      HttpMethod.freezeBalance(httpnode, fromAddress, 10000000000L, 0, 0,
          exchangeOwnerAddress, testKey002);
      HttpMethod.freezeBalance(httpnode, fromAddress, 10000000000L, 0, 1,
          exchangeOwnerAddress, testKey002);
      HttpMethod.unFreezeBalance(httpnode, fromAddress, 10000000000L,0, exchangeOwnerAddress, testKey002);
      HttpMethod.unFreezeBalance(httpnode, fromAddress, 10000000000L,1, exchangeOwnerAddress, testKey002);
    }
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethod.disConnect();
  }
}
