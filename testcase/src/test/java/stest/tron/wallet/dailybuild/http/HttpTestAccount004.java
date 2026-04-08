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
public class HttpTestAccount004 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethod.getFinalAddress(testKey002);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] setAccountIdAddress = ecKey1.getAddress();
  String setAccountIdKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  Long amount = 10000000L;
  String accountId;
  private JSONObject responseContent;
  private HttpResponse response;
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(0);
  private String httpSoliditynode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(2);


  private String httpPbftNode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(4);


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Set account by http", groups = {"daily", "serial"})
  public void test1setAccountId() {
    response = HttpMethod.sendCoin(httpnode, fromAddress, setAccountIdAddress, amount, testKey002);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);

    response = HttpMethod
        .setAccountId(httpnode, setAccountIdAddress, System.currentTimeMillis() + "id", false,
            setAccountIdKey);
    Assert.assertFalse(HttpMethod.verificationResult(response));

    //Set account id.
    accountId = System.currentTimeMillis() + "id";
    response = HttpMethod
        .setAccountId(httpnode, setAccountIdAddress, accountId, true, setAccountIdKey);
    Assert.assertTrue(HttpMethod.verificationResult(response));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get account by id via http", groups = {"daily", "serial"})
  public void test2getAccountId() {
    response = HttpMethod.getAccountById(httpnode, accountId, true);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertEquals(responseContent.get("account_id"), accountId);
    Assert.assertTrue(responseContent.size() >= 10);

    response = HttpMethod.getAccountById(httpnode, accountId, false);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.size() <= 1);


  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get account by id via http", groups = {"daily", "serial"})
  public void test3getAccountIdFromSolidity() {
    HttpMethod.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    response = HttpMethod.getAccountByIdFromSolidity(httpSoliditynode, accountId, true);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertEquals(responseContent.get("account_id"), accountId);
    Assert.assertTrue(responseContent.size() >= 10);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get account by id via PBFT http", groups = {"daily", "serial"})
  public void test4getAccountIdFromPbft() {
    HttpMethod.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    response = HttpMethod.getAccountByIdFromPbft(httpPbftNode, accountId, true);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertEquals(responseContent.get("account_id"), accountId);
    Assert.assertTrue(responseContent.size() >= 10);
  }


  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethod.freeResource(httpnode, setAccountIdAddress, fromAddress, setAccountIdKey);
    HttpMethod.disConnect();
  }
}