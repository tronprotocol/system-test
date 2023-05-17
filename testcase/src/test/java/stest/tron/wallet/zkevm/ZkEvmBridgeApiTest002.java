package stest.tron.wallet.zkevm;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpResponse;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.PublicMethodForZkEvm;
import stest.tron.wallet.common.client.utils.ZkEvmClient;

import java.math.BigInteger;

public class ZkEvmBridgeApiTest002 {
  String trxAmount = new BigInteger(String.valueOf(FullFlow.depositTrxAmount))
      .multiply(new BigInteger(String.valueOf(FullFlow.trxToZkEvmPrecision))).toString();
  String queryAddress ;
  String usdtAddress ;
  JSONObject bridgesTrxNileToZkEvm;

  @BeforeClass
  public void beforeClass() {
    FullFlow.fromHashTrxNileToZkEvm = "0xcb97b8db88a9c8a9826b9ea314654e4cf187daae41d92097ac2d516fe4223098";
    FullFlow.fromHashUsdtNileToZkEvmFirst = "0x6fc85450acd38a2ae41f5d648bea9d31662a8ecba56746e666c0397349aea147";
    FullFlow.fromHashUsdtNileToZkEvmSecond = "0x3931fd8933430e74468f09c66d282150fca714bd1fff23424f5b948c78807648";
    FullFlow.zkEvmDepositTrxTxid = "0x0377726fbea7d6ada40d8f508d7c62c55a8e83faedce96e496ddfd79c5ac8233";
    FullFlow.zkEvmDepositUsdtTxid = "0x56343c4430f628a662e5383c212fb4fa80b5558ad0cb24e4f8909981e12eee14";
    FullFlow.testAddress = ByteArray.fromHexString("410D649907E96B904DA1A03E5C96CF705C4EBE73BF");
    FullFlow.usdtErc20Contract ="TSijB7uecTp6homYzEc7XbBG3H8gcmGq9d";
     queryAddress = PublicMethodForZkEvm.getETHAddress(FullFlow.testAddress);
     usdtAddress = PublicMethodForZkEvm.
        getETHAddress(PublicMethed.decode58Check(FullFlow.usdtErc20Contract)).toLowerCase();
  }

  /** constructor. */
  @Test(enabled = true, description = "api bridges")
  public void test01Bridges() {

    int offset = 0;
    int limit = 30;
    HttpResponse response = PublicMethodForZkEvm.getBridgesHttp(queryAddress, offset, limit);
    JSONObject resContent = PublicMethodForZkEvm.parseResponseContent(response);
    Assert.assertEquals(2, resContent.size());
    Assert.assertEquals("5", resContent.getString("total_cnt"));
    JSONArray depositsArray = resContent.getJSONArray("deposits");
    Assert.assertEquals(5, depositsArray.size());
    for (Object tem : depositsArray) {
      JSONObject obj = (JSONObject)tem;
      int leaf_type = obj.getIntValue("leaf_type");
      int orig_net = obj.getIntValue("orig_net");
      String orig_addr = obj.getString("orig_addr");
      String amount = obj.getString("amount");
      int dest_net = obj.getIntValue("dest_net");
      String dest_addr = obj.getString("dest_addr");
      int network_id = obj.getIntValue("network_id");
      boolean ready_for_claim = obj.getBoolean("ready_for_claim");
      String txHash = obj.getString("tx_hash").toLowerCase();
      Assert.assertEquals(leaf_type, 0);
      Assert.assertEquals(orig_net, ZkEvmClient.netTron);

      if (txHash.contains(FullFlow.fromHashTrxNileToZkEvm.toLowerCase())) {
        bridgesTrxNileToZkEvm = obj;
        System.out.println("11111111"+bridgesTrxNileToZkEvm);
        Assert.assertEquals(orig_addr.toLowerCase(), ZkEvmClient.zeroAddressInNile);
        Assert.assertEquals(amount, String.valueOf(trxAmount));
        Assert.assertEquals(dest_net, ZkEvmClient.netZkEvm);
        Assert.assertEquals(dest_addr.toLowerCase(), queryAddress.toLowerCase());
        Assert.assertEquals(network_id, ZkEvmClient.netTron);
        Assert.assertTrue(ready_for_claim);
      } else if (txHash.contains(FullFlow.fromHashUsdtNileToZkEvmFirst.toLowerCase())) {
        Assert.assertEquals(orig_addr.toLowerCase(), usdtAddress);
        Assert.assertEquals(amount, FullFlow.depositUsdtAmount.intValue());
        Assert.assertEquals(dest_net, ZkEvmClient.netZkEvm);
        Assert.assertEquals(dest_addr.toLowerCase(), queryAddress.toLowerCase());
        Assert.assertEquals(network_id, ZkEvmClient.netTron);
        Assert.assertTrue(ready_for_claim);
      } else if (txHash.contains(FullFlow.fromHashUsdtNileToZkEvmSecond.toLowerCase())) {
        Assert.assertEquals(orig_addr.toLowerCase(), usdtAddress);
        Assert.assertEquals(amount, FullFlow.depositUsdtAmount.intValue());
        Assert.assertEquals(dest_net, ZkEvmClient.netZkEvm);
        Assert.assertEquals(dest_addr.toLowerCase(), queryAddress.toLowerCase());
        Assert.assertEquals(network_id, ZkEvmClient.netTron);
        Assert.assertTrue(ready_for_claim);
      } else if (txHash.contains(FullFlow.zkEvmDepositTrxTxid.toLowerCase())) {
        Assert.assertEquals(orig_addr.toLowerCase(), ZkEvmClient.zeroAddressInZkEvm);
        Assert.assertEquals(amount, FullFlow.depositTrxFromZkEvmToNileAmount.intValue());
        Assert.assertEquals(dest_net, ZkEvmClient.netTron);
        Assert.assertEquals(dest_addr.toLowerCase(), queryAddress.toLowerCase());
        Assert.assertEquals(network_id, ZkEvmClient.netZkEvm);
        Assert.assertFalse(ready_for_claim);
      } else if (txHash.contains(FullFlow.zkEvmDepositUsdtTxid.toLowerCase())) {
        Assert.assertEquals(orig_addr.toLowerCase(), usdtAddress.toLowerCase());
//        Assert.assertEquals(Long.valueOf(amount).longValue(), FullFlow.depositUsdtFromZkEvmToNileAmount.longValue());
        Assert.assertEquals(dest_net, ZkEvmClient.netTron);
        Assert.assertEquals(dest_addr.toLowerCase(), queryAddress.toLowerCase());
        Assert.assertEquals(network_id, ZkEvmClient.netZkEvm);
//        Assert.assertFalse(ready_for_claim);
      }
    }

  }

  /** constructor. */
  @Test(enabled = true, description = "api bridge")
  public void test02Bridge() {
    System.out.println(bridgesTrxNileToZkEvm.toJSONString());
    String cnt = bridgesTrxNileToZkEvm.getString("deposit_cnt");
    HttpResponse response = PublicMethodForZkEvm.getBridgeHttp(ZkEvmClient.netTron, Integer.valueOf(cnt));
    JSONObject resContent = PublicMethodForZkEvm.parseResponseContent(response);
    JSONObject bridgeTrxNileToZkEvm = resContent.getJSONObject("deposit");
    Assert.assertEquals(bridgesTrxNileToZkEvm.toJSONString(), bridgeTrxNileToZkEvm);
  }

  /** constructor. */
  @Test(enabled = true, description = "api getAccountTokenBalance")
  public void test03GetAccountTokenBalance() {
    HttpResponse response = PublicMethodForZkEvm.getAccountTokenBalanceHttp(queryAddress, Integer.valueOf(ZkEvmClient.netZkEvm));
    JSONObject resContent = PublicMethodForZkEvm.parseResponseContent(response);
    Assert.assertEquals(6, resContent.size());
    JSONArray dataArray = resContent.getJSONArray("data");
    for (Object tem : dataArray) {
      JSONObject obj = (JSONObject) tem;
      int chainId = obj.getIntValue("chainId");
      int isMainChain = obj.getIntValue("isMainChain");
      String mainTokenName = obj.getString("mainTokenName");
      String mainSymbol = obj.getString("mainSymbol");
      String address = obj.getString("address");
      int tokenPrecision = obj.getIntValue("tokenPrecision");
      String price = obj.getString("price");
      String prtokenTypeice = obj.getString("tokenType");
      String logo = obj.getString("logo");
      String balance = obj.getString("balance");
      int mainChainId = obj.getIntValue("mainChainId");
      JSONArray mapTokenInfo = obj.getJSONArray("mapTokenInfo");



      if(address.equalsIgnoreCase(usdtAddress)) {

      }
    }
  }

  /** constructor. */
  @Test(enabled = true, description = "api gas-estimate")
  public void test04GasEstimate() {

  }
}
