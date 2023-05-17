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
    FullFlow.depositTrxFromZkEvmToNileAmount = new BigInteger("1000000000000");
    FullFlow.depositUsdtFromZkEvmToNileAmount = BigInteger.ONE;
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
        Assert.assertEquals(orig_addr.toLowerCase(), ZkEvmClient.zeroAddressInZkEvm);
        Assert.assertEquals(amount, String.valueOf(trxAmount));
        Assert.assertEquals(dest_net, ZkEvmClient.netZkEvm);
        Assert.assertEquals(dest_addr.toLowerCase(), queryAddress.toLowerCase());
        Assert.assertEquals(network_id, ZkEvmClient.netTron);
        Assert.assertTrue(ready_for_claim);
      } else if (txHash.contains(FullFlow.fromHashUsdtNileToZkEvmFirst.toLowerCase())) {
        Assert.assertEquals(orig_addr.toLowerCase(), usdtAddress);
        Assert.assertEquals(amount, FullFlow.depositUsdtAmount.toString());
        Assert.assertEquals(dest_net, ZkEvmClient.netZkEvm);
        Assert.assertEquals(dest_addr.toLowerCase(), queryAddress.toLowerCase());
        Assert.assertEquals(network_id, ZkEvmClient.netTron);
        Assert.assertTrue(ready_for_claim);
      } else if (txHash.contains(FullFlow.fromHashUsdtNileToZkEvmSecond.toLowerCase())) {
        Assert.assertEquals(orig_addr.toLowerCase(), usdtAddress);
        Assert.assertEquals(amount, FullFlow.depositUsdtAmount.toString());
        Assert.assertEquals(dest_net, ZkEvmClient.netZkEvm);
        Assert.assertEquals(dest_addr.toLowerCase(), queryAddress.toLowerCase());
        Assert.assertEquals(network_id, ZkEvmClient.netTron);
        Assert.assertTrue(ready_for_claim);
      } else if (txHash.contains(FullFlow.zkEvmDepositTrxTxid.toLowerCase())) {
        Assert.assertEquals(orig_addr.toLowerCase(), ZkEvmClient.zeroAddressInZkEvm);
        Assert.assertEquals(amount, FullFlow.depositTrxFromZkEvmToNileAmount.toString());
        Assert.assertEquals(dest_net, ZkEvmClient.netTron);
        Assert.assertEquals(dest_addr.toLowerCase(), queryAddress.toLowerCase());
        Assert.assertEquals(network_id, ZkEvmClient.netZkEvm);
//        Assert.assertFalse(ready_for_claim);
      } else if (txHash.contains(FullFlow.zkEvmDepositUsdtTxid.toLowerCase())) {
        Assert.assertEquals(orig_addr.toLowerCase(), usdtAddress.toLowerCase());
        Assert.assertEquals(Long.valueOf(amount).longValue(), FullFlow.depositUsdtFromZkEvmToNileAmount.longValue());
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
    Assert.assertEquals(bridgesTrxNileToZkEvm.toJSONString(), bridgeTrxNileToZkEvm.toJSONString());
  }

  /** constructor. */
  @Test(enabled = true, description = "api getAccountTokenBalance in zkEvm")
  public void test03GetAccountTokenBalance() {
    HttpResponse response = PublicMethodForZkEvm.getAccountTokenBalanceHttp(queryAddress, Integer.valueOf(ZkEvmClient.netZkEvm));
    JSONObject resContent = PublicMethodForZkEvm.parseResponseContent(response);
    Assert.assertEquals(3, resContent.size());
    JSONArray dataArray = resContent.getJSONArray("data");
    Assert.assertEquals(5, dataArray.size());
    for (Object tem : dataArray) {
      JSONObject obj = (JSONObject) tem;
      int chainId = obj.getIntValue("chainId");
      int isMainChain = obj.getIntValue("isMainChain");
      String mainTokenName = obj.getString("mainTokenName");
      String mainSymbol = obj.getString("mainSymbol");
      String address = obj.getString("address");
      int tokenPrecision = obj.getIntValue("tokenPrecision");
      String price = obj.getString("price");
      String tokenType = obj.getString("tokenType");
      String logo = obj.getString("logo");
      String balance = obj.getString("balance");
      int mainChainId = obj.getIntValue("mainChainId");
      JSONArray mapTokenInfo = obj.getJSONArray("mapTokenInfo");
      Assert.assertEquals(mapTokenInfo.size(), 1);
      JSONObject tokenInfo = mapTokenInfo.getJSONObject(0);
      Assert.assertEquals(chainId, ZkEvmClient.netZkEvm);
      Assert.assertEquals(isMainChain, 0);
      Assert.assertEquals(mainChainId, 0);
      Assert.assertEquals(tokenInfo.getIntValue("chainId"),ZkEvmClient.netTron);
      Assert.assertEquals(mainTokenName, tokenInfo.getString("mainTokenName"));
      Assert.assertEquals(mainSymbol, tokenInfo.getString("mainSymbol"));
      Assert.assertEquals(mainTokenName, tokenInfo.getString("tokenName"));
      Assert.assertEquals(mainSymbol, tokenInfo.getString("symbol"));

      Assert.assertTrue(Double.valueOf(price) > 0);
      Assert.assertTrue(logo.length() > 0);
      Assert.assertTrue(Double.valueOf(price) > 0);
      if (address.equalsIgnoreCase(ZkEvmClient.usdtAddressInZkEvm)) {
        Assert.assertEquals(tokenPrecision, 6);
        Assert.assertEquals(tokenType, ZkEvmClient.tokenTypeErc20);
        Assert.assertEquals(balance, "0");
        Assert.assertEquals(tokenInfo.getString("address"), ZkEvmClient.usdtAddressInTron);
        Assert.assertEquals(tokenPrecision, tokenInfo.getIntValue("tokenPrecision"));
//        Assert.assertEquals(balance, FullFlow.depositUsdtAmount *2 - FullFlow.depositUsdtFromZkEvmToNileAmount.longValue());
      } else if (address.equalsIgnoreCase(ZkEvmClient.zeroAddressInZkEvm)) {
        Assert.assertEquals(tokenPrecision, 18);
        Assert.assertEquals(tokenInfo.getIntValue("tokenPrecision"), 6);
        Assert.assertEquals(tokenType, ZkEvmClient.tokenTypeTrx);
        Assert.assertTrue(Double.valueOf(balance) > 0);
        Assert.assertEquals(tokenInfo.getString("address"), ZkEvmClient.zeroAddressInZkEvm);
      } else if (address.equalsIgnoreCase(ZkEvmClient.bttAddressInZkEvm)) {
        Assert.assertEquals(tokenPrecision, 18);
        Assert.assertEquals(tokenPrecision, tokenInfo.getIntValue("tokenPrecision"));
        Assert.assertEquals(tokenType, ZkEvmClient.tokenTypeErc20);
        Assert.assertEquals(balance, "0");
        Assert.assertEquals(tokenInfo.getString("address"), ZkEvmClient.bttAddressInTron);
      } else if (address.equalsIgnoreCase(ZkEvmClient.usddAddressInZkEvm)) {
        Assert.assertEquals(tokenPrecision, 18);
        Assert.assertEquals(tokenPrecision, tokenInfo.getIntValue("tokenPrecision"));
        Assert.assertEquals(tokenType, ZkEvmClient.tokenTypeErc20);
        Assert.assertEquals(balance, "0");
        Assert.assertEquals(tokenInfo.getString("address"), ZkEvmClient.usddAddressInTron);
      } else if (address.equalsIgnoreCase(ZkEvmClient.maticAddressInZkEvm)) {
        Assert.assertEquals(tokenPrecision, 18);
        Assert.assertEquals(tokenPrecision, tokenInfo.getIntValue("tokenPrecision"));
        Assert.assertEquals(tokenType, ZkEvmClient.tokenTypeErc20);
        Assert.assertEquals(balance, "0");
        Assert.assertEquals(tokenInfo.getString("address"), ZkEvmClient.maticAddressInTron);
      }
    }
  }

  /** constructor. */
  @Test(enabled = true, description = "api getAccountTokenBalance in tron")
  public void test04GetAccountTokenBalance() {
    HttpResponse response = PublicMethodForZkEvm.getAccountTokenBalanceHttp(queryAddress, Integer.valueOf(ZkEvmClient.netTron));
    JSONObject resContent = PublicMethodForZkEvm.parseResponseContent(response);
    Assert.assertEquals(3, resContent.size());
    JSONArray dataArray = resContent.getJSONArray("data");
    Assert.assertEquals(5, dataArray.size());
    for (Object tem : dataArray) {
      JSONObject obj = (JSONObject) tem;
      int chainId = obj.getIntValue("chainId");
      int isMainChain = obj.getIntValue("isMainChain");
      String mainTokenName = obj.getString("mainTokenName");
      String mainSymbol = obj.getString("mainSymbol");
      String address = obj.getString("address");
      int tokenPrecision = obj.getIntValue("tokenPrecision");
      String price = obj.getString("price");
      String tokenType = obj.getString("tokenType");
      String logo = obj.getString("logo");
      String balance = obj.getString("balance");
      int mainChainId = obj.getIntValue("mainChainId");
      JSONArray mapTokenInfo = obj.getJSONArray("mapTokenInfo");
      Assert.assertEquals(mapTokenInfo.size(), 1);
      JSONObject tokenInfo = mapTokenInfo.getJSONObject(0);
      Assert.assertEquals(chainId, ZkEvmClient.netTron);
      Assert.assertEquals(isMainChain, 1);
      Assert.assertEquals(mainChainId, 0);
      Assert.assertEquals(tokenInfo.getIntValue("chainId"),ZkEvmClient.netZkEvm);
      Assert.assertEquals(mainTokenName, tokenInfo.getString("mainTokenName"));
      Assert.assertEquals(mainSymbol, tokenInfo.getString("mainSymbol"));
      Assert.assertEquals(mainTokenName, tokenInfo.getString("tokenName"));
      Assert.assertEquals(mainSymbol, tokenInfo.getString("symbol"));

      Assert.assertTrue(Double.valueOf(price) > 0);
      Assert.assertTrue(logo.length() > 0);
      Assert.assertTrue(Double.valueOf(price) > 0);
      if (address.equalsIgnoreCase(ZkEvmClient.usdtAddressInTron)) {
        Assert.assertEquals(tokenPrecision, 6);
        Assert.assertEquals(tokenType, ZkEvmClient.tokenTypeTrc20);
        Assert.assertEquals(balance, "0");
        Assert.assertEquals(tokenInfo.getString("address"), ZkEvmClient.usdtAddressInTron);
        Assert.assertEquals(tokenPrecision, tokenInfo.getIntValue("tokenPrecision"));
//        Assert.assertEquals(balance, FullFlow.depositUsdtAmount *2 - FullFlow.depositUsdtFromZkEvmToNileAmount.longValue());
      } else if (address.equalsIgnoreCase(ZkEvmClient.zeroAddressInZkEvm)) {
        Assert.assertEquals(tokenPrecision, 6);
        Assert.assertEquals(tokenInfo.getIntValue("tokenPrecision"), 18);
        Assert.assertEquals(tokenType, ZkEvmClient.tokenTypeTrx);
        Assert.assertEquals(balance, "0");
        Assert.assertEquals(tokenInfo.getString("address"), ZkEvmClient.zeroAddressInZkEvm);
      } else if (address.equalsIgnoreCase(ZkEvmClient.bttAddressInTron)) {
        Assert.assertEquals(tokenPrecision, 18);
        Assert.assertEquals(tokenPrecision, tokenInfo.getIntValue("tokenPrecision"));
        Assert.assertEquals(tokenType, ZkEvmClient.tokenTypeTrc20);
        Assert.assertEquals(balance, "0");
        Assert.assertEquals(tokenInfo.getString("address"), ZkEvmClient.bttAddressInTron);
      } else if (address.equalsIgnoreCase(ZkEvmClient.usddAddressInTron)) {
        Assert.assertEquals(tokenPrecision, 18);
        Assert.assertEquals(tokenPrecision, tokenInfo.getIntValue("tokenPrecision"));
        Assert.assertEquals(tokenType, ZkEvmClient.tokenTypeTrc20);
        Assert.assertEquals(balance, "0");
        Assert.assertEquals(tokenInfo.getString("address"), ZkEvmClient.usddAddressInTron);
      } else if (address.equalsIgnoreCase(ZkEvmClient.maticAddressInTron)) {
        Assert.assertEquals(tokenPrecision, 18);
        Assert.assertEquals(tokenPrecision, tokenInfo.getIntValue("tokenPrecision"));
        Assert.assertEquals(tokenType, ZkEvmClient.tokenTypeTrc20);
        Assert.assertEquals(balance, "0");
        Assert.assertEquals(tokenInfo.getString("address"), ZkEvmClient.maticAddressInTron);
      }
    }
  }

  /** constructor. */
  @Test(enabled = true, description = "api gas-estimate")
  public void test05GasEstimate() {
    HttpResponse response = PublicMethodForZkEvm.gasEstimateHttp(ZkEvmClient.netTron, ZkEvmClient.usdtAddressInTron);
    JSONObject resContent = PublicMethodForZkEvm.parseResponseContent(response);

  }
}
