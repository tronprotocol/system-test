package stest.tron.wallet.dailybuild.http;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.bouncycastle.math.ec.ScaleYNegateXPointMap;
import org.junit.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.*;


/**
 *
 */
@Slf4j
@Flaky(reason = "HTTP API timing-sensitive: freeze/unfreeze operations",
    since = "2026-04-03")
public class HttpTestFreezeV2001 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethod.getFinalAddress(testKey002);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] freezeBandwidthAddress = ecKey1.getAddress();
  String freezeBandwidthKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] receiverResourceAddress = ecKey2.getAddress();
  String receiverResourceKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());


  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] freezeEnergyAddress = ecKey3.getAddress();
  String freezeEnergyKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());

  ECKey ecKey4 = new ECKey(Utils.getRandom());
  byte[] freezeForQuery = ecKey4.getAddress();
  String freezeForQueryKey = ByteArray.toHexString(ecKey4.getPrivKeyBytes());
  
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
    if (!HttpMethod.proposalFreezeV2IsOpen(httpnode)) {
      throw new SkipException("Skipping this freezeV2 test case");
    }
    PublicMethod.printAddress(freezeBandwidthKey);
    PublicMethod.printAddress(receiverResourceKey);
    //Send trx to test account
    response = HttpMethod
        .sendCoin(httpnode, fromAddress, freezeBandwidthAddress, amount, testKey002);
    response = HttpMethod
        .sendCoin(httpnode, fromAddress, freezeEnergyAddress, amount, testKey002);
    response = HttpMethod
        .sendCoin(httpnode, fromAddress, receiverResourceAddress, 1L, testKey002);
    response = HttpMethod.sendCoin(httpnode, fromAddress, freezeForQuery, amount * 100, testKey002);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);


  }



  /**
   * constructor.
   */
  @Test(enabled = true, description = "FreezeBalanceV2 for bandwidth by http", groups = {"daily", "serial"})
  public void test001FreezeBalanceV2ForBandwidth() {
    beforeBalance = HttpMethod.getBalance(httpnode, freezeBandwidthAddress);
    //FreezeBalanceV2 balance
    response = HttpMethod
        .freezeBalanceV2(
            httpnode,
            freezeBandwidthAddress,
            frozenBalance,
            0,
            null,
            freezeBandwidthKey
        );
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethod.getBalance(httpnode, freezeBandwidthAddress);
    Assert.assertTrue(beforeBalance - afterBalance == frozenBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "UnFreezeBalanceV2 for bandwidth by http", groups = {"daily", "serial"})
  public void test002UnFreezeBalanceV2ForBandwidth() {
    beforeBalance = HttpMethod.getBalance(httpnode, freezeBandwidthAddress);
    //UnFreezeBalanceV2 for bandwidth
    response = HttpMethod
        .unFreezeBalanceV2(
            httpnode,
            freezeBandwidthAddress,
            unfrozenBalance,
            0,
            freezeBandwidthKey
        );

    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethod.getBalance(httpnode, freezeBandwidthAddress);
    Assert.assertEquals(afterBalance, beforeBalance);
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getAccount(httpnode, freezeBandwidthAddress)
    );
    JSONArray unfrozenV2 = responseContent.getJSONArray("unfrozenV2");
    Assert.assertTrue(unfrozenV2.size() == 1);

    Long unfreezeAmount = unfrozenV2.getJSONObject(0).getLong("unfreeze_amount");
    Long unfreezeExpireTime = unfrozenV2.getJSONObject(0).getLong("unfreeze_expire_time");
    logger.info("unfrozenBalance:" + unfrozenBalance);
    logger.info("unfreezeAmount:" + unfreezeAmount);
    logger.info("unfreezeExpireTime:" + unfreezeExpireTime);
    Assert.assertEquals(unfreezeAmount, unfrozenBalance);
    Assert.assertTrue(System.currentTimeMillis() < unfreezeExpireTime);
    Assert.assertTrue(!unfrozenV2.contains("type"));

    int retryTimes = 0;
    while (System.currentTimeMillis() < unfreezeExpireTime && retryTimes++ <= 100) {
      HttpMethod.waitToProduceOneBlock(httpnode);
    }

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "FreezeBalanceV2 for energy by http", groups = {"daily", "serial"})
  public void test003FreezeBalanceForEnergy() {
    beforeBalance = HttpMethod.getBalance(httpnode, freezeEnergyAddress);

    //Freeze balance for energy
    response = HttpMethod
        .freezeBalanceV2(
            httpnode,
            freezeEnergyAddress,
            frozenEnergyBalance,
            1,
            null,
            freezeEnergyKey
        );
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethod.getBalance(httpnode, freezeEnergyAddress);
    Assert.assertTrue(beforeBalance - afterBalance == frozenEnergyBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "UnFreezeBalanceV2 for energy by http", groups = {"daily", "serial"})
  public void test004UnFreezeBalanceForEnergy() {
    beforeBalance = HttpMethod.getBalance(httpnode, freezeEnergyAddress);
    HttpMethod.waitToProduceOneBlock(httpnode);
    //UnFreeze balance for energy
    response = HttpMethod.unFreezeBalanceV2(
        httpnode,
        freezeEnergyAddress,
        unfrozenEnergyBalance,
        1,
        freezeEnergyKey
    );

    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethod.getBalance(httpnode, freezeEnergyAddress);
    Assert.assertEquals(afterBalance, beforeBalance);

    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getAccount(httpnode, freezeEnergyAddress)
    );
    JSONArray unfrozenV2 = responseContent.getJSONArray("unfrozenV2");
    Assert.assertTrue(unfrozenV2.size() == 1);

    final Long unfreezeAmount = unfrozenV2.getJSONObject(0).getLong("unfreeze_amount");
    final Long unfreezeExpireTime = unfrozenV2.getJSONObject(0).getLong("unfreeze_expire_time");
    logger.info("unfrozenBalance:" + unfrozenBalance);
    logger.info("unfreezeAmount:" + unfreezeAmount);
    Assert.assertEquals(unfreezeAmount, unfrozenEnergyBalance);
    Assert.assertTrue(System.currentTimeMillis() < unfreezeExpireTime);
    Assert.assertEquals(unfrozenV2.getJSONObject(0).getString("type"), "ENERGY");


  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "DelegateResource of bandwidth for others by http", groups = {"daily", "serial"})
  public void test005DelegateResourceOfBandwidthForOthers() {
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getAccount(httpnode, freezeBandwidthAddress)
    );
    JSONArray frozenV2 = responseContent.getJSONArray("frozenV2");
    beforeFreezeBalance = frozenV2.getJSONObject(0).getLong("amount");

    //Freeze balance with bandwidth for others
    response = HttpMethod.delegateresource(
        httpnode,
        freezeBandwidthAddress,
        delegateAmount,
        0,
        null,
        null,
        receiverResourceAddress,
        freezeBandwidthKey
    );
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getAccount(httpnode, freezeBandwidthAddress)
    );
    frozenV2 = responseContent.getJSONArray("frozenV2");
    afterFreezeBalance =
        frozenV2.getJSONObject(0).getLong("amount");
    Assert.assertTrue(beforeFreezeBalance - afterFreezeBalance == delegateAmount);
    Long delegatedFrozenBalanceForBandwidth =
        responseContent.getLong("delegated_frozenV2_balance_for_bandwidth");
    Assert.assertEquals(delegatedFrozenBalanceForBandwidth, delegateAmount);
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getAccount(httpnode, receiverResourceAddress)
    );
    Long acquiredDelegatedFrozenBalanceForBandwidth =
        responseContent.getLong("acquired_delegated_frozenV2_balance_for_bandwidth");
    Assert.assertEquals(acquiredDelegatedFrozenBalanceForBandwidth, delegateAmount);


  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "UndelegateResource of bandwidth for others by http", groups = {"daily", "serial"})
  public void test006UndelegateResourceOfBandwidthForOthers() {
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getAccount(httpnode, freezeBandwidthAddress)
    );
    JSONArray frozenV2 = responseContent.getJSONArray("frozenV2");
    beforeFreezeBalance = frozenV2.getJSONObject(0).getLong("amount");

    //Freeze balance with bandwidth for others
    response = HttpMethod.unDelegateresource(
        httpnode,
        freezeBandwidthAddress,
        delegateAmount,
        0,
        receiverResourceAddress,
        freezeBandwidthKey
    );
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getAccount(httpnode, freezeBandwidthAddress)
    );
    frozenV2 = responseContent.getJSONArray("frozenV2");
    afterFreezeBalance = frozenV2.getJSONObject(0).getLong("amount");
    Assert.assertTrue(beforeFreezeBalance - afterFreezeBalance == -delegateAmount);
    Long delegatedFrozenBalanceForBandwidth =
        responseContent.getLong("delegated_frozenV2_balance_for_bandwidth");
    Assert.assertNull(delegatedFrozenBalanceForBandwidth);
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getAccount(httpnode, receiverResourceAddress)
    );
    Long acquiredDelegatedFrozenBalanceForBandwidth =
        responseContent.getLong("acquired_delegated_frozenV2_balance_for_bandwidth");
    Assert.assertNull(acquiredDelegatedFrozenBalanceForBandwidth);
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "DelegateResource of energy for others by http", groups = {"daily", "serial"})
  public void test007DelegateResourceOfEnergyForOthers() {
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getAccount(httpnode, freezeEnergyAddress)
    );
    JSONArray frozenV2 = responseContent.getJSONArray("frozenV2");
    beforeFreezeBalance = frozenV2.getJSONObject(1).getLong("amount");
    //Freeze balance with bandwidth for others
    response = HttpMethod.delegateresource(
        httpnode,
        freezeEnergyAddress,
        delegateAmount,
        1,
        null,
        null,
        receiverResourceAddress,
        freezeEnergyKey
    );
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getAccount(httpnode, freezeEnergyAddress)
    );
    frozenV2 = responseContent.getJSONArray("frozenV2");
    afterFreezeBalance = frozenV2.getJSONObject(1).getLong("amount");
    Assert.assertTrue(beforeFreezeBalance - afterFreezeBalance == delegateAmount);
    Long delegatedFrozenBalanceForBandwidth =
        responseContent.getJSONObject("account_resource")
            .getLong("delegated_frozenV2_balance_for_energy");
    Assert.assertEquals(delegatedFrozenBalanceForBandwidth, delegateAmount);
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getAccount(httpnode, receiverResourceAddress)
    );
    Long acquiredDelegatedFrozenBalanceForBandwidth =
        responseContent.getJSONObject("account_resource")
            .getLong("acquired_delegated_frozenV2_balance_for_energy");
    Assert.assertEquals(acquiredDelegatedFrozenBalanceForBandwidth, delegateAmount);


  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "UndelegateResource of energy for others by http", groups = {"daily", "serial"})
  public void test008UndelegateResourceOfEnergyForOthers() {
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getAccount(httpnode, freezeEnergyAddress)
    );
    JSONArray frozenV2 = responseContent.getJSONArray("frozenV2");
    beforeFreezeBalance = frozenV2.getJSONObject(1).getLong("amount");

    //Freeze balance with bandwidth for others
    response = HttpMethod.unDelegateresource(
        httpnode,
        freezeEnergyAddress,
        delegateAmount,
        1,
        receiverResourceAddress,
        freezeEnergyKey
    );
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getAccount(httpnode, freezeEnergyAddress)
    );
    frozenV2 = responseContent.getJSONArray("frozenV2");
    afterFreezeBalance = frozenV2.getJSONObject(1).getLong("amount");
    Assert.assertTrue(beforeFreezeBalance - afterFreezeBalance == -delegateAmount);
    Long delegatedFrozenBalanceForEnergy =
        responseContent.getJSONObject("account_resource")
            .getLong("delegated_frozenV2_balance_for_energy");
    Assert.assertNull(delegatedFrozenBalanceForEnergy);
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getAccount(httpnode, receiverResourceAddress)
    );
    Long acquiredDelegatedFrozenBalanceForEnergy =
        responseContent.getJSONObject("account_resource")
            .getLong("acquired_delegated_frozenV2_balance_for_energy");
    Assert.assertNull(acquiredDelegatedFrozenBalanceForEnergy);


  }



  /**
   * constructor.
   */
  @Test(enabled = true, description = "WithdrawExpireUnfreeze by http", groups = {"daily", "serial"})
  public void test009WithdrawExpireUnfreeze() {
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getAccount(httpnode, freezeBandwidthAddress)
    );
    JSONArray unfrozenV2 = responseContent.getJSONArray("unfrozenV2");
    Long unfreezeExpireTime = unfrozenV2.getJSONObject(0).getLong("unfreeze_expire_time");
    int retryTimes = 0;
    while (System.currentTimeMillis() < unfreezeExpireTime && retryTimes++ <= 100) {
      HttpMethod.waitToProduceOneBlock(httpnode);
    }





    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getAccount(httpnode, freezeBandwidthAddress)
    );
    unfrozenV2 = responseContent.getJSONArray("unfrozenV2");
    final Long unfreezeAmount = unfrozenV2.getJSONObject(0).getLong("unfreeze_amount");
    final Long beforeBalance = responseContent.getLong("balance");

    response = HttpMethod.withdrawExpireUnfreeze(
        httpnode,
        freezeBandwidthAddress,
        freezeBandwidthKey
    );
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);


    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getAccount(httpnode, freezeBandwidthAddress)
    );
    Assert.assertNull(responseContent.getJSONArray("unfrozenV2"));
    afterBalance = responseContent.getLong("balance");

    Assert.assertTrue(afterBalance - beforeBalance == unfreezeAmount);



  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetCanDelegateMaxSize by http", groups = {"daily", "serial"})
  public void test010GetCanDelegateMaxSize() {
    long frozenBalance = 100000000L;
    //without freeze, Assert null
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getCanDelegatedMaxSize(httpnode, freezeForQuery, 0L, false)
    );
    Assert.assertNull(responseContent.getJSONObject("max_size"));

    //freeze bandwidth
    response = HttpMethod.freezeBalanceV2(
        httpnode,
        freezeForQuery,
        frozenBalance,
        0,
        null,
        freezeForQueryKey
    );
    Assert.assertTrue(HttpMethod.verificationResult(response));

    //freeze energy
    response = HttpMethod.freezeBalanceV2(
        httpnode,
        freezeForQuery,
        frozenBalance,
        1,
        null,
        freezeForQueryKey
    );
    Assert.assertTrue(HttpMethod.verificationResult(response));

    HttpMethod.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);

    //query fullNode
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getCanDelegatedMaxSize(httpnode, freezeForQuery, 0L, false)
    );
    logger.info("bandwidth max_size:" + responseContent.getInteger("max_size").toString());
    Assert.assertTrue(responseContent.getInteger("max_size") <= frozenBalance);
    //query solidity node
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getCanDelegatedMaxSizeSolidity(httpSoliditynode, freezeForQuery, 0L, false)
    );
    logger.info("bandwidth max_size:" + responseContent.getInteger("max_size").toString());
    Assert.assertTrue(responseContent.getInteger("max_size") <= frozenBalance);
    //query pbft node
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getCanDelegatedMaxSizePbft(httpPbftNode, freezeForQuery, 0L, false)
    );
    logger.info("bandwidth max_size:" + responseContent.getInteger("max_size").toString());
    Assert.assertTrue(responseContent.getInteger("max_size") <= frozenBalance);


    //query fullNode
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getCanDelegatedMaxSize(httpnode, freezeForQuery, 1L, false)
    );
    logger.info("energy max_size:" + responseContent.getInteger("max_size").toString());
    Assert.assertTrue(responseContent.getInteger("max_size") == frozenBalance);
    //query solidity node
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getCanDelegatedMaxSizeSolidity(httpSoliditynode, freezeForQuery, 1L, false)
    );
    logger.info("energy max_size:" + responseContent.getInteger("max_size").toString());
    Assert.assertTrue(responseContent.getInteger("max_size") == frozenBalance);
    //query pbft node
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getCanDelegatedMaxSizePbft(httpPbftNode, freezeForQuery, 1L, false)
    );
    logger.info("energy max_size:" + responseContent.getInteger("max_size").toString());
    Assert.assertTrue(responseContent.getInteger("max_size") == frozenBalance);


  }



  @Test(enabled = true, description = "getCanWithdrawUnfreezeAmount by http", groups = {"daily", "serial"})
  public void test012GetCanWithdrawUnfreezeAmount() {
    final long fronzenBalance = 9000000L;
    final long unfronzenBalance = fronzenBalance;

    HttpMethod.freezeBalanceV2(
        httpnode,
        freezeForQuery,
        fronzenBalance,
        0,
        null,
        freezeForQueryKey
    );
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getCanWithdrawUnfreezeAmount(
            httpnode,
            freezeForQuery,
            System.currentTimeMillis(),
            false
        )
    );
    Assert.assertNull(responseContent.getJSONObject("amount"));
    HttpMethod.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    response = HttpMethod.unFreezeBalanceV2(
        httpnode,
        freezeForQuery,
        unfronzenBalance,
        0,
        freezeForQueryKey
    );
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);

    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getAccount(httpnode, freezeForQuery)
    );
    JSONArray unfreezeList = responseContent.getJSONArray("unfrozenV2");
    long unfreezeExpireTime = 0L;
    for (int i = 0; i < unfreezeList.size(); i++) {
      JSONObject item = unfreezeList.getJSONObject(i);
      if (item.getLongValue("unfreeze_amount") == unfronzenBalance) {
        unfreezeExpireTime = item.getLongValue("unfreeze_expire_time");
      }
    }
    //query  expire time
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getCanWithdrawUnfreezeAmount(httpnode, freezeForQuery, unfreezeExpireTime, false)
    );
    Assert.assertEquals(unfronzenBalance, responseContent.getLongValue("amount"));
    //query solidity
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getCanWithdrawUnfreezeAmountSolidity(
            httpSoliditynode,
            freezeForQuery,
            unfreezeExpireTime,
            false
        )
    );
    Assert.assertEquals(unfronzenBalance, responseContent.getLongValue("amount"));
    //query pbft node
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getCanWithdrawUnfreezeAmountPbft(
            httpPbftNode,
            freezeForQuery,
            unfreezeExpireTime,
            false
        )
    );
    Assert.assertEquals(unfronzenBalance, responseContent.getLongValue("amount"));

    //query  expire time-1
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getCanWithdrawUnfreezeAmount(
            httpnode,
            freezeForQuery,
            unfreezeExpireTime - 1L,
            false
        )
    );
    Assert.assertNull(responseContent.getJSONObject("amount"));
    //query solidity expire time-1
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getCanWithdrawUnfreezeAmountSolidity(
            httpSoliditynode,
            freezeForQuery,
            unfreezeExpireTime - 1L,
            false)
    );
    Assert.assertNull(responseContent.getJSONObject("amount"));

    //query expire time+1
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getCanWithdrawUnfreezeAmount(
            httpnode,
            freezeForQuery,
            unfreezeExpireTime + 1L,
            true
        )
    );
    Assert.assertEquals(unfronzenBalance, responseContent.getLongValue("amount"));
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getCanWithdrawUnfreezeAmountSolidity(
            httpSoliditynode,
            freezeForQuery,
            unfreezeExpireTime + 1L,
            true
        )
    );
    Assert.assertEquals(unfronzenBalance, responseContent.getLongValue("amount"));
  }

  @Test(enabled = true, description = "GetDelegatedResourceV2 by http", groups = {"daily", "serial"})
  public void test013GetDelegatedResourceV2() {
    final long freezeAmount = 100000000L;
    final long delegateAmount = 50000000L;
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getDelegatedResourceV2(
            httpnode,
            freezeForQuery,
            receiverResourceAddress,
            true
        )
    );
    Assert.assertNull(responseContent.getJSONArray("delegatedResource"));
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getDelegatedResourceV2Solidity(
            httpSoliditynode,
            freezeForQuery,
            receiverResourceAddress,
            true
        )
    );
    Assert.assertNull(responseContent.getJSONArray("delegatedResource"));

    response = HttpMethod.freezeBalanceV2(
        httpnode,
        freezeForQuery,
        freezeAmount,
        0,
        null,
        freezeForQueryKey
    );
    Assert.assertTrue(HttpMethod.verificationResult(response));
    response = HttpMethod.freezeBalanceV2(
        httpnode,
        freezeForQuery,
        freezeAmount,
        1,
        null,
        freezeForQueryKey
    );
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    response = HttpMethod.delegateresource(
        httpnode,
        freezeForQuery,
        delegateAmount,
        0,
        null,
        null,
        receiverResourceAddress,
        freezeForQueryKey
    );
    Assert.assertTrue(HttpMethod.verificationResult(response));
    response = HttpMethod.delegateresource(
        httpnode,
        freezeForQuery,
        delegateAmount,
        1,
        null,
        null,
        receiverResourceAddress,
        freezeForQueryKey
    );
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);

    //query fullNode
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getDelegatedResourceV2(
            httpnode,
            freezeForQuery,
            receiverResourceAddress,
            true)
    );
    JSONArray delegateList = responseContent.getJSONArray("delegatedResource");
    JSONObject delegateItem = null;
    for (int i = 0; i < delegateList.size(); i++) {

      if (delegateList.getJSONObject(i).getString("from")
          .equals(Base58.encode58Check(freezeForQuery))
          && delegateList.getJSONObject(i).getString("to")
          .equals(Base58.encode58Check(receiverResourceAddress))) {
        delegateItem = delegateList.getJSONObject(i);
      }
    }
    Assert.assertNotNull(delegateItem);
    Assert.assertEquals(delegateAmount, delegateItem.getLongValue("frozen_balance_for_bandwidth"));
    Assert.assertEquals(delegateAmount, delegateItem.getLongValue("frozen_balance_for_energy"));

    //query solidity
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getDelegatedResourceV2Solidity(
            httpSoliditynode,
            freezeForQuery,
            receiverResourceAddress,
            true
        )
    );
    delegateList = responseContent.getJSONArray("delegatedResource");
    delegateItem = null;
    for (int i = 0; i < delegateList.size(); i++) {
      if (delegateList.getJSONObject(i).getString("from")
          .equals(Base58.encode58Check(freezeForQuery))
          && delegateList.getJSONObject(i).getString("to")
          .equals(Base58.encode58Check(receiverResourceAddress))) {
        delegateItem = delegateList.getJSONObject(i);
      }
    }
    Assert.assertNotNull(delegateItem);
    Assert.assertEquals(delegateAmount, delegateItem.getLongValue("frozen_balance_for_bandwidth"));
    Assert.assertEquals(delegateAmount, delegateItem.getLongValue("frozen_balance_for_energy"));

    //query pbft
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getDelegatedResourceV2Pbft(
            httpPbftNode,
            freezeForQuery,
            receiverResourceAddress,
            true
        )
    );
    delegateList = responseContent.getJSONArray("delegatedResource");
    delegateItem = null;
    for (int i = 0; i < delegateList.size(); i++) {
      if (delegateList.getJSONObject(i).getString("from")
          .equals(Base58.encode58Check(freezeForQuery))
          && delegateList.getJSONObject(i).getString("to")
          .equals(Base58.encode58Check(receiverResourceAddress))) {
        delegateItem = delegateList.getJSONObject(i);
      }
    }
    Assert.assertNotNull(delegateItem);
    Assert.assertEquals(delegateAmount, delegateItem.getLongValue("frozen_balance_for_bandwidth"));
    Assert.assertEquals(delegateAmount, delegateItem.getLongValue("frozen_balance_for_energy"));


  }

  @Test(enabled = true, description = "GetDelegatedResourceAccountIndexV2 by http", groups = {"daily", "serial"})
  public void test014GetDelegatedResourceAccountIndexV2() {
    final long freezeAmount = 100000000L;
    final long delegateAmount = 50000000L;
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getDelegatedResourceAccountIndexV2(
            httpnode,
            freezeForQuery,
            true
        )
    );
    logger.info(responseContent.toJSONString());
    response = HttpMethod.freezeBalanceV2(
        httpnode,
        freezeForQuery,
        freezeAmount,
        0,
        null,
        freezeForQueryKey
    );
    Assert.assertTrue(HttpMethod.verificationResult(response));
    response = HttpMethod.freezeBalanceV2(
        httpnode,
        freezeForQuery,
        freezeAmount,
        1,
        null,
        freezeForQueryKey
    );
    Assert.assertTrue(HttpMethod.verificationResult(response));

    HttpMethod.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    response = HttpMethod.delegateresource(
        httpnode,
        freezeForQuery,
        delegateAmount,
        0,
        null,
        null,
        receiverResourceAddress,
        freezeForQueryKey
    );
    Assert.assertTrue(HttpMethod.verificationResult(response));
    response = HttpMethod.delegateresource(
        httpnode,
        freezeForQuery,
        delegateAmount,
        1,
        null,
        null,
        receiverResourceAddress,
        freezeForQueryKey
    );
    Assert.assertTrue(HttpMethod.verificationResult(response));

    HttpMethod.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    //query fullNode
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getDelegatedResourceAccountIndexV2(httpnode, freezeForQuery, true)
    );
    Assert.assertTrue(
        responseContent.getString("account").equals(Base58.encode58Check(freezeForQuery))
    );
    Assert.assertTrue(
        responseContent.getJSONArray("toAccounts")
            .contains(Base58.encode58Check(receiverResourceAddress))
    );
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getDelegatedResourceAccountIndexV2(httpnode, receiverResourceAddress, true)
    );
    Assert.assertTrue(
        responseContent.getString("account").equals(Base58.encode58Check(receiverResourceAddress))
    );
    Assert.assertTrue(
        responseContent.getJSONArray("fromAccounts").contains(Base58.encode58Check(freezeForQuery))
    );
    //query solidity
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getDelegatedResourceAccountIndexV2Solidity(
            httpSoliditynode,
            freezeForQuery,
            true
        )
    );
    Assert.assertTrue(
        responseContent.getString("account").equals(Base58.encode58Check(freezeForQuery))
    );
    Assert.assertTrue(
        responseContent.getJSONArray("toAccounts")
            .contains(Base58.encode58Check(receiverResourceAddress))
    );
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getDelegatedResourceAccountIndexV2Solidity(
            httpSoliditynode,
            receiverResourceAddress,
            true
        )
    );
    Assert.assertTrue(
        responseContent.getString("account").equals(Base58.encode58Check(receiverResourceAddress))
    );
    Assert.assertTrue(
        responseContent.getJSONArray("fromAccounts").contains(Base58.encode58Check(freezeForQuery))
    );
    //query pbft
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getDelegatedResourceAccountIndexV2Pbft(
            httpPbftNode,
            freezeForQuery,
            true
        )
    );
    Assert.assertTrue(
        responseContent.getString("account").equals(Base58.encode58Check(freezeForQuery))
    );
    Assert.assertTrue(
        responseContent.getJSONArray("toAccounts")
            .contains(Base58.encode58Check(receiverResourceAddress))
    );
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getDelegatedResourceAccountIndexV2Pbft(
            httpPbftNode,
            receiverResourceAddress,
            true
        )
    );
    Assert.assertTrue(
        responseContent.getString("account").equals(Base58.encode58Check(receiverResourceAddress))
    );
    Assert.assertTrue(
        responseContent.getJSONArray("fromAccounts").contains(Base58.encode58Check(freezeForQuery))
    );


  }

  @Test(enabled = true, description = "GetAvailableUnfreezeCount by http", groups = {"daily", "serial"})
  public void test011GetAvailableUnfreezeCount() {
    //use another account for case independence
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] unfreezeAccount = ecKey.getAddress();
    final String unfreezeAccountKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    response = HttpMethod.sendCoin(httpnode, fromAddress, unfreezeAccount, amount * 2, testKey002);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    long freezeAmount = 1000000L;
    //without unfreeze, Assert 32
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getAvailableUnfreezeCount(httpnode, unfreezeAccount, true)
    );
    Assert.assertEquals(32L, responseContent.getLongValue("count"));

    response = HttpMethod.freezeBalanceV2(
        httpnode,
        unfreezeAccount,
        freezeAmount,
        0,
        null,
        unfreezeAccountKey
    );
    HttpMethod.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);

    // unfreezeBalance 1 SUN bandwidth Assert 31
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.unFreezeBalanceV2(httpnode, unfreezeAccount, 1L, 0, unfreezeAccountKey)
    );
    HttpMethod.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    //query fullNode
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getAvailableUnfreezeCount(httpnode, unfreezeAccount, true)
    );
    Assert.assertEquals(31L, responseContent.getLongValue("count"));
    //query solidity
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getAvailableUnfreezeCountSolidity(httpSoliditynode, unfreezeAccount, true)
    );
    Assert.assertEquals(31L, responseContent.getLongValue("count"));
    //query pbft
    responseContent = HttpMethod.parseResponseContent(
        HttpMethod.getAvailableUnfreezeCountPbft(httpPbftNode, unfreezeAccount, true)
    );
    Assert.assertEquals(31L, responseContent.getLongValue("count"));

  }

  @Test(enabled = true, description = "Test lockPeriod = 1000L", groups = {"daily", "serial"})
  public void test015lockPeriodTest() {
    final long lockPeriod = 1000L;
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] fromDelegateAddress = ecKey.getAddress();
    String fromDelegateKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    HttpMethod.sendCoin(httpnode, fromAddress, fromDelegateAddress, amount, testKey002);
    HttpMethod.waitToProduceOneBlock(httpnode);
    HttpMethod.freezeBalanceV2(httpnode, fromDelegateAddress, amount, 1, null, fromDelegateKey);
    HttpMethod.waitToProduceOneBlock(httpnode);
    final long delegateTime = System.currentTimeMillis();
    response = HttpMethod.delegateresource(httpnode, fromDelegateAddress, delegateAmount,
        1, true, lockPeriod, receiverResourceAddress, fromDelegateKey);
    responseContent = HttpMethod.parseResponseContent(response);
    logger.info("delegateResource:" + responseContent.toJSONString());
    Assert.assertEquals(responseContent.getBoolean("result"), true);
    HttpMethod.waitToProduceOneBlock(httpnode);
    response = HttpMethod.getDelegatedResourceV2(
        httpnode, fromDelegateAddress, receiverResourceAddress, true);
    responseContent = HttpMethod.parseResponseContent(response);
    logger.info(responseContent.toJSONString());
    Long expireTime =
        responseContent
            .getJSONArray("delegatedResource")
            .getJSONObject(0)
            .getLong("expire_time_for_energy");
    Assert.assertTrue(Math.abs((expireTime - delegateTime) - (lockPeriod * 3 * 1000)) < 5000L);
    Assert.assertEquals(
        responseContent
            .getJSONArray("delegatedResource")
            .getJSONObject(0).getLong("frozen_balance_for_energy")
            .longValue(),
        delegateAmount.longValue());
    Assert.assertEquals(
        responseContent
            .getJSONArray("delegatedResource")
            .getJSONObject(0).getString("from"),
        Base58.encode58Check(fromDelegateAddress));
    Assert.assertEquals(
        responseContent
            .getJSONArray("delegatedResource")
            .getJSONObject(0).getString("to"),
        Base58.encode58Check(receiverResourceAddress));
  }

  /**
   * constructor.
   * */
  @AfterClass
  public void shutdown() throws InterruptedException {
    response = HttpMethod
        .unFreezeBalance(httpnode, fromAddress, frozenBalance, 0, freezeBandwidthAddress,
            testKey002);
    HttpMethod.freeResource(httpnode, freezeBandwidthAddress, fromAddress, freezeBandwidthKey);
    HttpMethod.disConnect();
  }
}