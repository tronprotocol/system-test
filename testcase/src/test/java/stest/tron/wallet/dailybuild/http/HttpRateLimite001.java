package stest.tron.wallet.dailybuild.http;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.JsonRpcBase;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class HttpRateLimite001 extends JsonRpcBase {

  private final String testKey002 =
      Configuration.getByPath("testng.conf").getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private JSONObject responseContent;
  private HttpResponse response;
  private String httpnode =
      Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list").get(0);
  private String httpSoliditynode =
      Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list").get(3);
  private String realHttpSoliditynode =
      Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list").get(2);
  private String httpPbftNode =
      Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list").get(4);

  

  /** constructor. */
  @BeforeClass
  public void beforeClass() {
  }

  /** constructor. */
  @Test(enabled = true, description = "Rate limit QpsStrategy for ListWitness interface")
  public void test01QpsStrategyForListWitnessInterface() {
    Long startTimeStamp = System.currentTimeMillis();
    Integer repeatTimes = 0;
    while (repeatTimes++ < 15) {
      HttpMethed.listwitnesses(httpnode);
    }
    Long endTimesStap = System.currentTimeMillis();
    logger.info("startTimeStamp - endTimesStap:" + (endTimesStap - startTimeStamp));
    Assert.assertTrue(endTimesStap - startTimeStamp > 4000);
  }

  /** constructor. */
  @Test(enabled = true, description = "Rate limit IpQpsStrategy for ListNodes interface")
  public void test02IpQpsStrategyForListNodesInterface() {
    Long startTimeStamp = System.currentTimeMillis();
    Integer repeatTimes = 0;
    while (repeatTimes++ < 15) {
      HttpMethed.listNodes(httpnode);
    }
    Long endTimesStap = System.currentTimeMillis();
    logger.info("startTimeStamp - endTimesStap:" + (endTimesStap - startTimeStamp));
    Assert.assertTrue(endTimesStap - startTimeStamp > 4000);
  }

  /** constructor. */
  @Test(
      enabled = true,
      description =
          "Rate limit IpQpsStrategy for GetBlockByLatestNumOnSolidity "
              + "interface on fullnode's solidity service")
  public void test03IpQpsStrategyForGetBlockByLatestNumOnSolidityInterface() {
    Long startTimeStamp = System.currentTimeMillis();
    Integer repeatTimes = 0;
    while (repeatTimes++ < 15) {
      HttpMethed.getBlockByLastNumFromSolidity(httpSoliditynode, 5);
      HttpMethed.getBlockByLastNumFromPbft(httpPbftNode, 5);
    }
    Long endTimesStap = System.currentTimeMillis();
    logger.info("startTimeStamp - endTimesStap:" + (endTimesStap - startTimeStamp));
    Assert.assertTrue(endTimesStap - startTimeStamp > 4000);
  }

  /** constructor. */
  @Test(
      enabled = true,
      description =
          "Rate limit QpsStrategy for getBlockByNum " + "interface on fullnode's solidity service")
  public void test04QpsStrategyForgetBlockByNumResourceInterfaceOnFullnodeSolidityService() {
    Long startTimeStamp = System.currentTimeMillis();
    Integer repeatTimes = 0;
    while (repeatTimes++ < 15) {
      HttpMethed.getBlockByLastNumFromSolidity(httpSoliditynode, 5);
      HttpMethed.getBlockByLastNumFromPbft(httpPbftNode, 5);
    }
    Long endTimesStap = System.currentTimeMillis();
    logger.info("startTimeStamp - endTimesStap:" + (endTimesStap - startTimeStamp));
    Assert.assertTrue(endTimesStap - startTimeStamp > 4000);
  }

  @Test(
      enabled = false,
      description =
          "Rate limit QpsStrategy for "
              + "getTransactionsFromThisFromSolidity "
              + "interface on real solidity")
  public void test06QpsStrategyForgetTransactionsToThisFromSolidity() {
    Long startTimeStamp = System.currentTimeMillis();
    Integer repeatTimes = 0;
    while (repeatTimes++ < 15) {
      logger.info(realHttpSoliditynode);
      HttpMethed.getTransactionsToThisFromSolidity(realHttpSoliditynode, fromAddress, 0, 50);
    }
    Long endTimesStap = System.currentTimeMillis();
    logger.info("startTimeStamp - endTimesStap:" + (endTimesStap - startTimeStamp));
    Assert.assertTrue(endTimesStap - startTimeStamp > 4000);
  }

  @Test(enabled = true, description = "Verify getstatsinfo Interface has been disabled")
  public void test07GetStatsInfo() {
    response = HttpMethed.getStatsInfo(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("responseContent:" + responseContent);
    String resultForGetstatsinfo = responseContent.getString("Error");
    logger.info("resultForGetstatsinfo:" + resultForGetstatsinfo);
    Assert.assertEquals(resultForGetstatsinfo, "this API is unavailable due to config");
  }


  /** constructor. */
  @Test(enabled = true, description = "Rate limit global qps for all jsonrpc api")
  public void test08GlobalQpsRate() {

    Long startTimeStamp = System.currentTimeMillis();
    Integer repeatTimes = 0;
    while (repeatTimes++ < 100) {
      JsonArray params = new JsonArray();
      params.add("0x" + ByteArray.toHexString(foundationAccountAddress).substring(2));
      params.add("latest");
      JsonObject requestBody = getJsonRpcBody("eth_getBalance", params);
      response = getJsonRpc(jsonRpcNode, requestBody);
      responseContent = HttpMethed.parseResponseContent(response);
      String balance = responseContent.getString("result").substring(2);
      Assert.assertTrue(balance.contains("0x"));
    }
    Long endTimesStamp = System.currentTimeMillis();
    logger.info("startTimeStamp - endTimesStap:" + (endTimesStamp - startTimeStamp));
    Assert.assertTrue(endTimesStamp - startTimeStamp > 7000);




  }


  /** constructor. */
  @Test(enabled = true, description = "Rate limit global qps for grpc api")
  public void test09GlobalQpsRateForJsonRpc() {

    Long startTimeStamp = System.currentTimeMillis();
    Integer repeatTimes = 0;
    while (repeatTimes++ < 100) {
      JsonArray params = new JsonArray();
      params.add("0x" + ByteArray.toHexString(foundationAccountAddress).substring(2));
      params.add("latest");
      JsonObject requestBody = getJsonRpcBody("eth_getBalance", params);
      response = getJsonRpc(jsonRpcNode, requestBody);
      responseContent = HttpMethed.parseResponseContent(response);
      String balance = responseContent.getString("result").substring(2);
      Assert.assertTrue(balance.contains("0x"));
    }
    Long endTimesStamp = System.currentTimeMillis();
    logger.info("startTimeStamp - endTimesStap:" + (endTimesStamp - startTimeStamp));
    Assert.assertTrue(endTimesStamp - startTimeStamp > 7000);




  }


  /** constructor. */
  @Test(enabled = true, description = "Rate limit global qps for grpc api")
  public void test10GlobalQpsRateForGrpc() {

    Long startTimeStamp = System.currentTimeMillis();
    Integer repeatTimes = 0;
    while (repeatTimes++ < 100) {
      Assert.assertTrue(PublicMethed.queryAccount(foundationAccountAddress,blockingStubFull)
          .getBalance() > 0);
    }
    Long endTimesStamp = System.currentTimeMillis();
    logger.info("startTimeStamp - endTimesStap:" + (endTimesStamp - startTimeStamp));
    Assert.assertTrue(endTimesStamp - startTimeStamp > 7000);




  }



  /** constructor. */
  @AfterClass
  public void shutdown() throws InterruptedException {}
}
