package stest.tron.wallet.dailybuild.http;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.*;

import java.util.HashMap;


/**
 *
 */
@Slf4j
public class HttpTestFreezeV2002 {

  private final String testKeyFrom = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKeyFrom);
  private final String testWitnessKey = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] testWitnessAddress = PublicMethed.getFinalAddress(testWitnessKey);

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] testAddress001 = ecKey1.getAddress();
  String testKey001 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

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
  @BeforeClass(enabled = true)
  public void beforeClass() {
    if (!HttpMethed.proposalFreezeV2IsOpen(httpnode)) {
      throw new SkipException("Skipping this freezeV2 test case");
    }
    PublicMethed.printAddress(testKey001);
    response = HttpMethed
        .sendCoin(httpnode, fromAddress, testAddress001, 1000000000L, testKeyFrom);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);

    response = HttpMethed.freezeBalance(httpnode, testAddress001,20000000L, 0, 0,  testKey001);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    response = HttpMethed.freezeBalance(httpnode, testAddress001,20000000L, 0, 1,  testKey001);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);

  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "cancel all unfrozen net")
  public void test001CancelAllUnfreezeNet() {
    response = HttpMethed.unFreezeBalanceV2(httpnode, testAddress001, 1000000L, 0, testKey001);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getAccount(httpnode, testAddress001);
    responseContent = HttpMethed.parseResponseContent(response);
    long beforeBalance = responseContent.getLongValue("balance");
    Assert.assertEquals(responseContent.getJSONArray("unfrozenV2").size(), 1);
    long beforeUnfreezeNet = responseContent.getJSONArray("unfrozenV2").getJSONObject(0).getLongValue("unfreeze_amount");
    Assert.assertEquals(beforeUnfreezeNet, 1000000);
    response = HttpMethed.getAccountReource(httpnode, testAddress001);
    responseContent = HttpMethed.parseResponseContent(response);
    long powerLimit1 = responseContent.getLongValue("tronPowerLimit");
    Assert.assertEquals(powerLimit1, 39);

    response = HttpMethed.cancelAllUnfreezeBalanceV2(httpnode, testAddress001, testKey001);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("test001 cancel res: " + responseContent.toJSONString());
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    String txid = responseContent.getString("txid");
    responseContent = HttpMethed.parseResponseContent(HttpMethed.getTransactionInfoById(httpnode, txid));
    logger.info("test001 cancelAllUnfreeze info: " + responseContent.toJSONString());
    JSONArray cancelArray = responseContent.getJSONArray("cancel_unfreezeV2_amount");
    Assert.assertEquals(cancelArray.size(), 3);
    for(int i=0;i<3;i++){
      JSONObject tem = cancelArray.getJSONObject(i);
      if(tem.getString("key").equals("BANDWIDTH")){
        Assert.assertEquals(tem.getLongValue("value"), 1000000);
      }else {
        Assert.assertEquals(tem.getLongValue("value"), 0);
      }
    }
    Assert.assertEquals(1000000, responseContent.getLongValue("cancel_all_unfreezeV2_amount"));
    Assert.assertFalse(responseContent.containsKey("withdraw_expire_amount"));

    response = HttpMethed.getAccount(httpnode, testAddress001);
    responseContent = HttpMethed.parseResponseContent(response);
    System.out.println(responseContent.toJSONString());
    long afterBalance = responseContent.getLongValue("balance");
    Assert.assertFalse(responseContent.containsKey("unfrozenV2"));
    long afterUnfreezeNet = responseContent.getJSONArray("frozenV2").getJSONObject(0).getLongValue("amount");
    long afterUnfreezeEnergy = responseContent.getJSONArray("frozenV2").getJSONObject(1).getLongValue("amount");
    Assert.assertEquals(afterUnfreezeNet, 20000000);
    Assert.assertEquals(afterUnfreezeEnergy, 20000000);
    response = HttpMethed.getAccountReource(httpnode, testAddress001);
    responseContent = HttpMethed.parseResponseContent(response);
    long powerLimit2 = responseContent.getLongValue("tronPowerLimit");
    Assert.assertEquals(beforeBalance, afterBalance);
    Assert.assertEquals(powerLimit2, 40);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "cancel all unfrozen energy")
  public void test002CancelAllUnfreezeEnergy() {
    response = HttpMethed.unFreezeBalanceV2(httpnode, testAddress001, 2000000L, 1, testKey001);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);

    response = HttpMethed.getAccount(httpnode, testAddress001);
    responseContent = HttpMethed.parseResponseContent(response);
    long beforeBalance = responseContent.getLongValue("balance");
    Assert.assertEquals(responseContent.getJSONArray("unfrozenV2").size(), 1);
    long beforeUnfreezeNet = responseContent.getJSONArray("unfrozenV2").getJSONObject(0).getLongValue("unfreeze_amount");
    Assert.assertEquals(beforeUnfreezeNet, 2000000);
    response = HttpMethed.getAccountReource(httpnode, testAddress001);
    responseContent = HttpMethed.parseResponseContent(response);
    long powerLimit1 = responseContent.getLongValue("tronPowerLimit");
    Assert.assertEquals(powerLimit1, 38);

    response = HttpMethed.cancelAllUnfreezeBalanceV2(httpnode, testAddress001, testKey001);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("test002 cancel res: " + responseContent.toJSONString());
    HttpMethed.waitToProduceOneBlock(httpnode);
    String txid = responseContent.getString("txid");
    responseContent = HttpMethed.parseResponseContent(HttpMethed.getTransactionInfoById(httpnode, txid));
    logger.info("test002 cancelAllUnfreeze info: " + responseContent.toJSONString());
    JSONArray cancelArray = responseContent.getJSONArray("cancel_unfreezeV2_amount");
    Assert.assertEquals(cancelArray.size(), 3);
    for(int i=0;i<3;i++){
      JSONObject tem = cancelArray.getJSONObject(i);
      if(tem.getString("key").equals("ENERGY")){
        Assert.assertEquals(tem.getLongValue("value"), 2000000);
      }else {
        Assert.assertEquals(tem.getLongValue("value"), 0);
      }
    }
    Assert.assertFalse(responseContent.containsKey("withdraw_expire_amount"));

    response = HttpMethed.getAccount(httpnode, testAddress001);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info(responseContent.toJSONString());
    long afterBalance = responseContent.getLongValue("balance");
    Assert.assertFalse(responseContent.containsKey("unfrozenV2"));
    long afterUnfreezeNet = responseContent.getJSONArray("frozenV2").getJSONObject(0).getLongValue("amount");
    long afterUnfreezeEnergy = responseContent.getJSONArray("frozenV2").getJSONObject(1).getLongValue("amount");
    Assert.assertEquals(afterUnfreezeNet, 20000000);
    Assert.assertEquals(afterUnfreezeEnergy, 20000000);
    response = HttpMethed.getAccountReource(httpnode, testAddress001);
    responseContent = HttpMethed.parseResponseContent(response);
    long powerLimit2 = responseContent.getLongValue("tronPowerLimit");
    Assert.assertEquals(beforeBalance, afterBalance);
    Assert.assertEquals(powerLimit2, 40);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "cancel all unexpired unfreeze net and energy")
  public void test03CancelAllUnfreezeNetAndEnergy() {
    response = HttpMethed.unFreezeBalanceV2(httpnode, testAddress001, 2000000L, 0, testKey001);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    response = HttpMethed.unFreezeBalanceV2(httpnode, testAddress001, 2000000L, 1, testKey001);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);

    response = HttpMethed.getAccount(httpnode, testAddress001);
    responseContent = HttpMethed.parseResponseContent(response);
    long beforeBalance = responseContent.getLongValue("balance");
    Assert.assertEquals(responseContent.getJSONArray("unfrozenV2").size(), 2);
    long beforeUnfreezeNet = responseContent.getJSONArray("unfrozenV2").getJSONObject(0).getLongValue("unfreeze_amount");
    long beforeUnfreezeEnergy = responseContent.getJSONArray("unfrozenV2").getJSONObject(1).getLongValue("unfreeze_amount");
    Assert.assertEquals(beforeUnfreezeNet, 2000000);
    Assert.assertEquals(beforeUnfreezeEnergy, 2000000);
    response = HttpMethed.getAccountReource(httpnode, testAddress001);
    responseContent = HttpMethed.parseResponseContent(response);
    long powerLimit1 = responseContent.getLongValue("tronPowerLimit");
    Assert.assertEquals(powerLimit1, 36);

    response = HttpMethed.cancelAllUnfreezeBalanceV2(httpnode, testAddress001, testKey001);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("test003 cancel res: " + responseContent.toJSONString());
    HttpMethed.waitToProduceOneBlock(httpnode);
    String txid = responseContent.getString("txid");
    responseContent = HttpMethed.parseResponseContent(HttpMethed.getTransactionInfoById(httpnode, txid));
    logger.info("test003 cancelAllUnfreeze info: " + responseContent.toJSONString());
    JSONArray cancelArray = responseContent.getJSONArray("cancel_unfreezeV2_amount");
    Assert.assertEquals(cancelArray.size(), 3);
    for (int i = 0; i < 3; i++) {
      JSONObject tem = cancelArray.getJSONObject(i);
      if (tem.getString("key").equals("TRON_POWER")) {
        Assert.assertEquals(tem.getLongValue("value"), 0);
      } else {
        Assert.assertEquals(tem.getLongValue("value"), 2000000);
      }
    }
    Assert.assertFalse(responseContent.containsKey("withdraw_expire_amount"));

    response = HttpMethed.getAccount(httpnode, testAddress001);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info(responseContent.toJSONString());
    long afterBalance = responseContent.getLongValue("balance");
    Assert.assertFalse(responseContent.containsKey("unfrozenV2"));
    long afterUnfreezeNet = responseContent.getJSONArray("frozenV2").getJSONObject(0).getLongValue("amount");
    long afterUnfreezeEnergy = responseContent.getJSONArray("frozenV2").getJSONObject(1).getLongValue("amount");
    Assert.assertEquals(afterUnfreezeNet, 20000000);
    Assert.assertEquals(afterUnfreezeEnergy, 20000000);
    response = HttpMethed.getAccountReource(httpnode, testAddress001);
    responseContent = HttpMethed.parseResponseContent(response);
    long powerLimit2 = responseContent.getLongValue("tronPowerLimit");
    Assert.assertEquals(beforeBalance, afterBalance);
    Assert.assertEquals(powerLimit2, 40);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "cancel 32  unexpired unfreeze")
  public void test04CancelAll32Unfreeze() {
    for(int i=0;i<32;i++){
      response = HttpMethed.unFreezeBalanceV2(httpnode, testAddress001, i + 1L, i % 2, testKey001);
      Assert.assertTrue(HttpMethed.verificationResult(response));
    }
    HttpMethed.waitToProduceOneBlock(httpnode);

    response = HttpMethed.getAccount(httpnode, testAddress001);
    responseContent = HttpMethed.parseResponseContent(response);
    long beforeBalance = responseContent.getLongValue("balance");
    Assert.assertEquals(responseContent.getJSONArray("unfrozenV2").size(), 32);
    response = HttpMethed.getAccountReource(httpnode, testAddress001);
    responseContent = HttpMethed.parseResponseContent(response);
    long powerLimit1 = responseContent.getLongValue("tronPowerLimit");
    Assert.assertEquals(powerLimit1, 39);

    response = HttpMethed.cancelAllUnfreezeBalanceV2(httpnode, testAddress001, testKey001);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info(responseContent.toJSONString());
    HttpMethed.waitToProduceOneBlock(httpnode);
    logger.info("test004 cancel res: " + responseContent.toJSONString());
    HttpMethed.waitToProduceOneBlock(httpnode);
    String txid = responseContent.getString("txid");
    responseContent = HttpMethed.parseResponseContent(HttpMethed.getTransactionInfoById(httpnode, txid));
    logger.info("test004 cancelAllUnfreeze info: " + responseContent.toJSONString());
    JSONArray cancelArray = responseContent.getJSONArray("cancel_unfreezeV2_amount");
    Assert.assertEquals(cancelArray.size(), 3);
    for (int i = 0; i < 3; i++) {
      JSONObject tem = cancelArray.getJSONObject(i);
      if (tem.getString("key").equals("BANDWIDTH")) {
        Assert.assertEquals(tem.getLongValue("value"), 256);
      }else if (tem.getString("key").equals("ENERGY")) {
        Assert.assertEquals(tem.getLongValue("value"), 272);
      } else {
        Assert.assertEquals(tem.getLongValue("value"), 0);
      }
    }
    Assert.assertFalse(responseContent.containsKey("withdraw_expire_amount"));

    response = HttpMethed.getAccount(httpnode, testAddress001);
    responseContent = HttpMethed.parseResponseContent(response);
    long afterBalance = responseContent.getLongValue("balance");
    Assert.assertFalse(responseContent.containsKey("unfrozenV2"));
    long afterUnfreezeNet = responseContent.getJSONArray("frozenV2").getJSONObject(0).getLongValue("amount");
    long afterUnfreezeEnergy = responseContent.getJSONArray("frozenV2").getJSONObject(1).getLongValue("amount");
    Assert.assertEquals(afterUnfreezeNet, 20000000);
    Assert.assertEquals(afterUnfreezeEnergy, 20000000);
    response = HttpMethed.getAccountReource(httpnode, testAddress001);
    responseContent = HttpMethed.parseResponseContent(response);
    long powerLimit2 = responseContent.getLongValue("tronPowerLimit");
    Assert.assertEquals(beforeBalance, afterBalance);
    Assert.assertEquals(powerLimit2, 40);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "vote after cancel all unfreeze")
  public void test05VoteAfterCancelAllUnfreeze() {
    JsonArray voteKeys = new JsonArray();
    JsonObject voteElement = new JsonObject();
    voteElement.addProperty("vote_address", ByteArray.toHexString(testWitnessAddress));
    voteElement.addProperty("vote_count", 40);
    voteKeys.add(voteElement);
    response = HttpMethed.voteWitnessAccount(httpnode, testAddress001, voteKeys, testKey001);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getAccount(httpnode, testAddress001);
    responseContent = HttpMethed.parseResponseContent(response);
    long afterUnfreezeNet = responseContent.getJSONArray("frozenV2").getJSONObject(0).getLongValue("amount");
    long afterUnfreezeEnergy = responseContent.getJSONArray("frozenV2").getJSONObject(1).getLongValue("amount");
    Assert.assertEquals(afterUnfreezeNet, 20000000);
    Assert.assertEquals(afterUnfreezeEnergy, 20000000);
    Assert.assertEquals(responseContent.getJSONArray("votes").size(), 1);
    String srAdd = responseContent.getJSONArray("votes").getJSONObject(0).getString("vote_address");
    int voteCount = responseContent.getJSONArray("votes").getJSONObject(0).getIntValue("vote_count");
    Assert.assertEquals(srAdd.toLowerCase(), ByteArray.toHexString(testWitnessAddress).toLowerCase());
    Assert.assertEquals(voteCount, 40);
  }



  /**
   * constructor.
   * */
  @AfterClass
  public void shutdown() throws InterruptedException {
    response = HttpMethed.getAccount(httpnode, testAddress001);
    responseContent = HttpMethed.parseResponseContent(response);
    for (int i = 0; i < responseContent.getJSONArray("frozenV2").size(); i++) {
      long amount = responseContent.getJSONArray("frozenV2").getJSONObject(i).getLongValue("amount");
      HttpMethed.unFreezeBalanceV2(httpnode, testAddress001, amount, i, testKey001);
    }
    HttpMethed.freedResource(httpnode, testAddress001, fromAddress, testKey001);
    HttpMethed.disConnect();
  }
}