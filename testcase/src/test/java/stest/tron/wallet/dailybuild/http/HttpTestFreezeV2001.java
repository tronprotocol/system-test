package stest.tron.wallet.dailybuild.http;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.bouncycastle.math.ec.ScaleYNegateXPointMap;
import org.junit.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.*;


/**
 *
 */
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
    if (!HttpMethed.proposalFreezeV2IsOpen(httpnode)) {
      throw new SkipException("Skipping this freezeV2 test case");
    }
    PublicMethed.printAddress(freezeBandwidthKey);
    PublicMethed.printAddress(receiverResourceKey);
    //Send trx to test account
    response = HttpMethed
        .sendCoin(httpnode, fromAddress, freezeBandwidthAddress, amount, testKey002);
    response = HttpMethed
        .sendCoin(httpnode, fromAddress, freezeEnergyAddress, amount, testKey002);
    response = HttpMethed
        .sendCoin(httpnode, fromAddress, receiverResourceAddress, 1L, testKey002);
    response = HttpMethed.sendCoin(httpnode, fromAddress, freezeForQuery, amount * 100, testKey002);
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
        .freezeBalanceV2(
            httpnode,
            freezeBandwidthAddress,
            frozenBalance,
            0,
            null,
            freezeBandwidthKey
        );
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
    response = HttpMethed
        .unFreezeBalanceV2(
            httpnode,
            freezeBandwidthAddress,
            unfrozenBalance,
            0,
            freezeBandwidthKey
        );

    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethed.getBalance(httpnode, freezeBandwidthAddress);
    Assert.assertEquals(afterBalance, beforeBalance);
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getAccount(httpnode, freezeBandwidthAddress)
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
        .freezeBalanceV2(
            httpnode,
            freezeEnergyAddress,
            frozenEnergyBalance,
            1,
            null,
            freezeEnergyKey
        );
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
    response = HttpMethed.unFreezeBalanceV2(
        httpnode,
        freezeEnergyAddress,
        unfrozenEnergyBalance,
        1,
        freezeEnergyKey
    );

    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethed.getBalance(httpnode, freezeEnergyAddress);
    Assert.assertEquals(afterBalance, beforeBalance);

    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getAccount(httpnode, freezeEnergyAddress)
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
  @Test(enabled = true, description = "DelegateResource of bandwidth for others by http")
  public void test005DelegateResourceOfBandwidthForOthers() {
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getAccount(httpnode, freezeBandwidthAddress)
    );
    JSONArray frozenV2 = responseContent.getJSONArray("frozenV2");
    beforeFreezeBalance = frozenV2.getJSONObject(0).getLong("amount");

    //Freeze balance with bandwidth for others
    response = HttpMethed.delegateresource(
        httpnode,
        freezeBandwidthAddress,
        delegateAmount,
        0,
        null,
        null,
        receiverResourceAddress,
        freezeBandwidthKey
    );
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getAccount(httpnode, freezeBandwidthAddress)
    );
    frozenV2 = responseContent.getJSONArray("frozenV2");
    afterFreezeBalance =
        frozenV2.getJSONObject(0).getLong("amount");
    Assert.assertTrue(beforeFreezeBalance - afterFreezeBalance == delegateAmount);
    Long delegatedFrozenBalanceForBandwidth =
        responseContent.getLong("delegated_frozenV2_balance_for_bandwidth");
    Assert.assertEquals(delegatedFrozenBalanceForBandwidth, delegateAmount);
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getAccount(httpnode, receiverResourceAddress)
    );
    Long acquiredDelegatedFrozenBalanceForBandwidth =
        responseContent.getLong("acquired_delegated_frozenV2_balance_for_bandwidth");
    Assert.assertEquals(acquiredDelegatedFrozenBalanceForBandwidth, delegateAmount);


  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "UndelegateResource of bandwidth for others by http")
  public void test006UndelegateResourceOfBandwidthForOthers() {
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getAccount(httpnode, freezeBandwidthAddress)
    );
    JSONArray frozenV2 = responseContent.getJSONArray("frozenV2");
    beforeFreezeBalance = frozenV2.getJSONObject(0).getLong("amount");

    //Freeze balance with bandwidth for others
    response = HttpMethed.unDelegateresource(
        httpnode,
        freezeBandwidthAddress,
        delegateAmount,
        0,
        receiverResourceAddress,
        freezeBandwidthKey
    );
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getAccount(httpnode, freezeBandwidthAddress)
    );
    frozenV2 = responseContent.getJSONArray("frozenV2");
    afterFreezeBalance = frozenV2.getJSONObject(0).getLong("amount");
    Assert.assertTrue(beforeFreezeBalance - afterFreezeBalance == -delegateAmount);
    Long delegatedFrozenBalanceForBandwidth =
        responseContent.getLong("delegated_frozenV2_balance_for_bandwidth");
    Assert.assertNull(delegatedFrozenBalanceForBandwidth);
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getAccount(httpnode, receiverResourceAddress)
    );
    Long acquiredDelegatedFrozenBalanceForBandwidth =
        responseContent.getLong("acquired_delegated_frozenV2_balance_for_bandwidth");
    Assert.assertNull(acquiredDelegatedFrozenBalanceForBandwidth);
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "DelegateResource of energy for others by http")
  public void test007DelegateResourceOfEnergyForOthers() {
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getAccount(httpnode, freezeEnergyAddress)
    );
    JSONArray frozenV2 = responseContent.getJSONArray("frozenV2");
    beforeFreezeBalance = frozenV2.getJSONObject(1).getLong("amount");
    //Freeze balance with bandwidth for others
    response = HttpMethed.delegateresource(
        httpnode,
        freezeEnergyAddress,
        delegateAmount,
        1,
        null,
        null,
        receiverResourceAddress,
        freezeEnergyKey
    );
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getAccount(httpnode, freezeEnergyAddress)
    );
    frozenV2 = responseContent.getJSONArray("frozenV2");
    afterFreezeBalance = frozenV2.getJSONObject(1).getLong("amount");
    Assert.assertTrue(beforeFreezeBalance - afterFreezeBalance == delegateAmount);
    Long delegatedFrozenBalanceForBandwidth =
        responseContent.getJSONObject("account_resource")
            .getLong("delegated_frozenV2_balance_for_energy");
    Assert.assertEquals(delegatedFrozenBalanceForBandwidth, delegateAmount);
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getAccount(httpnode, receiverResourceAddress)
    );
    Long acquiredDelegatedFrozenBalanceForBandwidth =
        responseContent.getJSONObject("account_resource")
            .getLong("acquired_delegated_frozenV2_balance_for_energy");
    Assert.assertEquals(acquiredDelegatedFrozenBalanceForBandwidth, delegateAmount);


  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "UndelegateResource of energy for others by http")
  public void test008UndelegateResourceOfEnergyForOthers() {
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getAccount(httpnode, freezeEnergyAddress)
    );
    JSONArray frozenV2 = responseContent.getJSONArray("frozenV2");
    beforeFreezeBalance = frozenV2.getJSONObject(1).getLong("amount");

    //Freeze balance with bandwidth for others
    response = HttpMethed.unDelegateresource(
        httpnode,
        freezeEnergyAddress,
        delegateAmount,
        1,
        receiverResourceAddress,
        freezeEnergyKey
    );
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getAccount(httpnode, freezeEnergyAddress)
    );
    frozenV2 = responseContent.getJSONArray("frozenV2");
    afterFreezeBalance = frozenV2.getJSONObject(1).getLong("amount");
    Assert.assertTrue(beforeFreezeBalance - afterFreezeBalance == -delegateAmount);
    Long delegatedFrozenBalanceForEnergy =
        responseContent.getJSONObject("account_resource")
            .getLong("delegated_frozenV2_balance_for_energy");
    Assert.assertNull(delegatedFrozenBalanceForEnergy);
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getAccount(httpnode, receiverResourceAddress)
    );
    Long acquiredDelegatedFrozenBalanceForEnergy =
        responseContent.getJSONObject("account_resource")
            .getLong("acquired_delegated_frozenV2_balance_for_energy");
    Assert.assertNull(acquiredDelegatedFrozenBalanceForEnergy);


  }



  /**
   * constructor.
   */
  @Test(enabled = true, description = "WithdrawExpireUnfreeze by http")
  public void test009WithdrawExpireUnfreeze() {
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getAccount(httpnode, freezeBandwidthAddress)
    );
    JSONArray unfrozenV2 = responseContent.getJSONArray("unfrozenV2");
    Long unfreezeExpireTime = unfrozenV2.getJSONObject(0).getLong("unfreeze_expire_time");
    int retryTimes = 0;
    while (System.currentTimeMillis() < unfreezeExpireTime && retryTimes++ <= 100) {
      HttpMethed.waitToProduceOneBlock(httpnode);
    }





    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getAccount(httpnode, freezeBandwidthAddress)
    );
    unfrozenV2 = responseContent.getJSONArray("unfrozenV2");
    final Long unfreezeAmount = unfrozenV2.getJSONObject(0).getLong("unfreeze_amount");
    final Long beforeBalance = responseContent.getLong("balance");

    response = HttpMethed.withdrawExpireUnfreeze(
        httpnode,
        freezeBandwidthAddress,
        freezeBandwidthKey
    );
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);


    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getAccount(httpnode, freezeBandwidthAddress)
    );
    Assert.assertNull(responseContent.getJSONArray("unfrozenV2"));
    afterBalance = responseContent.getLong("balance");

    Assert.assertTrue(afterBalance - beforeBalance == unfreezeAmount);



  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetCanDelegateMaxSize by http")
  public void test010GetCanDelegateMaxSize() {
    long frozenBalance = 100000000L;
    //without freeze, Assert null
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getCanDelegatedMaxSize(httpnode, freezeForQuery, 0L, false)
    );
    Assert.assertNull(responseContent.getJSONObject("max_size"));

    //freeze bandwidth
    response = HttpMethed.freezeBalanceV2(
        httpnode,
        freezeForQuery,
        frozenBalance,
        0,
        null,
        freezeForQueryKey
    );
    Assert.assertTrue(HttpMethed.verificationResult(response));

    //freeze energy
    response = HttpMethed.freezeBalanceV2(
        httpnode,
        freezeForQuery,
        frozenBalance,
        1,
        null,
        freezeForQueryKey
    );
    Assert.assertTrue(HttpMethed.verificationResult(response));

    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);

    //query fullNode
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getCanDelegatedMaxSize(httpnode, freezeForQuery, 0L, false)
    );
    logger.info("bandwidth max_size:" + responseContent.getInteger("max_size").toString());
    Assert.assertTrue(responseContent.getInteger("max_size") <= frozenBalance);
    //query solidity node
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getCanDelegatedMaxSizeSolidity(httpSoliditynode, freezeForQuery, 0L, false)
    );
    logger.info("bandwidth max_size:" + responseContent.getInteger("max_size").toString());
    Assert.assertTrue(responseContent.getInteger("max_size") <= frozenBalance);
    //query pbft node
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getCanDelegatedMaxSizePbft(httpPbftNode, freezeForQuery, 0L, false)
    );
    logger.info("bandwidth max_size:" + responseContent.getInteger("max_size").toString());
    Assert.assertTrue(responseContent.getInteger("max_size") <= frozenBalance);


    //query fullNode
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getCanDelegatedMaxSize(httpnode, freezeForQuery, 1L, false)
    );
    logger.info("energy max_size:" + responseContent.getInteger("max_size").toString());
    Assert.assertTrue(responseContent.getInteger("max_size") == frozenBalance);
    //query solidity node
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getCanDelegatedMaxSizeSolidity(httpSoliditynode, freezeForQuery, 1L, false)
    );
    logger.info("energy max_size:" + responseContent.getInteger("max_size").toString());
    Assert.assertTrue(responseContent.getInteger("max_size") == frozenBalance);
    //query pbft node
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getCanDelegatedMaxSizePbft(httpPbftNode, freezeForQuery, 1L, false)
    );
    logger.info("energy max_size:" + responseContent.getInteger("max_size").toString());
    Assert.assertTrue(responseContent.getInteger("max_size") == frozenBalance);


  }



  @Test(enabled = true, description = "getCanWithdrawUnfreezeAmount by http")
  public void test012GetCanWithdrawUnfreezeAmount() {
    final long fronzenBalance = 9000000L;
    final long unfronzenBalance = fronzenBalance;

    HttpMethed.freezeBalanceV2(
        httpnode,
        freezeForQuery,
        fronzenBalance,
        0,
        null,
        freezeForQueryKey
    );
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getCanWithdrawUnfreezeAmount(
            httpnode,
            freezeForQuery,
            System.currentTimeMillis(),
            false
        )
    );
    Assert.assertNull(responseContent.getJSONObject("amount"));
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    response = HttpMethed.unFreezeBalanceV2(
        httpnode,
        freezeForQuery,
        unfronzenBalance,
        0,
        freezeForQueryKey
    );
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);

    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getAccount(httpnode, freezeForQuery)
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
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getCanWithdrawUnfreezeAmount(httpnode, freezeForQuery, unfreezeExpireTime, false)
    );
    Assert.assertEquals(unfronzenBalance, responseContent.getLongValue("amount"));
    //query solidity
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getCanWithdrawUnfreezeAmountSolidity(
            httpSoliditynode,
            freezeForQuery,
            unfreezeExpireTime,
            false
        )
    );
    Assert.assertEquals(unfronzenBalance, responseContent.getLongValue("amount"));
    //query pbft node
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getCanWithdrawUnfreezeAmountPbft(
            httpPbftNode,
            freezeForQuery,
            unfreezeExpireTime,
            false
        )
    );
    Assert.assertEquals(unfronzenBalance, responseContent.getLongValue("amount"));

    //query  expire time-1
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getCanWithdrawUnfreezeAmount(
            httpnode,
            freezeForQuery,
            unfreezeExpireTime - 1L,
            false
        )
    );
    Assert.assertNull(responseContent.getJSONObject("amount"));
    //query solidity expire time-1
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getCanWithdrawUnfreezeAmountSolidity(
            httpSoliditynode,
            freezeForQuery,
            unfreezeExpireTime - 1L,
            false)
    );
    Assert.assertNull(responseContent.getJSONObject("amount"));

    //query expire time+1
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getCanWithdrawUnfreezeAmount(
            httpnode,
            freezeForQuery,
            unfreezeExpireTime + 1L,
            true
        )
    );
    Assert.assertEquals(unfronzenBalance, responseContent.getLongValue("amount"));
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getCanWithdrawUnfreezeAmountSolidity(
            httpSoliditynode,
            freezeForQuery,
            unfreezeExpireTime + 1L,
            true
        )
    );
    Assert.assertEquals(unfronzenBalance, responseContent.getLongValue("amount"));
  }

  @Test(enabled = true, description = "GetDelegatedResourceV2 by http")
  public void test013GetDelegatedResourceV2() {
    final long freezeAmount = 100000000L;
    final long delegateAmount = 50000000L;
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getDelegatedResourceV2(
            httpnode,
            freezeForQuery,
            receiverResourceAddress,
            true
        )
    );
    Assert.assertNull(responseContent.getJSONArray("delegatedResource"));
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getDelegatedResourceV2Solidity(
            httpSoliditynode,
            freezeForQuery,
            receiverResourceAddress,
            true
        )
    );
    Assert.assertNull(responseContent.getJSONArray("delegatedResource"));

    response = HttpMethed.freezeBalanceV2(
        httpnode,
        freezeForQuery,
        freezeAmount,
        0,
        null,
        freezeForQueryKey
    );
    Assert.assertTrue(HttpMethed.verificationResult(response));
    response = HttpMethed.freezeBalanceV2(
        httpnode,
        freezeForQuery,
        freezeAmount,
        1,
        null,
        freezeForQueryKey
    );
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    response = HttpMethed.delegateresource(
        httpnode,
        freezeForQuery,
        delegateAmount,
        0,
        null,
        null,
        receiverResourceAddress,
        freezeForQueryKey
    );
    Assert.assertTrue(HttpMethed.verificationResult(response));
    response = HttpMethed.delegateresource(
        httpnode,
        freezeForQuery,
        delegateAmount,
        1,
        null,
        null,
        receiverResourceAddress,
        freezeForQueryKey
    );
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);

    //query fullNode
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getDelegatedResourceV2(
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
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getDelegatedResourceV2Solidity(
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
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getDelegatedResourceV2Pbft(
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

  @Test(enabled = true, description = "GetDelegatedResourceAccountIndexV2 by http")
  public void test014GetDelegatedResourceAccountIndexV2() {
    final long freezeAmount = 100000000L;
    final long delegateAmount = 50000000L;
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getDelegatedResourceAccountIndexV2(
            httpnode,
            freezeForQuery,
            true
        )
    );
    logger.info(responseContent.toJSONString());
    response = HttpMethed.freezeBalanceV2(
        httpnode,
        freezeForQuery,
        freezeAmount,
        0,
        null,
        freezeForQueryKey
    );
    Assert.assertTrue(HttpMethed.verificationResult(response));
    response = HttpMethed.freezeBalanceV2(
        httpnode,
        freezeForQuery,
        freezeAmount,
        1,
        null,
        freezeForQueryKey
    );
    Assert.assertTrue(HttpMethed.verificationResult(response));

    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    response = HttpMethed.delegateresource(
        httpnode,
        freezeForQuery,
        delegateAmount,
        0,
        null,
        null,
        receiverResourceAddress,
        freezeForQueryKey
    );
    Assert.assertTrue(HttpMethed.verificationResult(response));
    response = HttpMethed.delegateresource(
        httpnode,
        freezeForQuery,
        delegateAmount,
        1,
        null,
        null,
        receiverResourceAddress,
        freezeForQueryKey
    );
    Assert.assertTrue(HttpMethed.verificationResult(response));

    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    //query fullNode
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getDelegatedResourceAccountIndexV2(httpnode, freezeForQuery, true)
    );
    Assert.assertTrue(
        responseContent.getString("account").equals(Base58.encode58Check(freezeForQuery))
    );
    Assert.assertTrue(
        responseContent.getJSONArray("toAccounts")
            .contains(Base58.encode58Check(receiverResourceAddress))
    );
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getDelegatedResourceAccountIndexV2(httpnode, receiverResourceAddress, true)
    );
    Assert.assertTrue(
        responseContent.getString("account").equals(Base58.encode58Check(receiverResourceAddress))
    );
    Assert.assertTrue(
        responseContent.getJSONArray("fromAccounts").contains(Base58.encode58Check(freezeForQuery))
    );
    //query solidity
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getDelegatedResourceAccountIndexV2Solidity(
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
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getDelegatedResourceAccountIndexV2Solidity(
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
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getDelegatedResourceAccountIndexV2Pbft(
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
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getDelegatedResourceAccountIndexV2Pbft(
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

  @Test(enabled = true, description = "GetAvailableUnfreezeCount by http")
  public void test011GetAvailableUnfreezeCount() {
    //use another account for case independence
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] unfreezeAccount = ecKey.getAddress();
    final String unfreezeAccountKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    response = HttpMethed.sendCoin(httpnode, fromAddress, unfreezeAccount, amount * 2, testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    long freezeAmount = 1000000L;
    //without unfreeze, Assert 32
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getAvailableUnfreezeCount(httpnode, unfreezeAccount, true)
    );
    Assert.assertEquals(32L, responseContent.getLongValue("count"));

    response = HttpMethed.freezeBalanceV2(
        httpnode,
        unfreezeAccount,
        freezeAmount,
        0,
        null,
        unfreezeAccountKey
    );
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);

    // unfreezeBalance 1 SUN bandwidth Assert 31
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.unFreezeBalanceV2(httpnode, unfreezeAccount, 1L, 0, unfreezeAccountKey)
    );
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    //query fullNode
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getAvailableUnfreezeCount(httpnode, unfreezeAccount, true)
    );
    Assert.assertEquals(31L, responseContent.getLongValue("count"));
    //query solidity
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getAvailableUnfreezeCountSolidity(httpSoliditynode, unfreezeAccount, true)
    );
    Assert.assertEquals(31L, responseContent.getLongValue("count"));
    //query pbft
    responseContent = HttpMethed.parseResponseContent(
        HttpMethed.getAvailableUnfreezeCountPbft(httpPbftNode, unfreezeAccount, true)
    );
    Assert.assertEquals(31L, responseContent.getLongValue("count"));

  }

  @Test(enabled = true, description = "Test lockPeriod = 1000L")
  public void test012lockPeriodTest() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] fromDelegateAddress = ecKey.getAddress();
    String fromDelegateKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    HttpMethed.sendCoin(httpnode, fromAddress, fromDelegateAddress, amount, testKey002);
    HttpMethed.waitToProduceOneBlock(httpnode);
    HttpMethed.freezeBalanceV2(httpnode, fromDelegateAddress, amount, 1, null, fromDelegateKey);
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.delegateresource(httpnode, fromDelegateAddress, delegateAmount,
        1, true, 1000L, receiverResourceAddress, fromDelegateKey);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("delegateResource:" + responseContent.toJSONString());
    Assert.assertEquals(responseContent.getBoolean("result"), true);
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getDelegatedResourceV2(httpnode, fromDelegateAddress, receiverResourceAddress, true);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info(responseContent.toJSONString());
    Assert.assertTrue(responseContent.getJSONArray("delegatedResource").getJSONObject(0).getLong("expire_time_for_energy") > System.currentTimeMillis());
    Assert.assertEquals(responseContent.getJSONArray("delegatedResource").getJSONObject(0).getLong("frozen_balance_for_energy").longValue(), delegateAmount.longValue());
    Assert.assertEquals(responseContent.getJSONArray("delegatedResource").getJSONObject(0).getString("from"), Base58.encode58Check(fromDelegateAddress));
    Assert.assertEquals(responseContent.getJSONArray("delegatedResource").getJSONObject(0).getString("to"), Base58.encode58Check(receiverResourceAddress));
  }

  /**
   * constructor.
   * */
  @AfterClass
  public void shutdown() throws InterruptedException {
    response = HttpMethed
        .unFreezeBalance(httpnode, fromAddress, frozenBalance, 0, freezeBandwidthAddress,
            testKey002);
    HttpMethed.freedResource(httpnode, freezeBandwidthAddress, fromAddress, freezeBandwidthKey);
    HttpMethed.disConnect();
  }
}