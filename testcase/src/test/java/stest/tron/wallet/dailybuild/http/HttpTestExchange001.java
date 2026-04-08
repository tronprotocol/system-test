package stest.tron.wallet.dailybuild.http;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
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
public class HttpTestExchange001 {

  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = now;
  private static String name = "testAssetIssue002_" + now;
  private static String assetIssueId1;
  private static String assetIssueId2;
  private static Integer exchangeId;
  private static Long beforeInjectBalance;
  private static Long afterInjectBalance;
  private static Long beforeWithdrawBalance;
  private static Long afterWithdrawBalance;
  private static Long beforeTransactionBalance;
  private static Long afterTransactionBalance;
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
  String url = Configuration.getByPath("testng.conf").getString("defaultParameter.assetUrl");
  private JSONObject responseContent;
  private HttpResponse response;
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(1);
  private String httpSoliditynode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(2);
  private String httpPbftNode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(4);


  /**
   * constructor.
   */
  @Test(enabled = false, description = "Create asset issue by http", groups = {"daily", "serial"})
  public void test01CreateExchange() {
    response = HttpMethod
        .sendCoin(httpnode, fromAddress, exchangeOwnerAddress, 2048000000L, testKey002);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    response = HttpMethod.sendCoin(httpnode, fromAddress, asset2Address, amount, testKey002);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);

    //Create an asset issue
    response = HttpMethod.assetIssue(httpnode, exchangeOwnerAddress, name, name, totalSupply, 1, 1,
        System.currentTimeMillis() + 5000, System.currentTimeMillis() + 50000000, 2, 3, description,
        url, 1000L, 1000L, exchangeOwnerKey);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    response = HttpMethod.assetIssue(httpnode, asset2Address, name, name, totalSupply, 1, 1,
        System.currentTimeMillis() + 5000, System.currentTimeMillis() + 50000000, 2, 3, description,
        url, 1000L, 1000L, asset2Key);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);

    response = HttpMethod.getAccount(httpnode, exchangeOwnerAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    assetIssueId1 = responseContent.getString("asset_issued_ID");
    Assert.assertTrue(Integer.parseInt(assetIssueId1) > 1000000);

    response = HttpMethod.getAccount(httpnode, asset2Address);
    responseContent = HttpMethod.parseResponseContent(response);
    assetIssueId2 = responseContent.getString("asset_issued_ID");
    Assert.assertTrue(Integer.parseInt(assetIssueId2) > 1000000);

    response = HttpMethod
        .transferAsset(httpnode, asset2Address, exchangeOwnerAddress, assetIssueId2, 10000000000L,
            asset2Key);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);

    //Create exchange.
    response = HttpMethod
        .exchangeCreate(httpnode, exchangeOwnerAddress, assetIssueId1, 1000000L, assetIssueId2,
            1000000L, exchangeOwnerKey);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "List exchanges by http", groups = {"daily", "serial"})
  public void test02ListExchange() {
    response = HttpMethod.listExchanges(httpnode);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    JSONArray jsonArray = JSONArray.parseArray(responseContent.getString("exchanges"));
    Assert.assertTrue(jsonArray.size() >= 1);
    exchangeId = jsonArray.size();
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "List exchanges from solidity by http", groups = {"daily", "serial"})
  public void test03ListExchangeFromSolidity() {
    HttpMethod.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    response = HttpMethod.listExchangesFromSolidity(httpSoliditynode);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    JSONArray jsonArray = JSONArray.parseArray(responseContent.getString("exchanges"));
    Assert.assertTrue(jsonArray.size() >= 1);
    exchangeId = jsonArray.size();
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "List exchanges from PBFT by http", groups = {"daily", "serial"})
  public void test04ListExchangeFromPbft() {
    HttpMethod.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    response = HttpMethod.listExchangesFromPbft(httpPbftNode);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    JSONArray jsonArray = JSONArray.parseArray(responseContent.getString("exchanges"));
    Assert.assertTrue(jsonArray.size() >= 1);
    exchangeId = jsonArray.size();
  }


  /**
   * constructor.
   */
  @Test(enabled = false, description = "GetExchangeById by http", groups = {"daily", "serial"})
  public void test05GetExchangeById() {
    response = HttpMethod.getExchangeById(httpnode, exchangeId);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.getInteger("exchange_id").equals(exchangeId));
    Assert.assertEquals(responseContent.getString("creator_address"),
        ByteArray.toHexString(exchangeOwnerAddress));
    beforeInjectBalance = responseContent.getLong("first_token_balance");

    logger.info("beforeInjectBalance" + beforeInjectBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "GetExchangeById from solidity by http", groups = {"daily", "serial"})
  public void test06GetExchangeByIdFromSolidity() {
    response = HttpMethod.getExchangeByIdFromSolidity(httpSoliditynode, exchangeId);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.getInteger("exchange_id").equals(exchangeId));
    Assert.assertEquals(responseContent.getString("creator_address"),
        ByteArray.toHexString(exchangeOwnerAddress));
    beforeInjectBalance = responseContent.getLong("first_token_balance");

    logger.info("beforeInjectBalance" + beforeInjectBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "GetExchangeById from Pbft by http", groups = {"daily", "serial"})
  public void test07GetExchangeByIdFromPbft() {
    response = HttpMethod.getExchangeByIdFromPbft(httpPbftNode, exchangeId);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.getInteger("exchange_id").equals(exchangeId));
    Assert.assertEquals(responseContent.getString("creator_address"),
        ByteArray.toHexString(exchangeOwnerAddress));
    beforeInjectBalance = responseContent.getLong("first_token_balance");

    logger.info("beforeInjectBalance" + beforeInjectBalance);
  }


  /**
   * constructor.
   */
  @Test(enabled = false, description = "Inject exchange by http", groups = {"daily", "serial"})
  public void test08InjectExchange() {
    //Inject exchange.
    response = HttpMethod
        .exchangeInject(httpnode, exchangeOwnerAddress, exchangeId, assetIssueId1, 300L,
            exchangeOwnerKey);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    response = HttpMethod.getExchangeById(httpnode, exchangeId);
    responseContent = HttpMethod.parseResponseContent(response);
    afterInjectBalance = responseContent.getLong("first_token_balance");
    response = HttpMethod.getExchangeById(httpnode, exchangeId);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    logger.info("afterInjectBalance" + afterInjectBalance);
    Assert.assertTrue(afterInjectBalance - beforeInjectBalance == 300L);
    beforeWithdrawBalance = afterInjectBalance;
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "Withdraw exchange by http", groups = {"daily", "serial"})
  public void test09WithdrawExchange() {
    //Withdraw exchange.
    response = HttpMethod
        .exchangeWithdraw(httpnode, exchangeOwnerAddress, exchangeId, assetIssueId1, 170L,
            exchangeOwnerKey);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    response = HttpMethod.getExchangeById(httpnode, exchangeId);
    responseContent = HttpMethod.parseResponseContent(response);
    afterWithdrawBalance = responseContent.getLong("first_token_balance");
    response = HttpMethod.getExchangeById(httpnode, exchangeId);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(beforeWithdrawBalance - afterWithdrawBalance == 170L);
    beforeTransactionBalance = afterWithdrawBalance;
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "Transaction exchange by http", groups = {"daily", "serial"})
  public void test10TransactionExchange() {
    //Transaction exchange.
    response = HttpMethod
        .exchangeTransaction(httpnode, exchangeOwnerAddress, exchangeId, assetIssueId1, 100L, 1L,
            exchangeOwnerKey);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    response = HttpMethod.getExchangeById(httpnode, exchangeId);
    responseContent = HttpMethod.parseResponseContent(response);
    afterTransactionBalance = responseContent.getLong("first_token_balance");
    response = HttpMethod.getExchangeById(httpnode, exchangeId);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(afterTransactionBalance - beforeTransactionBalance >= 1);
  }


  /**
   * constructor.
   */
  @Test(enabled = false, description = "Get asset issue list by name by http", groups = {"daily", "serial"})
  public void test11GetAssetIssueListByName() {
    response = HttpMethod.getAssetIssueListByName(httpnode, name);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    JSONArray jsonArray = JSONArray.parseArray(responseContent.get("assetIssue").toString());
    Assert.assertTrue(jsonArray.size() >= 2);
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "Get asset issue list by name from solidity and pbft by http", groups = {"daily", "serial"})
  public void test12GetAssetIssueListByNameFromSolidityAndPbft() {
    HttpMethod.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    response = HttpMethod.getAssetIssueListByNameFromSolidity(httpSoliditynode, name);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    JSONArray jsonArray = JSONArray.parseArray(responseContent.get("assetIssue").toString());
    Assert.assertTrue(jsonArray.size() >= 2);

    response = HttpMethod.getAssetIssueListByNameFromPbft(httpPbftNode, name);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    jsonArray = JSONArray.parseArray(responseContent.get("assetIssue").toString());
    Assert.assertTrue(jsonArray.size() >= 2);
  }

  /**
   * * constructor. *
   */
  @Test(enabled = false, description = "Get paginated exchange list by http", groups = {"daily", "serial"})
  public void test13GetPaginatedExchangeList() {

    response = HttpMethod.getPaginatedExchangeList(httpnode, 0, 1);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);

    JSONArray jsonArray = JSONArray.parseArray(responseContent.getString("exchanges"));
    Assert.assertTrue(jsonArray.size() == 1);
  }


  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethod.freeResource(httpnode, asset2Address, fromAddress, asset2Key);
    HttpMethod.disConnect();
  }
}