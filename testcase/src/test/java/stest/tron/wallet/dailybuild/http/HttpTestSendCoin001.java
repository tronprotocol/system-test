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
public class HttpTestSendCoin001 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethod.getFinalAddress(testKey002);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] receiverAddress = ecKey1.getAddress();
  String receiverKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  Long amount = 1000L;
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(1);
  private String httpSoliditynode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(2);
  private String httpPbftNode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(4);
  private JSONObject responseContent;
  private HttpResponse response;
  String txid;
  /**
   * constructor.
   */
  @Test(enabled = true, description = "SendCoin by http", groups = {"daily", "serial"})
  public void test1SendCoin() {
    response = HttpMethod.sendCoin(httpnode, fromAddress, receiverAddress, amount, testKey002);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    Assert.assertEquals(HttpMethod.getBalance(httpnode, receiverAddress), amount);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get transaction by id from solidity by http", groups = {"daily", "serial"})
  public void test2GetTransactionByIdFromSolidity() {
    txid = HttpMethod
        .sendCoinGetTxid(httpnode, fromAddress, receiverAddress, amount, testKey002);
    HttpMethod.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);

    response = HttpMethod.getTransactionByIdFromSolidity(httpSoliditynode, txid);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    String retString = responseContent.getString("ret");
    JSONArray array = JSONArray.parseArray(retString);
    Assert.assertEquals(
        HttpMethod.parseStringContent(array.get(0).toString()).getString("contractRet"), "SUCCESS");
    Assert.assertTrue(responseContent.size() > 4);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get transaction by id from PBFT by http", groups = {"daily", "serial"})
  public void test3GetTransactionByIdFromPbft() {
    response = HttpMethod.getTransactionByIdFromPbft(httpPbftNode, txid);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    String retString = responseContent.getString("ret");
    JSONArray array = JSONArray.parseArray(retString);
    Assert.assertEquals(
        HttpMethod.parseStringContent(array.get(0).toString()).getString("contractRet"), "SUCCESS");
    Assert.assertTrue(responseContent.size() > 4);
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get transaction info by id from solidity by http", groups = {"daily", "serial"})
  public void test4GetTransactionInfoByIdFromSolidity() {
    response = HttpMethod.getTransactionInfoByIdFromSolidity(httpSoliditynode, txid);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.size() > 4);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get transaction info by id from PBFT by http", groups = {"daily", "serial"})
  public void test5GetTransactionInfoByIdFromPbft() {
    response = HttpMethod.getTransactionInfoByIdFromPbft(httpPbftNode, txid);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.size() > 4);
  }


  /**
   * constructor.
   */
  @Test(enabled = false, description = "Get transactions from this from solidity by http", groups = {"daily", "serial"})
  public void test4GetTransactionsFromThisFromSolidity() {
    response = HttpMethod
        .getTransactionsFromThisFromSolidity(httpSoliditynode, fromAddress, 0, 100);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    JSONObject transactionObject = HttpMethod.parseStringContent(
        JSONArray.parseArray(responseContent.getString("transaction")).get(0).toString());
    String retString = transactionObject.getString("ret");
    JSONArray array = JSONArray.parseArray(retString);
    Assert.assertEquals(
        HttpMethod.parseStringContent(array.get(0).toString()).getString("contractRet"), "SUCCESS");
    Assert.assertTrue(responseContent.size() == 1);
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "Get transactions to this from solidity by http", groups = {"daily", "serial"})
  public void test5GetTransactionsToThisFromSolidity() {
    response = HttpMethod
        .getTransactionsFromThisFromSolidity(httpSoliditynode, fromAddress, 0, 100);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    JSONObject transactionObject = HttpMethod.parseStringContent(
        JSONArray.parseArray(responseContent.getString("transaction")).get(0).toString());
    String retString = transactionObject.getString("ret");
    JSONArray array = JSONArray.parseArray(retString);
    Assert.assertEquals(
        HttpMethod.parseStringContent(array.get(0).toString()).getString("contractRet"), "SUCCESS");
    Assert.assertTrue(responseContent.size() == 1);
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethod.freeResource(httpnode, receiverAddress, fromAddress, receiverKey);
    HttpMethod.disConnect();
  }

}
