package stest.tron.wallet.dailybuild.http;

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
public class HttpTestMarket002 {

  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = now;
  private static String name = "testAssetIssue002_" + now;
  private static String assetIssueId1;
  private static String assetIssueId2;
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethod.getFinalAddress(testKey002);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] sellAddress = ecKey1.getAddress();
  String sellKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ECKey ecKey2 = new ECKey(Utils.getRandom());
  private byte[] dev002Address = ecKey2.getAddress();
  private String dev002Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  String txId1;
  String txId2;
  String orderId;
  String orderId1;
  String orderId2;

  Long amount = 2048000000L;

  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf").getString("defaultParameter.assetUrl");
  private JSONObject responseContent;
  private JSONObject getMarketOrderByIdContent;
  private JSONObject getMarketOrderByIdContentFromSolidity;
  private JSONObject getMarketOrderByIdContentFromPbft;
  private JSONObject getMarketOrderByAccountContent;
  private JSONObject getMarketOrderByAccountContentFromSolidity;
  private JSONObject getMarketOrderByAccountContentFromPbft;
  private JSONObject getMarketPairListContent;
  private JSONObject getMarketPairListContentFromSolidity;
  private JSONObject getMarketPairListContentFromPbft;
  private JSONObject getMarketOrderListByPairContent;
  private JSONObject getMarketOrderListByPairContentFromSolidity;
  private JSONObject getMarketOrderListByPairContentFromPbft;
  private JSONObject getMarketPriceByPairContent;
  private JSONObject getMarketPriceByPairContentFromSolidity;
  private JSONObject getMarketPriceByPairContentFromPbft;
  private HttpResponse response;
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(1);
  private String httpSolidityNode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(2);
  private String httpPbftNode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(4);


  /**
   * constructor.
   */
  @Test(enabled = false, description = "MarketSellAsset trx with trc10 by http", groups = {"daily", "serial"})
  public void test01MarketSellAsset() {
    PublicMethod.printAddress(sellKey);
    PublicMethod.printAddress(dev002Key);

    response = HttpMethod.sendCoin(httpnode, fromAddress, sellAddress, amount, testKey002);
    response = HttpMethod.sendCoin(httpnode, fromAddress, dev002Address, amount, testKey002);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);

    //Create an asset issue
    response = HttpMethod.assetIssue(httpnode, sellAddress, name, name, totalSupply, 1, 1,
        System.currentTimeMillis() + 5000, System.currentTimeMillis() + 50000000, 2, 3, description,
        url, 1000L, 1000L, sellKey);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    response = HttpMethod.getAccount(httpnode, sellAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    assetIssueId1 = responseContent.getString("asset_issued_ID");
    logger.info(assetIssueId1);
    Assert.assertTrue(Integer.parseInt(assetIssueId1) > 1000000);

    response = HttpMethod.assetIssue(httpnode, dev002Address, name, name, totalSupply, 1, 1,
        System.currentTimeMillis() + 5000, System.currentTimeMillis() + 50000000, 2, 3, description,
        url, 1000L, 1000L, dev002Key);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    response = HttpMethod.getAccount(httpnode, dev002Address);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    assetIssueId2 = responseContent.getString("asset_issued_ID");
    logger.info(assetIssueId2);
    Assert.assertTrue(Integer.parseInt(assetIssueId2) > 1000000);

    // transferAsset
    response = HttpMethod
        .transferAsset(httpnode, dev002Address, sellAddress, assetIssueId2, 10000L, dev002Key);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    response = HttpMethod.getAccount(httpnode, sellAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);

    // marketsellasset trc10-trc10
    txId2 = HttpMethod
        .marketSellAssetGetTxId(httpnode, sellAddress, assetIssueId1, 10L, assetIssueId2, 500L,
            sellKey, "false");
    HttpMethod.waitToProduceOneBlock(httpnode);
    logger.info(txId2);
    response = HttpMethod.getTransactionInfoById(httpnode, txId2);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(!responseContent.getString("orderId").isEmpty());
    orderId = responseContent.getString("orderId");
    logger.info("orderId:" + orderId);

    // marketsellasset trx-trc10
    txId1 = HttpMethod
        .marketSellAssetGetTxId(httpnode, sellAddress, "_", 1000L, assetIssueId1, 20L, sellKey,
            "false");
    HttpMethod.waitToProduceOneBlock(httpnode);
    logger.info(txId1);
    response = HttpMethod.getTransactionInfoById(httpnode, txId1);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(!responseContent.getString("orderId").isEmpty());
    orderId1 = responseContent.getString("orderId");
    logger.info("orderId1:" + orderId1);

    // marketsellasset trc10-trx
    txId2 = HttpMethod
        .marketSellAssetGetTxId(httpnode, sellAddress, assetIssueId1, 10L, "_", 500L, sellKey,
            "false");
    HttpMethod.waitToProduceOneBlock(httpnode);
    logger.info(txId2);
    response = HttpMethod.getTransactionInfoById(httpnode, txId2);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    JSONObject orderDetails = responseContent.getJSONArray("orderDetails").getJSONObject(0);
    Assert.assertTrue(!responseContent.getString("orderId").isEmpty());
    Assert.assertTrue(500L == orderDetails.getLong("fillBuyQuantity"));
    Assert.assertTrue(10L == orderDetails.getLong("fillSellQuantity"));
    Assert
        .assertEquals(responseContent.getString("orderId"), orderDetails.getString("takerOrderId"));
    Assert.assertEquals(orderId1, orderDetails.getString("makerOrderId"));
    orderId2 = responseContent.getString("orderId");
    logger.info("orderId2:" + orderId2);


  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "GetMarketOrderById by http", groups = {"daily", "serial"})
  public void test02GetMarketOrderById() {
    // getMarketOrderById orderId1
    HttpMethod.waitToProduceOneBlock(httpnode);
    response = HttpMethod.getMarketOrderById(httpnode, orderId1, "false");
    getMarketOrderByIdContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(getMarketOrderByIdContent);
    Assert.assertEquals(ByteArray.toHexString(sellAddress),
        getMarketOrderByIdContent.getString("owner_address"));
    Assert.assertEquals("5f", getMarketOrderByIdContent.getString("sell_token_id"));
    Assert.assertTrue(1000L == getMarketOrderByIdContent.getLong("sell_token_quantity"));
    Assert.assertEquals(HttpMethod.str2hex(assetIssueId1),
        getMarketOrderByIdContent.getString("buy_token_id"));
    Assert.assertTrue(20L == getMarketOrderByIdContent.getLong("buy_token_quantity"));
    Assert.assertTrue(500L == getMarketOrderByIdContent.getLong("sell_token_quantity_remain"));

    // getMarketOrderById orderId2
    HttpResponse response2 = HttpMethod.getMarketOrderById(httpnode, orderId2, "false");
    JSONObject getMarketOrderByIdContent2 = HttpMethod.parseResponseContent(response2);
    HttpMethod.printJsonContent(getMarketOrderByIdContent2);
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "GetMarketOrderById by http from solidity", groups = {"daily", "serial"})
  public void test03GetMarketOrderByIdFromSolidity() {
    HttpMethod.waitToProduceOneBlockFromSolidity(httpnode, httpSolidityNode);
    response = HttpMethod.getMarketOrderByIdFromSolidity(httpSolidityNode, orderId1, "false");
    getMarketOrderByIdContentFromSolidity = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(getMarketOrderByIdContentFromSolidity);
    Assert.assertEquals(ByteArray.toHexString(sellAddress),
        getMarketOrderByIdContentFromSolidity.getString("owner_address"));
    Assert.assertEquals("5f", getMarketOrderByIdContentFromSolidity.getString("sell_token_id"));
    Assert
        .assertTrue(1000L == getMarketOrderByIdContentFromSolidity.getLong("sell_token_quantity"));
    Assert.assertEquals(HttpMethod.str2hex(assetIssueId1),
        getMarketOrderByIdContentFromSolidity.getString("buy_token_id"));
    Assert.assertTrue(20L == getMarketOrderByIdContentFromSolidity.getLong("buy_token_quantity"));
    Assert.assertTrue(
        500L == getMarketOrderByIdContentFromSolidity.getLong("sell_token_quantity_remain"));
    Assert.assertEquals(getMarketOrderByIdContent, getMarketOrderByIdContentFromSolidity);
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "GetMarketOrderById by http from pbft", groups = {"daily", "serial"})
  public void test04GetMarketOrderByIdFromPbft() {
    HttpMethod.waitToProduceOneBlockFromPbft(httpnode, httpPbftNode);
    response = HttpMethod.getMarketOrderByIdFromPbft(httpPbftNode, orderId1, "false");
    getMarketOrderByIdContentFromPbft = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(getMarketOrderByIdContentFromPbft);
    Assert.assertEquals(ByteArray.toHexString(sellAddress),
        getMarketOrderByIdContentFromPbft.getString("owner_address"));
    Assert.assertEquals("5f", getMarketOrderByIdContentFromPbft.getString("sell_token_id"));
    Assert
        .assertTrue(1000L == getMarketOrderByIdContentFromPbft.getLong("sell_token_quantity"));
    Assert.assertEquals(HttpMethod.str2hex(assetIssueId1),
        getMarketOrderByIdContentFromPbft.getString("buy_token_id"));
    Assert.assertTrue(20L == getMarketOrderByIdContentFromPbft.getLong("buy_token_quantity"));
    Assert.assertTrue(
        500L == getMarketOrderByIdContentFromPbft.getLong("sell_token_quantity_remain"));
    Assert.assertEquals(getMarketOrderByIdContent, getMarketOrderByIdContentFromPbft);
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "GetMarketOrderByAccount by http", groups = {"daily", "serial"})
  public void test05GetMarketOrderByAccount() {
    HttpMethod.waitToProduceOneBlock(httpnode);
    response = HttpMethod.getMarketOrderByAccount(httpnode, sellAddress, "false");
    getMarketOrderByAccountContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(getMarketOrderByAccountContent);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    JSONObject orders = getMarketOrderByAccountContent.getJSONArray("orders").getJSONObject(1);
    Assert.assertEquals(ByteArray.toHexString(sellAddress), orders.getString("owner_address"));
    Assert.assertEquals("5f", orders.getString("sell_token_id"));
    Assert.assertTrue(1000L == orders.getLong("sell_token_quantity"));
    Assert.assertEquals(HttpMethod.str2hex(assetIssueId1), orders.getString("buy_token_id"));
    Assert.assertTrue(20L == orders.getLong("buy_token_quantity"));
    Assert.assertTrue(500L == orders.getLong("sell_token_quantity_remain"));
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "GetMarketOrderByAccount by http from solidity", groups = {"daily", "serial"})
  public void test06GetMarketOrderByAccountFromSolidity() {
    response = HttpMethod
        .getMarketOrderByAccountFromSolidity(httpSolidityNode, sellAddress, "false");
    getMarketOrderByAccountContentFromSolidity = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(getMarketOrderByAccountContentFromSolidity);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    JSONObject orders = getMarketOrderByAccountContentFromSolidity.getJSONArray("orders")
        .getJSONObject(1);
    Assert.assertEquals(ByteArray.toHexString(sellAddress), orders.getString("owner_address"));
    Assert.assertEquals("5f", orders.getString("sell_token_id"));
    Assert.assertTrue(1000L == orders.getLong("sell_token_quantity"));
    Assert.assertEquals(HttpMethod.str2hex(assetIssueId1), orders.getString("buy_token_id"));
    Assert.assertTrue(20L == orders.getLong("buy_token_quantity"));
    Assert.assertTrue(500L == orders.getLong("sell_token_quantity_remain"));
    Assert.assertEquals(getMarketOrderByAccountContent, getMarketOrderByAccountContentFromSolidity);
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "GetMarketOrderByAccount by http from pbft", groups = {"daily", "serial"})
  public void test07GetMarketOrderByAccountFromPbft() {
    response = HttpMethod.getMarketOrderByAccountFromPbft(httpPbftNode, sellAddress, "false");
    getMarketOrderByAccountContentFromPbft = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(getMarketOrderByAccountContentFromPbft);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    JSONObject orders = getMarketOrderByAccountContentFromPbft.getJSONArray("orders")
        .getJSONObject(1);
    Assert.assertEquals(ByteArray.toHexString(sellAddress), orders.getString("owner_address"));
    Assert.assertEquals("5f", orders.getString("sell_token_id"));
    Assert.assertTrue(1000L == orders.getLong("sell_token_quantity"));
    Assert.assertEquals(HttpMethod.str2hex(assetIssueId1), orders.getString("buy_token_id"));
    Assert.assertTrue(20L == orders.getLong("buy_token_quantity"));
    Assert.assertTrue(500L == orders.getLong("sell_token_quantity_remain"));
    Assert.assertEquals(getMarketOrderByAccountContent, getMarketOrderByAccountContentFromPbft);
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "GetMarketPairList by http", groups = {"daily", "serial"})
  public void test08GetMarketPairList() {
    response = HttpMethod.getMarketPairList(httpnode, "false");
    getMarketPairListContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(getMarketPairListContent);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    int orderPairSize = getMarketPairListContent.getJSONArray("orderPair").size();
    Assert.assertTrue(orderPairSize > 0);
    Assert.assertEquals("5f",
        getMarketPairListContent.getJSONArray("orderPair").getJSONObject(orderPairSize - 1)
            .getString("sell_token_id"));
    Assert.assertEquals(HttpMethod.str2hex(assetIssueId1),
        getMarketPairListContent.getJSONArray("orderPair").getJSONObject(orderPairSize - 1)
            .getString("buy_token_id"));
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "GetMarketPairList by http from solidity", groups = {"daily", "serial"})
  public void test09GetMarketPairListFromSolidity() {
    response = HttpMethod.getMarketPairListFromSolidity(httpSolidityNode, "false");
    getMarketPairListContentFromSolidity = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(getMarketPairListContentFromSolidity);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    int orderPairSize = getMarketPairListContentFromSolidity.getJSONArray("orderPair").size();
    Assert.assertTrue(orderPairSize > 0);
    Assert.assertEquals("5f",
        getMarketPairListContentFromSolidity.getJSONArray("orderPair")
            .getJSONObject(orderPairSize - 1)
            .getString("sell_token_id"));
    Assert.assertEquals(HttpMethod.str2hex(assetIssueId1),
        getMarketPairListContentFromSolidity.getJSONArray("orderPair")
            .getJSONObject(orderPairSize - 1)
            .getString("buy_token_id"));
    Assert.assertEquals(getMarketPairListContent, getMarketPairListContentFromSolidity);

  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "GetMarketPairList by http from pbft", groups = {"daily", "serial"})
  public void test10GetMarketPairListFromPbft() {
    response = HttpMethod.getMarketPairListFromPbft(httpPbftNode, "false");
    getMarketPairListContentFromPbft = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(getMarketPairListContentFromPbft);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    int orderPairSize = getMarketPairListContentFromPbft.getJSONArray("orderPair").size();
    Assert.assertTrue(orderPairSize > 0);
    Assert.assertEquals("5f",
        getMarketPairListContentFromPbft.getJSONArray("orderPair")
            .getJSONObject(orderPairSize - 1)
            .getString("sell_token_id"));
    Assert.assertEquals(HttpMethod.str2hex(assetIssueId1),
        getMarketPairListContentFromPbft.getJSONArray("orderPair")
            .getJSONObject(orderPairSize - 1)
            .getString("buy_token_id"));
    Assert.assertEquals(getMarketPairListContent, getMarketPairListContentFromPbft);

  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "GetMarketOrderListByPair by http", groups = {"daily", "serial"})
  public void test11GetMarketOrderListByPair() {
    response = HttpMethod.getMarketOrderListByPair(httpnode, "_", assetIssueId1, "false");
    getMarketOrderListByPairContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(getMarketOrderListByPairContent);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    JSONObject orders = getMarketOrderListByPairContent.getJSONArray("orders")
        .getJSONObject(getMarketOrderListByPairContent.getJSONArray("orders").size() - 1);
    Assert.assertEquals(ByteArray.toHexString(sellAddress), orders.getString("owner_address"));
    Assert.assertEquals("5f", orders.getString("sell_token_id"));
    Assert.assertTrue(1000L == orders.getLong("sell_token_quantity"));
    Assert.assertEquals(HttpMethod.str2hex(assetIssueId1), orders.getString("buy_token_id"));
    Assert.assertTrue(20L == orders.getLong("buy_token_quantity"));
    Assert.assertEquals(getMarketOrderListByPairContent.getLong("sell_token_quantity"),
        getMarketOrderListByPairContent.getLong("sell_token_quantity_remain"));

    Assert.assertTrue(getMarketOrderListByPairContent.getJSONArray("orders").size() > 0);
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "GetMarketOrderListByPair by http from solidity", groups = {"daily", "serial"})
  public void test12GetMarketOrderListByPairFromSolidity() {
    response = HttpMethod
        .getMarketOrderListByPairFromSolidity(httpSolidityNode, "_", assetIssueId1, "false");
    getMarketOrderListByPairContentFromSolidity = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(getMarketOrderListByPairContentFromSolidity);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    JSONObject orders = getMarketOrderListByPairContentFromSolidity.getJSONArray("orders")
        .getJSONObject(
            getMarketOrderListByPairContentFromSolidity.getJSONArray("orders").size() - 1);
    Assert.assertEquals(ByteArray.toHexString(sellAddress), orders.getString("owner_address"));
    Assert.assertEquals("5f", orders.getString("sell_token_id"));
    Assert.assertTrue(1000L == orders.getLong("sell_token_quantity"));
    Assert.assertEquals(HttpMethod.str2hex(assetIssueId1), orders.getString("buy_token_id"));
    Assert.assertTrue(20L == orders.getLong("buy_token_quantity"));
    Assert.assertEquals(getMarketOrderListByPairContentFromSolidity.getLong("sell_token_quantity"),
        getMarketOrderListByPairContentFromSolidity.getLong("sell_token_quantity_remain"));

    Assert
        .assertTrue(getMarketOrderListByPairContentFromSolidity.getJSONArray("orders").size() > 0);
    Assert
        .assertEquals(getMarketOrderListByPairContent, getMarketOrderListByPairContentFromSolidity);

  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "GetMarketOrderListByPair by http from pbft", groups = {"daily", "serial"})
  public void test13GetMarketOrderListByPairFromPbft() {
    response = HttpMethod
        .getMarketOrderListByPairFromPbft(httpPbftNode, "_", assetIssueId1, "false");
    getMarketOrderListByPairContentFromPbft = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(getMarketOrderListByPairContentFromPbft);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    JSONObject orders = getMarketOrderListByPairContentFromPbft.getJSONArray("orders")
        .getJSONObject(
            getMarketOrderListByPairContentFromPbft.getJSONArray("orders").size() - 1);
    Assert.assertEquals(ByteArray.toHexString(sellAddress), orders.getString("owner_address"));
    Assert.assertEquals("5f", orders.getString("sell_token_id"));
    Assert.assertTrue(1000L == orders.getLong("sell_token_quantity"));
    Assert.assertEquals(HttpMethod.str2hex(assetIssueId1), orders.getString("buy_token_id"));
    Assert.assertTrue(20L == orders.getLong("buy_token_quantity"));
    Assert.assertEquals(getMarketOrderListByPairContentFromPbft.getLong("sell_token_quantity"),
        getMarketOrderListByPairContentFromPbft.getLong("sell_token_quantity_remain"));

    Assert
        .assertTrue(getMarketOrderListByPairContentFromPbft.getJSONArray("orders").size() > 0);
    Assert
        .assertEquals(getMarketOrderListByPairContent, getMarketOrderListByPairContentFromPbft);

  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "GetMarketPriceByPair from by http", groups = {"daily", "serial"})
  public void test14GetMarketPriceByPair() {
    response = HttpMethod.getMarketPriceByPair(httpnode, "_", assetIssueId1, "false");
    getMarketPriceByPairContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(getMarketPriceByPairContent);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    Assert.assertEquals("5f", getMarketPriceByPairContent.getString("sell_token_id"));
    Assert.assertEquals(HttpMethod.str2hex(assetIssueId1),
        getMarketPriceByPairContent.getString("buy_token_id"));
    JSONObject prices = getMarketPriceByPairContent.getJSONArray("prices").getJSONObject(0);
    Assert.assertEquals("50", prices.getString("sell_token_quantity"));
    Assert.assertEquals("1", prices.getString("buy_token_quantity"));
    Assert.assertTrue(getMarketPriceByPairContent.getJSONArray("prices").size() > 0);
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "GetMarketPriceByPair from by http from solidity", groups = {"daily", "serial"})
  public void test15GetMarketPriceByPairFromSolidity() {
    response = HttpMethod
        .getMarketPriceByPairFromSolidity(httpSolidityNode, "_", assetIssueId1, "false");
    getMarketPriceByPairContentFromSolidity = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(getMarketPriceByPairContentFromSolidity);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    Assert.assertEquals("5f", getMarketPriceByPairContentFromSolidity.getString("sell_token_id"));
    Assert
        .assertEquals(HttpMethod.str2hex(assetIssueId1),
            getMarketPriceByPairContentFromSolidity.getString("buy_token_id"));
    JSONObject prices = getMarketPriceByPairContentFromSolidity.getJSONArray("prices")
        .getJSONObject(0);
    Assert.assertEquals("50", prices.getString("sell_token_quantity"));
    Assert.assertEquals("1", prices.getString("buy_token_quantity"));
    Assert.assertTrue(getMarketPriceByPairContentFromSolidity.getJSONArray("prices").size() > 0);
    Assert.assertEquals(getMarketPriceByPairContent, getMarketPriceByPairContentFromSolidity);
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "GetMarketPriceByPair from by http from pbft", groups = {"daily", "serial"})
  public void test16GetMarketPriceByPairFromPbft() {
    response = HttpMethod
        .getMarketPriceByPairFromPbft(httpPbftNode, "_", assetIssueId1, "false");
    getMarketPriceByPairContentFromPbft = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(getMarketPriceByPairContentFromPbft);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    Assert.assertEquals("5f", getMarketPriceByPairContentFromPbft.getString("sell_token_id"));
    Assert
        .assertEquals(HttpMethod.str2hex(assetIssueId1),
            getMarketPriceByPairContentFromPbft.getString("buy_token_id"));
    JSONObject prices = getMarketPriceByPairContentFromPbft.getJSONArray("prices")
        .getJSONObject(0);
    Assert.assertEquals("50", prices.getString("sell_token_quantity"));
    Assert.assertEquals("1", prices.getString("buy_token_quantity"));
    Assert.assertTrue(getMarketPriceByPairContentFromPbft.getJSONArray("prices").size() > 0);
    Assert.assertEquals(getMarketPriceByPairContent, getMarketPriceByPairContentFromPbft);
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "MarketCancelOrder trx with trc10 by http", groups = {"daily", "serial"})
  public void test17MarketCancelOrder() {
    response = HttpMethod.getMarketOrderByAccount(httpnode, sellAddress, "false");
    getMarketOrderByAccountContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(getMarketOrderByAccountContent);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    Assert.assertEquals(2, getMarketOrderByAccountContent.getJSONArray("orders").size());

    // MarketCancelOrder
    String txId = HttpMethod.marketCancelOrder(httpnode, sellAddress, orderId1, sellKey, "false");
    HttpMethod.waitToProduceOneBlock(httpnode);
    logger.info(txId);
    response = HttpMethod.getTransactionInfoById(httpnode, txId);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);

    response = HttpMethod.getMarketOrderByAccount(httpnode, sellAddress, "false");
    getMarketOrderByAccountContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(getMarketOrderByAccountContent);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    Assert.assertEquals(1, getMarketOrderByAccountContent.getJSONArray("orders").size());
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethod.freeResource(httpnode, sellAddress, fromAddress, sellKey);
    HttpMethod.disConnect();
  }
}