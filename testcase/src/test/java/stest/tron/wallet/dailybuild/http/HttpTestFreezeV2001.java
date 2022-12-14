package stest.tron.wallet.dailybuild.http;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.ProposalEnum;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Utils;

@Slf4j
public class HttpTestFreezeV2001 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] freezeBandwidthAddress = ecKey1.getAddress();
  String freezeBandwidthKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] receiverResourceAddress = ecKey2.getAddress();
  String receiverResourceKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());


  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] freezeEnergyAddress = ecKey3.getAddress();
  String freezeEnergyKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
  
  
  Long beforeBalance;
  Long afterBalance;
  
  Long beforeFreezeBalance;
  Long afterFreezeBalance;
  Long amount = 40000000L;
  Long frozenBalance = 20000000L;
  Long frozenEnergyBalance = 30000000L;
  Long unfrozenBalance = frozenBalance / 10;
  Long unfrozenEnergyBalance = frozenEnergyBalance / 10;
  Long delegateAmount = 1000000L;
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
  @BeforeClass(enabled = true)
  public void beforeClass() {
    if(!HttpMethed.proposalFreezeV2IsOpen(httpnode)) {
      throw new SkipException("Skipping this freezeV2 test case");
    }

    PublicMethed.printAddress(freezeBandwidthKey);
    PublicMethed.printAddress(receiverResourceKey);
    //Send trx to test account
    response = HttpMethed.sendCoin(httpnode, fromAddress, freezeBandwidthAddress, amount, testKey002);
    response = HttpMethed.sendCoin(httpnode, fromAddress, freezeEnergyAddress, amount, testKey002);
    response = HttpMethed
        .sendCoin(httpnode, fromAddress, receiverResourceAddress, 1L, testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);


  }



  /**
   * constructor.
   */
  @Test(enabled = true, description = "FreezeBalanceV2 for bandwidth by http")
  public void test001FreezeBalanceV2ForBandwidth() {
    beforeBalance = HttpMethed.getBalance(httpnode, freezeBandwidthAddress);
    //FreezeBalanceV2 balance
    response = HttpMethed
        .freezeBalanceV2(httpnode, freezeBandwidthAddress, frozenBalance, 0,null, freezeBandwidthKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethed.getBalance(httpnode, freezeBandwidthAddress);
    Assert.assertTrue(beforeBalance - afterBalance == frozenBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "UnFreezeBalanceV2 for bandwidth by http")
  public void test002UnFreezeBalanceV2ForBandwidth() {
    beforeBalance = HttpMethed.getBalance(httpnode, freezeBandwidthAddress);
    //UnFreezeBalanceV2 for bandwidth
    response = HttpMethed.unFreezeBalanceV2(httpnode, freezeBandwidthAddress, unfrozenBalance,0, freezeBandwidthKey);

    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethed.getBalance(httpnode, freezeBandwidthAddress);
    Assert.assertEquals(afterBalance,beforeBalance);

    responseContent = HttpMethed.parseResponseContent(HttpMethed.getAccount(httpnode,freezeBandwidthAddress));
    JSONArray unfrozenV2 = responseContent.getJSONArray("unfrozenV2");
    Assert.assertTrue(unfrozenV2.size() == 1);

    Long unfreezeAmount = unfrozenV2.getJSONObject(0).getLong("unfreeze_amount");
    Long unfreezeExpireTime = unfrozenV2.getJSONObject(0).getLong("unfreeze_expire_time");
    logger.info("unfrozenBalance:" + unfrozenBalance);
    logger.info("unfreezeAmount:" + unfreezeAmount);
    logger.info("unfreezeExpireTime:" + unfreezeExpireTime);
    Assert.assertEquals(unfreezeAmount,unfrozenBalance);
    Assert.assertTrue(System.currentTimeMillis() < unfreezeExpireTime);
    Assert.assertTrue(!unfrozenV2.contains("type"));

    int retryTimes = 0;
    while (System.currentTimeMillis() < unfreezeExpireTime && retryTimes++ <= 100) {
      HttpMethed.waitToProduceOneBlock(httpnode);
    }

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "FreezeBalanceV2 for energy by http")
  public void test003FreezeBalanceForEnergy() {
    beforeBalance = HttpMethed.getBalance(httpnode, freezeEnergyAddress);

    //Freeze balance for energy
    response = HttpMethed
        .freezeBalanceV2(httpnode, freezeEnergyAddress, frozenEnergyBalance, 1, null, freezeEnergyKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethed.getBalance(httpnode, freezeEnergyAddress);
    Assert.assertTrue(beforeBalance - afterBalance == frozenEnergyBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "UnFreezeBalanceV2 for energy by http")
  public void test004UnFreezeBalanceForEnergy() {
    beforeBalance = HttpMethed.getBalance(httpnode, freezeEnergyAddress);
    HttpMethed.waitToProduceOneBlock(httpnode);
    //UnFreeze balance for energy
    response = HttpMethed.unFreezeBalanceV2(httpnode, freezeEnergyAddress, unfrozenEnergyBalance,1, freezeEnergyKey);

    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethed.getBalance(httpnode, freezeEnergyAddress);
    Assert.assertEquals(afterBalance,beforeBalance);

    responseContent = HttpMethed.parseResponseContent(HttpMethed.getAccount(httpnode,freezeEnergyAddress));
    JSONArray unfrozenV2 = responseContent.getJSONArray("unfrozenV2");
    Assert.assertTrue(unfrozenV2.size() == 1);

    Long unfreezeAmount = unfrozenV2.getJSONObject(0).getLong("unfreeze_amount");
    Long unfreezeExpireTime = unfrozenV2.getJSONObject(0).getLong("unfreeze_expire_time");
    logger.info("unfrozenBalance:" + unfrozenBalance);
    logger.info("unfreezeAmount:" + unfreezeAmount);
    Assert.assertEquals(unfreezeAmount,unfrozenEnergyBalance);
    Assert.assertTrue(System.currentTimeMillis() < unfreezeExpireTime);
    Assert.assertEquals(unfrozenV2.getJSONObject(0).getString("type"),"ENERGY");


  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "DelegateResource of bandwidth for others by http")
  public void test005DelegateResourceOfBandwidthForOthers() {
    responseContent = HttpMethed.parseResponseContent(HttpMethed.getAccount(httpnode,freezeBandwidthAddress));
    JSONArray frozenV2 = responseContent.getJSONArray("frozenV2");
    beforeFreezeBalance = frozenV2.getJSONObject(0).getLong("amount");

    //Freeze balance with bandwidth for others
    response = HttpMethed.delegateresource(httpnode,freezeBandwidthAddress,delegateAmount,0,receiverResourceAddress,freezeBandwidthKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    responseContent = HttpMethed.parseResponseContent(HttpMethed.getAccount(httpnode,freezeBandwidthAddress));
    frozenV2 = responseContent.getJSONArray("frozenV2");
    afterFreezeBalance = frozenV2.getJSONObject(0).getLong("amount");
    Assert.assertTrue(beforeFreezeBalance - afterFreezeBalance == delegateAmount);
    Long delegatedFrozenBalanceForBandwidth = responseContent.getLong("delegated_frozenV2_balance_for_bandwidth");
    Assert.assertEquals(delegatedFrozenBalanceForBandwidth,delegateAmount);
    responseContent = HttpMethed.parseResponseContent(HttpMethed.getAccount(httpnode,receiverResourceAddress));
    Long acquiredDelegatedFrozenBalanceForBandwidth = responseContent.getLong("acquired_delegated_frozenV2_balance_for_bandwidth");
    Assert.assertEquals(acquiredDelegatedFrozenBalanceForBandwidth,delegateAmount);


  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "UndelegateResource of bandwidth for others by http")
  public void test006UndelegateResourceOfBandwidthForOthers() {
    responseContent = HttpMethed.parseResponseContent(HttpMethed.getAccount(httpnode,freezeBandwidthAddress));
    JSONArray frozenV2 = responseContent.getJSONArray("frozenV2");
    beforeFreezeBalance = frozenV2.getJSONObject(0).getLong("amount");

    //Freeze balance with bandwidth for others
    response = HttpMethed.unDelegateresource(httpnode,freezeBandwidthAddress,delegateAmount,0,receiverResourceAddress,freezeBandwidthKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    responseContent = HttpMethed.parseResponseContent(HttpMethed.getAccount(httpnode,freezeBandwidthAddress));
    frozenV2 = responseContent.getJSONArray("frozenV2");
    afterFreezeBalance = frozenV2.getJSONObject(0).getLong("amount");
    Assert.assertTrue(beforeFreezeBalance - afterFreezeBalance == -delegateAmount);
    Long delegatedFrozenBalanceForBandwidth = responseContent.getLong("delegated_frozenV2_balance_for_bandwidth");
    Assert.assertNull(delegatedFrozenBalanceForBandwidth);
    responseContent = HttpMethed.parseResponseContent(HttpMethed.getAccount(httpnode,receiverResourceAddress));
    Long acquiredDelegatedFrozenBalanceForBandwidth = responseContent.getLong("acquired_delegated_frozenV2_balance_for_bandwidth");
    Assert.assertNull(acquiredDelegatedFrozenBalanceForBandwidth);


  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "DelegateResource of energy for others by http")
  public void test007DelegateResourceOfEnergyForOthers() {
    responseContent = HttpMethed.parseResponseContent(HttpMethed.getAccount(httpnode,freezeEnergyAddress));
    JSONArray frozenV2 = responseContent.getJSONArray("frozenV2");
    beforeFreezeBalance = frozenV2.getJSONObject(1).getLong("amount");

    //Freeze balance with bandwidth for others
    response = HttpMethed.delegateresource(httpnode,freezeEnergyAddress,delegateAmount,1,receiverResourceAddress,freezeEnergyKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    responseContent = HttpMethed.parseResponseContent(HttpMethed.getAccount(httpnode,freezeEnergyAddress));
    frozenV2 = responseContent.getJSONArray("frozenV2");
    afterFreezeBalance = frozenV2.getJSONObject(1).getLong("amount");
    Assert.assertTrue(beforeFreezeBalance - afterFreezeBalance == delegateAmount);
    Long delegatedFrozenBalanceForBandwidth = responseContent.getJSONObject("account_resource").getLong("delegated_frozenV2_balance_for_energy");
    Assert.assertEquals(delegatedFrozenBalanceForBandwidth,delegateAmount);
    responseContent = HttpMethed.parseResponseContent(HttpMethed.getAccount(httpnode,receiverResourceAddress));
    Long acquiredDelegatedFrozenBalanceForBandwidth = responseContent.getJSONObject("account_resource").getLong("acquired_delegated_frozenV2_balance_for_energy");
    Assert.assertEquals(acquiredDelegatedFrozenBalanceForBandwidth,delegateAmount);


  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "UndelegateResource of energy for others by http")
  public void test008UndelegateResourceOfEnergyForOthers() {
    responseContent = HttpMethed.parseResponseContent(HttpMethed.getAccount(httpnode,freezeEnergyAddress));
    JSONArray frozenV2 = responseContent.getJSONArray("frozenV2");
    beforeFreezeBalance = frozenV2.getJSONObject(1).getLong("amount");

    //Freeze balance with bandwidth for others
    response = HttpMethed.unDelegateresource(httpnode,freezeEnergyAddress,delegateAmount,1,receiverResourceAddress,freezeEnergyKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    responseContent = HttpMethed.parseResponseContent(HttpMethed.getAccount(httpnode,freezeEnergyAddress));
    frozenV2 = responseContent.getJSONArray("frozenV2");
    afterFreezeBalance = frozenV2.getJSONObject(1).getLong("amount");
    Assert.assertTrue(beforeFreezeBalance - afterFreezeBalance == -delegateAmount);
    Long delegatedFrozenBalanceForEnergy = responseContent.getJSONObject("account_resource").getLong("delegated_frozenV2_balance_for_energy");
    Assert.assertNull(delegatedFrozenBalanceForEnergy);
    responseContent = HttpMethed.parseResponseContent(HttpMethed.getAccount(httpnode,receiverResourceAddress));
    Long acquiredDelegatedFrozenBalanceForEnergy = responseContent.getJSONObject("account_resource").getLong("acquired_delegated_frozenV2_balance_for_energy");
    Assert.assertNull(acquiredDelegatedFrozenBalanceForEnergy);


  }



  /**
   * constructor.
   */
  @Test(enabled = true, description = "WithdrawExpireUnfreeze by http")
  public void test009WithdrawExpireUnfreeze() {
    responseContent = HttpMethed.parseResponseContent(HttpMethed.getAccount(httpnode,freezeBandwidthAddress));
    JSONArray unfrozenV2 = responseContent.getJSONArray("unfrozenV2");
    Long unfreezeExpireTime = unfrozenV2.getJSONObject(0).getLong("unfreeze_expire_time");
    int retryTimes = 0;
    while (System.currentTimeMillis() < unfreezeExpireTime && retryTimes++ <= 100) {
      HttpMethed.waitToProduceOneBlock(httpnode);
    }





    responseContent = HttpMethed.parseResponseContent(HttpMethed.getAccount(httpnode,freezeBandwidthAddress));
    unfrozenV2 = responseContent.getJSONArray("unfrozenV2");
    Long unfreezeAmount = unfrozenV2.getJSONObject(0).getLong("unfreeze_amount");
    Long beforeBalance = responseContent.getLong("balance");

    response = HttpMethed.withdrawExpireUnfreeze(httpnode,freezeBandwidthAddress,freezeBandwidthKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);


    responseContent = HttpMethed.parseResponseContent(HttpMethed.getAccount(httpnode,freezeBandwidthAddress));
    Assert.assertNull(responseContent.getJSONArray("unfrozenV2"));
    afterBalance = responseContent.getLong("balance");

    Assert.assertTrue(afterBalance - beforeBalance == unfreezeAmount);



  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    response = HttpMethed
        .unFreezeBalance(httpnode, fromAddress, frozenBalance,0, freezeBandwidthAddress,
            testKey002);
    HttpMethed.freedResource(httpnode, freezeBandwidthAddress, fromAddress, freezeBandwidthKey);
    HttpMethed.disConnect();
  }
}