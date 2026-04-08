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
import stest.tron.wallet.common.client.utils.Base58;

@Slf4j
public class HttpTestAsset001 {

  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = now;
  private static String name = "testAssetIssue002_" + now;
  private static String assetIssueId;
  private static String updateDescription = "Description_update_" + now;
  private static String updateUrl = "Url_update_" + now;
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethod.getFinalAddress(testKey002);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] assetAddress = ecKey1.getAddress();
  String assetKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] participateAddress = ecKey2.getAddress();
  String participateKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  Long amount = 2048000000L;

  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf").getString("defaultParameter.assetUrl");
  private JSONObject responseContent;
  private JSONObject getAssetIssueByIdContent;
  private JSONObject getAssetIssueByNameContent;
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
  @Test(enabled = true, description = "Create asset issue by http", groups = {"daily", "serial"})
  public void test01CreateAssetIssue() {
    response = HttpMethod.sendCoin(httpnode, fromAddress, assetAddress, amount, testKey002);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    response = HttpMethod
        .sendCoin(httpnode, fromAddress, participateAddress, 10000000L, testKey002);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    //Create an asset issue
    response = HttpMethod.assetIssue(httpnode, assetAddress, name, name, totalSupply, 1, 1,
        System.currentTimeMillis() + 5000, System.currentTimeMillis() + 50000000, 2, 3, description,
        url, 1000L, 1000L, assetKey);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    response = HttpMethod.getAccount(httpnode, assetAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);

    assetIssueId = responseContent.getString("asset_issued_ID");
    logger.info(assetIssueId);
    Assert.assertTrue(Integer.parseInt(assetIssueId) > 1000000);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetAssetIssueById by http", groups = {"daily", "serial"})
  public void test02GetAssetIssueById() {
    HttpMethod.waitToProduceOneBlock(httpnode);
    response = HttpMethod.getAssetIssueById(httpnode, assetIssueId);
    getAssetIssueByIdContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(getAssetIssueByIdContent);
    Assert.assertTrue(totalSupply == getAssetIssueByIdContent.getLong("total_supply"));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetAssetIssueById from solidity by http", groups = {"daily", "serial"})
  public void test03GetAssetIssueByIdFromSolidity() {
    HttpMethod.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    response = HttpMethod.getAssetIssueByIdFromSolidity(httpSoliditynode, assetIssueId);
    getAssetIssueByIdContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(getAssetIssueByIdContent);
    Assert.assertTrue(totalSupply == getAssetIssueByIdContent.getLong("total_supply"));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetAssetIssueById from PBFT by http", groups = {"daily", "serial"})
  public void test04GetAssetIssueByIdFromPbft() {
    HttpMethod.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    response = HttpMethod.getAssetIssueByIdFromPbft(httpPbftNode, assetIssueId);
    getAssetIssueByIdContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(getAssetIssueByIdContent);
    Assert.assertTrue(totalSupply == getAssetIssueByIdContent.getLong("total_supply"));
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetAssetIssueByName by http", groups = {"daily", "serial"})
  public void test05GetAssetIssueByName() {
    response = HttpMethod.getAssetIssueByName(httpnode, name);
    getAssetIssueByNameContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(getAssetIssueByNameContent);
    Assert.assertTrue(totalSupply == getAssetIssueByNameContent.getLong("total_supply"));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetAssetIssueByName from solidity by http", groups = {"daily", "serial"})
  public void test06GetAssetIssueByNameFromSolidity() {
    response = HttpMethod.getAssetIssueByNameFromSolidity(httpSoliditynode, name);
    getAssetIssueByNameContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(getAssetIssueByNameContent);
    Assert.assertTrue(totalSupply == getAssetIssueByNameContent.getLong("total_supply"));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetAssetIssueByName from PBFT by http", groups = {"daily", "serial"})
  public void test07GetAssetIssueByNameFromPbft() {
    response = HttpMethod.getAssetIssueByNameFromPbft(httpPbftNode, name);
    getAssetIssueByNameContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(getAssetIssueByNameContent);
    Assert.assertTrue(totalSupply == getAssetIssueByNameContent.getLong("total_supply"));
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "TransferAsset by http", groups = {"daily", "serial"})
  public void test08TransferAsset() {
    logger.info("Transfer asset.");
    response = HttpMethod
        .transferAsset(httpnode, assetAddress, participateAddress, assetIssueId, 100L, assetKey);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    response = HttpMethod.getAccount(httpnode, participateAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(!responseContent.getString("assetV2").isEmpty());
    //logger.info(responseContent.get("assetV2").toString());

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Participate asset issue by http", groups = {"daily", "serial"})
  public void test09ParticipateAssetIssue() {
    HttpMethod.waitToProduceOneBlock(httpnode);
    response = HttpMethod
        .participateAssetIssue(httpnode, assetAddress, participateAddress, assetIssueId, 1000L,
            participateKey);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    response = HttpMethod.getAccount(httpnode, participateAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Update asset issue by http", groups = {"daily", "serial"})
  public void test10UpdateAssetIssue() {
    response = HttpMethod
        .updateAssetIssue(httpnode, assetAddress, updateDescription, updateUrl, 290L, 390L,
            assetKey);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    response = HttpMethod.getAssetIssueById(httpnode, assetIssueId);
    getAssetIssueByIdContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(getAssetIssueByIdContent);

    Assert.assertTrue(getAssetIssueByIdContent.getLong("public_free_asset_net_limit") == 390L);
    Assert.assertTrue(getAssetIssueByIdContent.getLong("free_asset_net_limit") == 290L);
    Assert.assertTrue(getAssetIssueByIdContent.getString("description")
        .equalsIgnoreCase(HttpMethod.str2hex(updateDescription)));
    Assert.assertTrue(
        getAssetIssueByIdContent.getString("url").equalsIgnoreCase(HttpMethod.str2hex(updateUrl)));
  }


  /**
   * * constructor. *
   */
  @Test(enabled = true, description = "Get asset issue list by http", groups = {"daily", "serial"})
  public void test11GetAssetissueList() {

    response = HttpMethod.getAssetissueList(httpnode);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);

    JSONArray jsonArray = JSONArray.parseArray(responseContent.getString("assetIssue"));
    Assert.assertTrue(jsonArray.size() >= 1);
  }


  /**
   * * constructor. *
   */
  @Test(enabled = true, description = "Get asset issue list from solidity by http", groups = {"daily", "serial"})
  public void test12GetAssetissueListFromSolidity() {
    HttpMethod.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    response = HttpMethod.getAssetIssueListFromSolidity(httpSoliditynode);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);

    JSONArray jsonArray = JSONArray.parseArray(responseContent.getString("assetIssue"));
    Assert.assertTrue(jsonArray.size() >= 1);
  }

  /**
   * * constructor. *
   */
  @Test(enabled = true, description = "Get asset issue list from PBFT by http", groups = {"daily", "serial"})
  public void test13GetAssetissueListFromPbft() {
    HttpMethod.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    response = HttpMethod.getAssetIssueListFromPbft(httpPbftNode);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);

    JSONArray jsonArray = JSONArray.parseArray(responseContent.getString("assetIssue"));
    Assert.assertTrue(jsonArray.size() >= 1);
  }


  /**
   * * constructor. *
   */
  @Test(enabled = true, description = "Get paginated asset issue list by http", groups = {"daily", "serial"})
  public void test14GetPaginatedAssetissueList() {
    response = HttpMethod.getPaginatedAssetissueList(httpnode, 0, 1);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);

    JSONArray jsonArray = JSONArray.parseArray(responseContent.getString("assetIssue"));
    Assert.assertTrue(jsonArray.size() == 1);
  }


  /**
   * * constructor. *
   */
  @Test(enabled = true, description = "Get paginated asset issue list from solidity by http", groups = {"daily", "serial"})
  public void test15GetPaginatedAssetissueListFromSolidity() {
    response = HttpMethod.getPaginatedAssetissueListFromSolidity(httpSoliditynode, 0, 1);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);

    JSONArray jsonArray = JSONArray.parseArray(responseContent.getString("assetIssue"));
    Assert.assertTrue(jsonArray.size() == 1);
  }


  /**
   * * constructor. *
   */
  @Test(enabled = true, description = "Get paginated asset issue list from PBFT by http", groups = {"daily", "serial"})
  public void test16GetPaginatedAssetissueListFromPbft() {
    response = HttpMethod.getPaginatedAssetissueListFromPbft(httpPbftNode, 0, 1);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);

    JSONArray jsonArray = JSONArray.parseArray(responseContent.getString("assetIssue"));
    Assert.assertTrue(jsonArray.size() == 1);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "TransferAsset visible true,then broadcast hex", groups = {"daily", "serial"})
  public void test17TransferAssetVisible() {
    logger.info("Transfer asset visible true,then broadcast hex");
    response = HttpMethod.getAccount(httpnode, participateAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    int amountBefore = responseContent.getJSONArray("assetV2").getJSONObject(0).getIntValue("value");
    response = HttpMethod.transferAsset(httpnode, Base58.encode58Check(assetAddress),
        Base58.encode58Check(participateAddress), assetIssueId, 1L, assetKey);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    response = HttpMethod.getAccount(httpnode, participateAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    int amountAfter = responseContent.getJSONArray("assetV2").getJSONObject(0).getIntValue("value");
    Assert.assertEquals(1, amountAfter - amountBefore);

  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethod.freeResource(httpnode, assetAddress, fromAddress, assetKey);
    HttpMethod.freeResource(httpnode, participateAddress, fromAddress, participateKey);
    HttpMethod.disConnect();
  }
}