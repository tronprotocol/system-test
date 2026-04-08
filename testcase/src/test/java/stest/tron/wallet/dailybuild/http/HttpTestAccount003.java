package stest.tron.wallet.dailybuild.http;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.List;
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
public class HttpTestAccount003 {

  private static String updateAccountName = "updateAccount_" + System.currentTimeMillis();
  private static String updateUrl = "http://www.update.url" + System.currentTimeMillis();
  private final String testKey002 =
      Configuration.getByPath("testng.conf").getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethod.getFinalAddress(testKey002);
  private final String witnessKey001 =
      Configuration.getByPath("testng.conf").getString("witness.key1");
  private final byte[] witness1Address = PublicMethod.getFinalAddress(witnessKey001);
  private final String witnessKey002 =
      Configuration.getByPath("testng.conf").getString("witness.key2");
  private final byte[] witness2Address = PublicMethod.getFinalAddress(witnessKey002);
  private final Long createWitnessAmount =
      Configuration.getByPath("testng.conf").getLong("defaultParameter.createWitnessAmount");
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] newAccountAddress = ecKey1.getAddress();
  String newAccountKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] updateAccountAddress = ecKey2.getAddress();
  String updateAccountKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  Long amount = 50000000L;
  JsonArray voteKeys = new JsonArray();
  JsonObject voteElement = new JsonObject();
  private JSONObject responseContent;

  private Long frozenBalance = 40000000L;
  private HttpResponse response;
  private String httpnode =
      Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list").get(0);
  private String httpSoliditynode =
      Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list").get(2);
  private String httpPbftNode =
      Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list").get(4);

  /** constructor. */
  @Test(enabled = true, description = "Update account by http", groups = {"daily", "serial"})
  public void test01UpdateAccount() {
    response = HttpMethod.sendCoin(httpnode, fromAddress, updateAccountAddress, amount, testKey002);
    responseContent = HttpMethod.parseResponseContent(response);

    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);

    response =
        HttpMethod.updateAccount(
            httpnode, updateAccountAddress, updateAccountName, updateAccountKey);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);

    response = HttpMethod.getAccount(httpnode, updateAccountAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(
        responseContent
            .getString("account_name")
            .equalsIgnoreCase(HttpMethod.str2hex(updateAccountName)));

    Assert.assertFalse(responseContent.getString("active_permission").isEmpty());
  }

  /** constructor. */
  @Test(enabled = true, description = "Vote witness account by http", groups = {"daily", "serial"})
  public void test02VoteWitnessAccount() {
    // Freeze balance
    response =
        HttpMethod.freezeBalance(httpnode, updateAccountAddress, frozenBalance, 0,
            HttpMethod.proposalTronPowerIsOpen(httpnode) ? 2 : 0, updateAccountKey);
    responseContent = HttpMethod.parseResponseContent(response);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.printJsonContent(responseContent);
    HttpMethod.waitToProduceOneBlock(httpnode);
    voteElement.addProperty("vote_address", ByteArray.toHexString(witness1Address));
    voteElement.addProperty("vote_count", 11);
    voteKeys.add(voteElement);

    voteElement.remove("vote_address");
    voteElement.remove("vote_count");
    voteElement.addProperty("vote_address", ByteArray.toHexString(witness2Address));
    voteElement.addProperty("vote_count", 12);
    voteKeys.add(voteElement);

    response =
        HttpMethod.voteWitnessAccount(httpnode, updateAccountAddress, voteKeys, updateAccountKey);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    response = HttpMethod.getAccount(httpnode, updateAccountAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(!responseContent.getString("votes").isEmpty());
  }

  /** constructor. */
  @Test(enabled = true, description = "List witnesses by http", groups = {"daily", "serial"})
  public void test03ListWitness() {
    response = HttpMethod.listwitnesses(httpnode);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    JSONArray jsonArray = responseContent.getJSONArray("witnesses");
    for (int i = 0; i < jsonArray.size(); i++) {
      Assert.assertTrue(jsonArray.getJSONObject(i).getString("address").startsWith("41"));
    }
  }

  /** constructor. */
  @Test(enabled = true, description = "List witnesses by http with visible is true", groups = {"daily", "serial"})
  public void test04ListWitness() {
    response = HttpMethod.listwitnesses(httpnode, true);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    JSONArray jsonArray = responseContent.getJSONArray("witnesses");
    for (int i = 0; i < responseContent.size(); i++) {
      Assert.assertTrue(jsonArray.getJSONObject(i).getString("address").startsWith("41"));
    }
  }

  /** constructor. */
  @Test(enabled = true, description = "List witnesses from solidity by http", groups = {"daily", "serial"})
  public void test05ListWitnessFromSolidity() {
    HttpMethod.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    response = HttpMethod.listwitnessesFromSolidity(httpSoliditynode);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    JSONArray jsonArray = JSONArray.parseArray(responseContent.getString("witnesses"));
    Assert.assertTrue(jsonArray.size() >= 2);
  }

  /** constructor. */
  @Test(enabled = true, description = "List witnesses from PBFT by http", groups = {"daily", "serial"})
  public void test06ListWitnessFromPbft() {
    HttpMethod.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    response = HttpMethod.listwitnessesFromPbft(httpPbftNode);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    JSONArray jsonArray = JSONArray.parseArray(responseContent.getString("witnesses"));
    Assert.assertTrue(jsonArray.size() >= 2);
  }

  /** constructor. */
  @Test(enabled = true, description = "Update witness by http", groups = {"daily", "serial"})
  public void test07UpdateWitness() {
    response = HttpMethod.updateWitness(httpnode, witness2Address, updateUrl, witnessKey002);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);

    response = HttpMethod.listwitnesses(httpnode);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.getString("witnesses").indexOf(updateUrl) != -1);
    // logger.info("result is " + responseContent.getString("witnesses").indexOf(updateUrl));
  }

  /** constructor. */
  @Test(enabled = true, description = "Create account by http", groups = {"daily", "serial"})
  public void test08CreateAccount() {
    PublicMethod.printAddress(newAccountKey);
    response = HttpMethod.createAccount(httpnode, fromAddress, newAccountAddress, testKey002);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    response = HttpMethod.getAccount(httpnode, newAccountAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.getLong("create_time") > 3);
  }

  /** constructor. */
  @Test(enabled = true, description = "Create witness by http", groups = {"daily", "serial"})
  public void test09CreateWitness() {
    response =
        HttpMethod.sendCoin(
            httpnode, fromAddress, newAccountAddress, createWitnessAmount, testKey002);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    PublicMethod.printAddress(newAccountKey);

    response = HttpMethod.createWitness(httpnode, newAccountAddress, updateUrl);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(!responseContent.getString("txID").isEmpty());
  }

  /** constructor. */
  @Test(enabled = true, description = "Withdraw by http", groups = {"daily", "serial"})
  public void test10Withdraw() {
    response = HttpMethod.withdrawBalance(httpnode, witness1Address);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(
        responseContent.getString("Error").indexOf("is a guard representative") != -1);
  }

  /** constructor. */
  @Test(enabled = true, description = "Unfreeze balance for tron power by http", groups = {"daily", "serial"})
  public void test11UnfreezeTronPower() {
    response = HttpMethod.unFreezeBalance(httpnode, updateAccountAddress, frozenBalance,2, updateAccountKey);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
  }


  // TODO: Enable when HttpMethod supports getPaginatedNowWitnessList (v4.8.1+ API)
  // This test calls methods that don't exist yet in HttpMethod.
  // Commented out to allow compilation. Uncomment when the API is available.
  /*
  @Test(enabled = false, description = "List witness realTime vote data", groups = {"daily", "serial"})
  public void test12CheckVoteChangesRealtimeAfterVote(){
    // Uses HttpMethod.getPaginatedNowWitnessList / getPaginatedNowWitnessListSolidity
    // which are not yet implemented.
  }
  */

  /** constructor. */
  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethod.freeResource(httpnode, updateAccountAddress, fromAddress, updateAccountKey);
    HttpMethod.disConnect();
  }
}
