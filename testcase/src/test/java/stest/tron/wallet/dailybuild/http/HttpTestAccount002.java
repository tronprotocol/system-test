package stest.tron.wallet.dailybuild.http;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.HttpMethod;
import stest.tron.wallet.common.client.utils.ProposalEnum;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;

@Slf4j
public class HttpTestAccount002 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethod.getFinalAddress(testKey002);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] freezeBalanceAddress = ecKey1.getAddress();
  String freezeBalanceKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] receiverResourceAddress = ecKey2.getAddress();
  String receiverResourceKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  Long berforeBalance;
  Long afterBalance;
  Long amount = 10000000L;
  Long frozenBalance = 2000000L;
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
  @Test(enabled = true, description = "FreezeBalance for bandwidth by http", groups = {"daily", "serial"})
  public void test001FreezeBalanceForBandwidth() {
    PublicMethod.printAddress(freezeBalanceKey);
    //Send trx to test account
    response = HttpMethod.sendCoin(httpnode, fromAddress, freezeBalanceAddress, amount, testKey002);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    berforeBalance = HttpMethod.getBalance(httpnode, freezeBalanceAddress);

    //Freeze balance
    response = HttpMethod
        .freezeBalance(httpnode, freezeBalanceAddress, frozenBalance, 0, 0, freezeBalanceKey);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethod.getBalance(httpnode, freezeBalanceAddress);
    Assert.assertTrue(berforeBalance - afterBalance == frozenBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "UnFreezeBalance for bandwidth by http", groups = {"daily", "serial"})
  public void test002UnFreezeBalanceForBandwidth() {
    berforeBalance = HttpMethod.getBalance(httpnode, freezeBalanceAddress);

    //UnFreeze balance for bandwidth
    if(HttpMethod.getProposalValue(httpnode,ProposalEnum.GetUnfreezeDelayDays.getProposalName()) > 0) {
      response = HttpMethod.unFreezeBalanceV2(httpnode, freezeBalanceAddress, frozenBalance,0, freezeBalanceKey);
    } else {
      response = HttpMethod.unFreezeBalance(httpnode, freezeBalanceAddress, frozenBalance,0, freezeBalanceKey);
    }

    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethod.getBalance(httpnode, freezeBalanceAddress);
    if(HttpMethod.getProposalValue(httpnode,ProposalEnum.GetUnfreezeDelayDays.getProposalName()) > 0) {
      Assert.assertEquals(afterBalance,berforeBalance);
    } else {
      Assert.assertTrue(afterBalance - berforeBalance == frozenBalance);
    }

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "FreezeBalance for energy by http", groups = {"daily", "serial"})
  public void test003FreezeBalanceForEnergy() {
    berforeBalance = HttpMethod.getBalance(httpnode, freezeBalanceAddress);

    //Freeze balance for energy
    response = HttpMethod
        .freezeBalance(httpnode, freezeBalanceAddress, frozenBalance, 0, 1, freezeBalanceKey);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethod.getBalance(httpnode, freezeBalanceAddress);
    Assert.assertTrue(berforeBalance - afterBalance == frozenBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "UnFreezeBalance for energy by http", groups = {"daily", "serial"})
  public void test004UnFreezeBalanceForEnergy() {

    berforeBalance = HttpMethod.getBalance(httpnode, freezeBalanceAddress);
    HttpMethod.waitToProduceOneBlock(httpnode);
    //UnFreeze balance for energy
    if(HttpMethod.getProposalValue(httpnode,ProposalEnum.GetUnfreezeDelayDays.getProposalName()) > 0) {
      response = HttpMethod.unFreezeBalanceV2(httpnode, freezeBalanceAddress, frozenBalance,1, freezeBalanceKey);
    } else {
      response = HttpMethod.unFreezeBalance(httpnode, freezeBalanceAddress, frozenBalance,1, freezeBalanceKey);
    }

    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethod.getBalance(httpnode, freezeBalanceAddress);
    if(HttpMethod.getProposalValue(httpnode,ProposalEnum.GetUnfreezeDelayDays.getProposalName()) > 0) {
      Assert.assertEquals(afterBalance,berforeBalance);
    } else {
      Assert.assertTrue(afterBalance - berforeBalance == frozenBalance);
    }
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "FreezeBalance with bandwidth for others by http", groups = {"daily", "serial"})
  public void test005FreezeBalanceOfBandwidthForOthers() {
    response = HttpMethod
        .sendCoin(httpnode, fromAddress, receiverResourceAddress, amount, testKey002);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    berforeBalance = HttpMethod.getBalance(httpnode, freezeBalanceAddress);

    //Freeze balance with bandwidth for others
    response = HttpMethod
        .freezeBalance(httpnode, freezeBalanceAddress, frozenBalance, 0, 0, receiverResourceAddress,
            freezeBalanceKey);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethod.getBalance(httpnode, freezeBalanceAddress);
    logger.info("berforeBalance:" + berforeBalance);
    logger.info("afterBalance:" + afterBalance);
    logger.info("frozenBalance:" + frozenBalance);

    Assert.assertTrue(berforeBalance - afterBalance == frozenBalance);
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get Delegated Resource by http", groups = {"daily", "serial"})
  public void test006GetDelegatedResource() {
    if(HttpMethod.proposalFreezeV2IsOpen(httpnode)) {
      throw new SkipException("Skipping this freezeV1 test case");
    }
    response = HttpMethod
        .getDelegatedResource(httpnode, freezeBalanceAddress, receiverResourceAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    JSONArray jsonArray = JSONArray.parseArray(responseContent.get("delegatedResource").toString());
    Assert.assertTrue(jsonArray.size() >= 1);
    Assert.assertEquals(jsonArray.getJSONObject(0).getString("from"),
        ByteArray.toHexString(freezeBalanceAddress));
    Assert.assertEquals(jsonArray.getJSONObject(0).getString("to"),
        ByteArray.toHexString(receiverResourceAddress));
    Assert.assertEquals(jsonArray.getJSONObject(0).getLong("frozen_balance_for_bandwidth"),
        frozenBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get Delegated Resource from solidity by http", groups = {"daily", "serial"})
  public void test007GetDelegatedResourceFromSolidity() {
    if(HttpMethod.proposalFreezeV2IsOpen(httpnode)) {
      throw new SkipException("Skipping this freezeV1 test case");
    }
    HttpMethod.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    HttpMethod.waitToProduceOneBlockFromPbft(httpnode, httpPbftNode);
    response = HttpMethod.getDelegatedResourceFromSolidity(httpSoliditynode, freezeBalanceAddress,
        receiverResourceAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    JSONArray jsonArray = JSONArray.parseArray(responseContent.get("delegatedResource").toString());
    Assert.assertTrue(jsonArray.size() >= 1);
    Assert.assertEquals(jsonArray.getJSONObject(0).getString("from"),
        ByteArray.toHexString(freezeBalanceAddress));
    Assert.assertEquals(jsonArray.getJSONObject(0).getString("to"),
        ByteArray.toHexString(receiverResourceAddress));
    Assert.assertEquals(jsonArray.getJSONObject(0).getLong("frozen_balance_for_bandwidth"),
        frozenBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get Delegated Resource from PBFT by http", groups = {"daily", "serial"})
  public void test008GetDelegatedResourceFromPbft() {
    if(HttpMethod.proposalFreezeV2IsOpen(httpnode)) {
      throw new SkipException("Skipping this freezeV1 test case");
    }
    HttpMethod.waitToProduceOneBlockFromPbft(httpnode, httpPbftNode);
    response = HttpMethod
        .getDelegatedResourceFromPbft(httpPbftNode, freezeBalanceAddress, receiverResourceAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    JSONArray jsonArray = JSONArray.parseArray(responseContent.get("delegatedResource").toString());
    Assert.assertTrue(jsonArray.size() >= 1);
    Assert.assertEquals(jsonArray.getJSONObject(0).getString("from"),
        ByteArray.toHexString(freezeBalanceAddress));
    Assert.assertEquals(jsonArray.getJSONObject(0).getString("to"),
        ByteArray.toHexString(receiverResourceAddress));
    Assert.assertEquals(jsonArray.getJSONObject(0).getLong("frozen_balance_for_bandwidth"),
        frozenBalance);
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get Delegated Resource Account Index by http", groups = {"daily", "serial"})
  public void test009GetDelegatedResourceAccountIndex() {
    if(HttpMethod.proposalFreezeV2IsOpen(httpnode)) {
      throw new SkipException("Skipping this freezeV1 test case");
    }
    response = HttpMethod.getDelegatedResourceAccountIndex(httpnode, freezeBalanceAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertFalse(responseContent.get("toAccounts").toString().isEmpty());
    String toAddress = responseContent.getJSONArray("toAccounts").get(0).toString();
    Assert.assertEquals(toAddress, ByteArray.toHexString(receiverResourceAddress));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get Delegated Resource Account Index from solidity by http", groups = {"daily", "serial"})
  public void test010GetDelegatedResourceAccountIndexFromSolidity() {
    if(HttpMethod.proposalFreezeV2IsOpen(httpnode)) {
      throw new SkipException("Skipping this freezeV1 test case");
    }
    response = HttpMethod
        .getDelegatedResourceAccountIndexFromSolidity(httpSoliditynode, freezeBalanceAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertFalse(responseContent.get("toAccounts").toString().isEmpty());
    String toAddress = responseContent.getJSONArray("toAccounts").get(0).toString();
    Assert.assertEquals(toAddress, ByteArray.toHexString(receiverResourceAddress));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get Delegated Resource Account Index from PBFT by http", groups = {"daily", "serial"})
  public void test011GetDelegatedResourceAccountIndexFromPbft() {
    if(HttpMethod.proposalFreezeV2IsOpen(httpnode)) {
      throw new SkipException("Skipping this freezeV1 test case");
    }
    response = HttpMethod
        .getDelegatedResourceAccountIndexFromPbft(httpPbftNode, freezeBalanceAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertFalse(responseContent.get("toAccounts").toString().isEmpty());
    String toAddress = responseContent.getJSONArray("toAccounts").get(0).toString();
    Assert.assertEquals(toAddress, ByteArray.toHexString(receiverResourceAddress));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "UnFreezeBalance with bandwidth for others by http", groups = {"daily", "serial"})
  public void test012UnFreezeBalanceOfBandwidthForOthers() {
    HttpMethod.waitToProduceOneBlock(httpnode);
    HttpMethod.waitToProduceOneBlock(httpnode);
    HttpMethod.waitToProduceOneBlock(httpnode);
    berforeBalance = HttpMethod.getBalance(httpnode, freezeBalanceAddress);
    //UnFreeze balance with bandwidth for others
    response = HttpMethod
        .unFreezeBalance(httpnode, freezeBalanceAddress, frozenBalance,0, receiverResourceAddress,
            freezeBalanceKey);
    logger.info(HttpMethod.parseResponseContent(response).toJSONString());
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethod.getBalance(httpnode, freezeBalanceAddress);
    if(HttpMethod.getProposalValue(httpnode,ProposalEnum.GetUnfreezeDelayDays.getProposalName()) == 0) {
      Assert.assertTrue(afterBalance - berforeBalance == frozenBalance);
    } else {
      logger.info("afterBalance:" + afterBalance);
      logger.info("berforeBalance:" + berforeBalance);
      //another case's unfreeze balance has been expired
      Assert.assertTrue(afterBalance == berforeBalance + frozenBalance);

    }
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "FreezeBalance with energy for others by http", groups = {"daily", "serial"})
  public void test013FreezeBalanceOfEnergyForOthers() {
    response = HttpMethod
        .sendCoin(httpnode, fromAddress, receiverResourceAddress, amount, testKey002);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    response = HttpMethod
        .freezeBalance(httpnode, fromAddress, 1000000000L, 0, 0, freezeBalanceAddress,
            testKey002);
    HttpMethod.waitToProduceOneBlock(httpnode);
    berforeBalance = HttpMethod.getBalance(httpnode, freezeBalanceAddress);

    //Freeze balance with energy for others
    response = HttpMethod
        .freezeBalance(httpnode, freezeBalanceAddress, frozenBalance, 0, 1, receiverResourceAddress,
            freezeBalanceKey);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethod.getBalance(httpnode, freezeBalanceAddress);
    Assert.assertTrue(berforeBalance - afterBalance == frozenBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "UnFreezeBalance with energy for others by http", groups = {"daily", "serial"})
  public void test014UnFreezeBalanceOfEnergyForOthers() {
    if(HttpMethod.proposalFreezeV2IsOpen(httpnode)) {
      throw new SkipException("Skipping this freezeV1 test case");
    }
    berforeBalance = HttpMethod.getBalance(httpnode, freezeBalanceAddress);

    //UnFreeze balance with energy for others
    response = HttpMethod
        .unFreezeBalance(httpnode, freezeBalanceAddress, frozenBalance,1, receiverResourceAddress,
            freezeBalanceKey);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethod.getBalance(httpnode, freezeBalanceAddress);
    if(HttpMethod.getProposalValue(httpnode,ProposalEnum.GetUnfreezeDelayDays.getProposalName()) == 0) {
      Assert.assertTrue(afterBalance - berforeBalance == frozenBalance);
    } else {
      Assert.assertEquals(afterBalance, berforeBalance);
    }
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "FreezeBalance for tron power by http", groups = {"daily", "serial"})
  public void test015FreezeTronPower() {
    if(HttpMethod.proposalFreezeV2IsOpen(httpnode)) {
      throw new SkipException("Skipping this freezeV1 test case");
    }
    if(HttpMethod.getProposalValue(httpnode, ProposalEnum.GetAllowNewResourceModel.getProposalName()) == 1) {
      return;
    }
    HttpMethod.waitToProduceOneBlock(httpnode);
    berforeBalance = HttpMethod.getBalance(httpnode, freezeBalanceAddress);

    response = HttpMethod
        .freezeBalance(httpnode, freezeBalanceAddress, frozenBalance, 0,
            HttpMethod.proposalTronPowerIsOpen(httpnode) ? 2 : 0, null,
            freezeBalanceKey);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethod.getBalance(httpnode, freezeBalanceAddress);
    Assert.assertTrue(berforeBalance - afterBalance == frozenBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "UnFreezeBalance for tron power by http", groups = {"daily", "serial"})
  public void test016UnFreezeBalanceForTronPower() {
    if(HttpMethod.proposalFreezeV2IsOpen(httpnode)) {
      throw new SkipException("Skipping this freezeV1 test case");
    }
    if(HttpMethod.getProposalValue(httpnode, ProposalEnum.GetAllowNewResourceModel.getProposalName()) == 1) {
      return;
    }
    berforeBalance = HttpMethod.getBalance(httpnode, freezeBalanceAddress);

    //UnFreeze balance with energy for others

    response = HttpMethod
        .unFreezeBalance(httpnode, freezeBalanceAddress, frozenBalance,HttpMethod.proposalTronPowerIsOpen(httpnode) ? 2 : 0, null,
            freezeBalanceKey);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethod.getBalance(httpnode, freezeBalanceAddress);
    Assert.assertTrue(afterBalance - berforeBalance == frozenBalance);
  }




  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    response = HttpMethod
        .unFreezeBalance(httpnode, fromAddress, frozenBalance,0, freezeBalanceAddress,
            testKey002);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.freeResource(httpnode, freezeBalanceAddress, fromAddress, freezeBalanceKey);
    HttpMethod.disConnect();
  }
}