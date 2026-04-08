package stest.tron.wallet.dailybuild.http;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.HttpMethod;
import stest.tron.wallet.common.client.utils.PublicMethod;

@Slf4j
public class HttpTestAccount001 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethod.getFinalAddress(testKey002);
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
  @Test(enabled = true, description = "Get account by http", groups = {"daily", "serial"})
  public void getAccount() {
    response = HttpMethod.getAccount(httpnode, fromAddress);
    logger.info("code is " + response.getStatusLine().getStatusCode());
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.size() > 3);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get account from solidity by http", groups = {"daily", "serial"})
  public void getAccountFromSolidity() {
    response = HttpMethod.getAccountFromSolidity(httpSoliditynode, fromAddress);
    logger.info("code is " + response.getStatusLine().getStatusCode());
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.size() > 3);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get accountNet by http", groups = {"daily", "serial"})
  public void getAccountNet() {
    response = HttpMethod.getAccountNet(httpnode, fromAddress);
    logger.info("code is " + response.getStatusLine().getStatusCode());
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertEquals(Integer.parseInt(responseContent.get("freeNetLimit").toString()), 1500);
    Assert.assertEquals(Long.parseLong(responseContent.get("TotalNetLimit").toString()),
        43200000000L);
    Assert.assertTrue(responseContent.size() >= 2);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get accountResource by http", groups = {"daily", "serial"})
  public void getAccountResource() {
    response = HttpMethod.getAccountReource(httpnode, fromAddress);
    logger.info("code is " + response.getStatusLine().getStatusCode());
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(
        Long.parseLong(responseContent.get("TotalEnergyLimit").toString()) >= 50000000000L);
    Assert.assertTrue(responseContent.size() >= 3);
  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethod.disConnect();
  }
}