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
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.ProposalEnum;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Utils;

@Slf4j
public class HttpTestAccount002 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
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
  @Test(enabled = true, description = "FreezeBalance for bandwidth by http")
  public void test001FreezeBalanceForBandwidth() {
    PublicMethed.printAddress(freezeBalanceKey);
    //Send trx to test account
    response = HttpMethed.sendCoin(httpnode, fromAddress, freezeBalanceAddress, amount, testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    berforeBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);

    //Freeze balance
    response = HttpMethed
        .freezeBalance(httpnode, freezeBalanceAddress, frozenBalance, 0, 0, freezeBalanceKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);
    Assert.assertTrue(berforeBalance - afterBalance == frozenBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "UnFreezeBalance for bandwidth by http")
  public void test002UnFreezeBalanceForBandwidth() {
    berforeBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);

    //UnFreeze balance for bandwidth
    if(HttpMethed.getProposalValue(httpnode,ProposalEnum.GetUnfreezeDelayDays.getProposalName()) > 0) {
      response = HttpMethed.unFreezeBalanceV2(httpnode, freezeBalanceAddress, frozenBalance,0, freezeBalanceKey);
    } else {
      response = HttpMethed.unFreezeBalance(httpnode, freezeBalanceAddress, frozenBalance,0, freezeBalanceKey);
    }

    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);
    if(HttpMethed.getProposalValue(httpnode,ProposalEnum.GetUnfreezeDelayDays.getProposalName()) > 0) {
      Assert.assertEquals(afterBalance,berforeBalance);
    } else {
      Assert.assertTrue(afterBalance - berforeBalance == frozenBalance);
    }

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "FreezeBalance for energy by http")
  public void test003FreezeBalanceForEnergy() {
    berforeBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);

    //Freeze balance for energy
    response = HttpMethed
        .freezeBalance(httpnode, freezeBalanceAddress, frozenBalance, 0, 1, freezeBalanceKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);
    Assert.assertTrue(berforeBalance - afterBalance == frozenBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "UnFreezeBalance for energy by http")
  public void test004UnFreezeBalanceForEnergy() {

    berforeBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);
    HttpMethed.waitToProduceOneBlock(httpnode);
    //UnFreeze balance for energy
    if(HttpMethed.getProposalValue(httpnode,ProposalEnum.GetUnfreezeDelayDays.getProposalName()) > 0) {
      response = HttpMethed.unFreezeBalanceV2(httpnode, freezeBalanceAddress, frozenBalance,1, freezeBalanceKey);
    } else {
      response = HttpMethed.unFreezeBalance(httpnode, freezeBalanceAddress, frozenBalance,1, freezeBalanceKey);
    }

    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);
    if(HttpMethed.getProposalValue(httpnode,ProposalEnum.GetUnfreezeDelayDays.getProposalName()) > 0) {
      Assert.assertEquals(afterBalance,berforeBalance);
    } else {
      Assert.assertTrue(afterBalance - berforeBalance == frozenBalance);
    }
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "FreezeBalance with bandwidth for others by http")
  public void test005FreezeBalanceOfBandwidthForOthers() {
    response = HttpMethed
        .sendCoin(httpnode, fromAddress, receiverResourceAddress, amount, testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    berforeBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);

    //Freeze balance with bandwidth for others
    response = HttpMethed
        .freezeBalance(httpnode, freezeBalanceAddress, frozenBalance, 0, 0, receiverResourceAddress,
            freezeBalanceKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);
    logger.info("berforeBalance:" + berforeBalance);
    logger.info("afterBalance:" + afterBalance);
    logger.info("frozenBalance:" + frozenBalance);

    Assert.assertTrue(berforeBalance - afterBalance == frozenBalance);
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get Delegated Resource by http")
  public void test006GetDelegatedResource() {
    if(HttpMethed.proposalFreezeV2IsOpen(httpnode)) {
      throw new SkipException("Skipping this freezeV1 test case");
    }
    response = HttpMethed
        .getDelegatedResource(httpnode, freezeBalanceAddress, receiverResourceAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
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
  @Test(enabled = true, description = "Get Delegated Resource from solidity by http")
  public void test007GetDelegatedResourceFromSolidity() {
    if(HttpMethed.proposalFreezeV2IsOpen(httpnode)) {
      throw new SkipException("Skipping this freezeV1 test case");
    }
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    HttpMethed.waitToProduceOneBlockFromPbft(httpnode, httpPbftNode);
    response = HttpMethed.getDelegatedResourceFromSolidity(httpSoliditynode, freezeBalanceAddress,
        receiverResourceAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
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
  @Test(enabled = true, description = "Get Delegated Resource from PBFT by http")
  public void test008GetDelegatedResourceFromPbft() {
    if(HttpMethed.proposalFreezeV2IsOpen(httpnode)) {
      throw new SkipException("Skipping this freezeV1 test case");
    }
    HttpMethed.waitToProduceOneBlockFromPbft(httpnode, httpPbftNode);
    response = HttpMethed
        .getDelegatedResourceFromPbft(httpPbftNode, freezeBalanceAddress, receiverResourceAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
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
  @Test(enabled = true, description = "Get Delegated Resource Account Index by http")
  public void test009GetDelegatedResourceAccountIndex() {
    if(HttpMethed.proposalFreezeV2IsOpen(httpnode)) {
      throw new SkipException("Skipping this freezeV1 test case");
    }
    response = HttpMethed.getDelegatedResourceAccountIndex(httpnode, freezeBalanceAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertFalse(responseContent.get("toAccounts").toString().isEmpty());
    String toAddress = responseContent.getJSONArray("toAccounts").get(0).toString();
    Assert.assertEquals(toAddress, ByteArray.toHexString(receiverResourceAddress));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get Delegated Resource Account Index from solidity by http")
  public void test010GetDelegatedResourceAccountIndexFromSolidity() {
    if(HttpMethed.proposalFreezeV2IsOpen(httpnode)) {
      throw new SkipException("Skipping this freezeV1 test case");
    }
    response = HttpMethed
        .getDelegatedResourceAccountIndexFromSolidity(httpSoliditynode, freezeBalanceAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertFalse(responseContent.get("toAccounts").toString().isEmpty());
    String toAddress = responseContent.getJSONArray("toAccounts").get(0).toString();
    Assert.assertEquals(toAddress, ByteArray.toHexString(receiverResourceAddress));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get Delegated Resource Account Index from PBFT by http")
  public void test011GetDelegatedResourceAccountIndexFromPbft() {
    if(HttpMethed.proposalFreezeV2IsOpen(httpnode)) {
      throw new SkipException("Skipping this freezeV1 test case");
    }
    response = HttpMethed
        .getDelegatedResourceAccountIndexFromPbft(httpPbftNode, freezeBalanceAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertFalse(responseContent.get("toAccounts").toString().isEmpty());
    String toAddress = responseContent.getJSONArray("toAccounts").get(0).toString();
    Assert.assertEquals(toAddress, ByteArray.toHexString(receiverResourceAddress));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "UnFreezeBalance with bandwidth for others by http")
  public void test012UnFreezeBalanceOfBandwidthForOthers() {
    HttpMethed.waitToProduceOneBlock(httpnode);
    HttpMethed.waitToProduceOneBlock(httpnode);
    HttpMethed.waitToProduceOneBlock(httpnode);
    berforeBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);
    //UnFreeze balance with bandwidth for others
    response = HttpMethed
        .unFreezeBalance(httpnode, freezeBalanceAddress, frozenBalance,0, receiverResourceAddress,
            freezeBalanceKey);
    logger.info(HttpMethed.parseResponseContent(response).toJSONString());
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);
    if(HttpMethed.getProposalValue(httpnode,ProposalEnum.GetUnfreezeDelayDays.getProposalName()) == 0) {
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
  @Test(enabled = true, description = "FreezeBalance with energy for others by http")
  public void test013FreezeBalanceOfEnergyForOthers() {
    response = HttpMethed
        .sendCoin(httpnode, fromAddress, receiverResourceAddress, amount, testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    response = HttpMethed
        .freezeBalance(httpnode, fromAddress, 1000000000L, 0, 0, freezeBalanceAddress,
            testKey002);
    HttpMethed.waitToProduceOneBlock(httpnode);
    berforeBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);

    //Freeze balance with energy for others
    response = HttpMethed
        .freezeBalance(httpnode, freezeBalanceAddress, frozenBalance, 0, 1, receiverResourceAddress,
            freezeBalanceKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);
    Assert.assertTrue(berforeBalance - afterBalance == frozenBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "UnFreezeBalance with energy for others by http")
  public void test014UnFreezeBalanceOfEnergyForOthers() {
    if(HttpMethed.proposalFreezeV2IsOpen(httpnode)) {
      throw new SkipException("Skipping this freezeV1 test case");
    }
    berforeBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);

    //UnFreeze balance with energy for others
    response = HttpMethed
        .unFreezeBalance(httpnode, freezeBalanceAddress, frozenBalance,1, receiverResourceAddress,
            freezeBalanceKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);
    if(HttpMethed.getProposalValue(httpnode,ProposalEnum.GetUnfreezeDelayDays.getProposalName()) == 0) {
      Assert.assertTrue(afterBalance - berforeBalance == frozenBalance);
    } else {
      Assert.assertEquals(afterBalance, berforeBalance);
    }
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "FreezeBalance for tron power by http")
  public void test015FreezeTronPower() {
    if(HttpMethed.proposalFreezeV2IsOpen(httpnode)) {
      throw new SkipException("Skipping this freezeV1 test case");
    }
    if(HttpMethed.getProposalValue(httpnode, ProposalEnum.GetAllowNewResourceModel.getProposalName()) == 1) {
      return;
    }
    HttpMethed.waitToProduceOneBlock(httpnode);
    berforeBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);

    response = HttpMethed
        .freezeBalance(httpnode, freezeBalanceAddress, frozenBalance, 0,
            HttpMethed.proposalTronPowerIsOpen(httpnode) ? 2 : 0, null,
            freezeBalanceKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);
    Assert.assertTrue(berforeBalance - afterBalance == frozenBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "UnFreezeBalance for tron power by http")
  public void test016UnFreezeBalanceForTronPower() {
    if(HttpMethed.proposalFreezeV2IsOpen(httpnode)) {
      throw new SkipException("Skipping this freezeV1 test case");
    }
    if(HttpMethed.getProposalValue(httpnode, ProposalEnum.GetAllowNewResourceModel.getProposalName()) == 1) {
      return;
    }
    berforeBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);

    //UnFreeze balance with energy for others

    response = HttpMethed
        .unFreezeBalance(httpnode, freezeBalanceAddress, frozenBalance,HttpMethed.proposalTronPowerIsOpen(httpnode) ? 2 : 0, null,
            freezeBalanceKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);
    Assert.assertTrue(afterBalance - berforeBalance == frozenBalance);
  }




  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    response = HttpMethed
        .unFreezeBalance(httpnode, fromAddress, frozenBalance,0, freezeBalanceAddress,
            testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.freedResource(httpnode, freezeBalanceAddress, fromAddress, freezeBalanceKey);
    HttpMethed.disConnect();
  }
}