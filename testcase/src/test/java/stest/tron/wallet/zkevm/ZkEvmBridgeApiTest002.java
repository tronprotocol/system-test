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
    FullFlow.fromHashTrxNileToZkEvm = "0xf2171b68488dbed50abb082a66b1188285f562d752bb8aff5ff76cfa8c8e5895";
    FullFlow.fromHashUsdtNileToZkEvmFirst = "0xf3e8dde5333891f05051ad91aed9b2a02c88fc6b619f392d18ae71ef818bb9c9";
    FullFlow.fromHashUsdtNileToZkEvmSecond = "0xbe65e8ff5d70920e3e1f7f5183a3a4d82a75dbbb34f64e4565854387730d3724";
    FullFlow.fromHashRealUsdtNileToZkEvm = "0x815a25402483217099bb1854269a564748b17bd71f3fe6b4ea6429e0adc1a771";
    FullFlow.zkEvmDepositTrxTxid = "0x0x26dd8cdbf2c18a3bfa888f473dd79c9e92d7875641d367e57051506e918f7535";
    FullFlow.zkEvmDepositUsdtTxid = "0x687c72dabe6bb4f032beadf633de7266493fb7e1d94689cffe7bc50046c52767";
    FullFlow.testAddress = ByteArray.fromHexString("418ea3b57809c9266812b30cf546d32eaed9f7536d");
    FullFlow.usdtErc20Contract ="TNTANRDqPp7MVSJamTdJyg74UY5hFWkdn4";
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
    Assert.assertEquals("6", resContent.getString("total_cnt"));
    JSONArray depositsArray = resContent.getJSONArray("deposits");
    Assert.assertEquals(6, depositsArray.size());
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
      String tx_hash = obj.getString("tx_hash").toLowerCase();
      long block_num = Long.valueOf(obj.getString("block_num"));
      long deposit_cnt = Long.valueOf(obj.getString("deposit_cnt"));
      String claim_tx_hash = obj.getString("claim_tx_hash");
      String metadata = obj.getString("metadata");
      Assert.assertEquals(leaf_type, 0);
      Assert.assertEquals(orig_net, ZkEvmClient.netTron);
      Assert.assertTrue(block_num > 0);
      Assert.assertTrue(deposit_cnt > 0);

      if (tx_hash.contains(FullFlow.fromHashTrxNileToZkEvm.toLowerCase())) {
        bridgesTrxNileToZkEvm = obj;
        Assert.assertEquals(orig_addr.toLowerCase(), ZkEvmClient.zeroAddressInZkEvm);
        Assert.assertEquals(amount, String.valueOf(trxAmount));
        Assert.assertEquals(dest_net, ZkEvmClient.netZkEvm);
        Assert.assertEquals(dest_addr.toLowerCase(), queryAddress.toLowerCase());
        Assert.assertEquals(network_id, ZkEvmClient.netTron);
        Assert.assertTrue(ready_for_claim);
        Assert.assertTrue(claim_tx_hash.length() > 0);
        Assert.assertEquals(metadata, "0x");
      } else if (tx_hash.contains(FullFlow.fromHashUsdtNileToZkEvmFirst.toLowerCase())) {
        Assert.assertEquals(orig_addr.toLowerCase(), usdtAddress);
        Assert.assertEquals(amount, FullFlow.depositUsdtAmount.toString());
        Assert.assertEquals(dest_net, ZkEvmClient.netZkEvm);
        Assert.assertEquals(dest_addr.toLowerCase(), queryAddress.toLowerCase());
        Assert.assertEquals(network_id, ZkEvmClient.netTron);
        Assert.assertTrue(ready_for_claim);
        Assert.assertTrue(claim_tx_hash.length() > 0);
        Assert.assertTrue(metadata.length() > 2);
      } else if (tx_hash.contains(FullFlow.fromHashUsdtNileToZkEvmSecond.toLowerCase())) {
        Assert.assertEquals(orig_addr.toLowerCase(), usdtAddress);
        Assert.assertEquals(amount, FullFlow.depositUsdtAmount.toString());
        Assert.assertEquals(dest_net, ZkEvmClient.netZkEvm);
        Assert.assertEquals(dest_addr.toLowerCase(), queryAddress.toLowerCase());
        Assert.assertEquals(network_id, ZkEvmClient.netTron);
        Assert.assertTrue(ready_for_claim);
        Assert.assertTrue(claim_tx_hash.length() > 0);
        Assert.assertTrue(metadata.length() > 2);
      } else if (tx_hash.contains(FullFlow.fromHashRealUsdtNileToZkEvm.toLowerCase())) {
        Assert.assertEquals(orig_addr.toLowerCase(), ZkEvmClient.usdtAddressInTron.toLowerCase());
        Assert.assertEquals(amount, FullFlow.depositRealUsdtAmount.toString());
        Assert.assertEquals(dest_net, ZkEvmClient.netZkEvm);
        Assert.assertEquals(dest_addr.toLowerCase(), queryAddress.toLowerCase());
        Assert.assertEquals(network_id, ZkEvmClient.netTron);
        Assert.assertTrue(ready_for_claim);
        Assert.assertTrue(claim_tx_hash.length() > 0);
        Assert.assertTrue(metadata.length() > 2);
      } else if (tx_hash.contains(FullFlow.zkEvmDepositTrxTxid.toLowerCase())) {
        Assert.assertEquals(orig_addr.toLowerCase(), ZkEvmClient.zeroAddressInZkEvm);
        Assert.assertEquals(amount, FullFlow.depositTrxFromZkEvmToNileAmount.toString());
        Assert.assertEquals(dest_net, ZkEvmClient.netTron);
        Assert.assertEquals(dest_addr.toLowerCase(), queryAddress.toLowerCase());
        Assert.assertEquals(network_id, ZkEvmClient.netZkEvm);
        Assert.assertEquals(metadata, "0x");
//        Assert.assertFalse(ready_for_claim);
      } else if (tx_hash.contains(FullFlow.zkEvmDepositUsdtTxid.toLowerCase())) {
        Assert.assertEquals(orig_addr.toLowerCase(), usdtAddress.toLowerCase());
        Assert.assertEquals(Long.valueOf(amount).longValue(), FullFlow.depositUsdtFromZkEvmToNileAmount.longValue());
        Assert.assertEquals(dest_net, ZkEvmClient.netTron);
        Assert.assertEquals(dest_addr.toLowerCase(), queryAddress.toLowerCase());
        Assert.assertEquals(network_id, ZkEvmClient.netZkEvm);
        Assert.assertEquals(metadata, "0x");
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
      Assert.assertEquals(logo, tokenInfo.getString("logo"));
      Assert.assertTrue(Double.valueOf(price) > 0);
      if (address.equalsIgnoreCase(ZkEvmClient.usdtAddressInZkEvm)) {
        Assert.assertEquals(tokenPrecision,  ZkEvmClient.tokenPrecision6);
        Assert.assertEquals(tokenType, ZkEvmClient.tokenTypeErc20);
        Assert.assertEquals(balance, String.valueOf(FullFlow.depositRealUsdtAmount));
        Assert.assertEquals(tokenInfo.getString("address"), ZkEvmClient.usdtAddressInTron);
        Assert.assertEquals(tokenPrecision, tokenInfo.getIntValue("tokenPrecision"));
        Assert.assertEquals(mainTokenName, "Tether USD");
        Assert.assertEquals(mainSymbol, "USDT");
        Assert.assertEquals(logo, "https://static.bt.io/production/BTTC_Logo/USDT.svg");

      } else if (address.equalsIgnoreCase(ZkEvmClient.zeroAddressInZkEvm)) {
        Assert.assertEquals(tokenPrecision, ZkEvmClient.tokenPrecision18);
        Assert.assertEquals(tokenInfo.getIntValue("tokenPrecision"), ZkEvmClient.tokenPrecision6);
        Assert.assertEquals(tokenType, ZkEvmClient.tokenTypeTrx);
        Assert.assertTrue(Double.valueOf(balance) > 0);
        Assert.assertEquals(tokenInfo.getString("address"), ZkEvmClient.zeroAddressInZkEvm);
        Assert.assertEquals(mainTokenName, "TRX");
        Assert.assertEquals(mainSymbol, "TRX");
        Assert.assertEquals(logo, "https://static.bt.io/production/upload/logo/TNUC9Qb1rRpS5CbWLmNMxXBjyFoydXjWFR.png?t=1598430824415");
      } else if (address.equalsIgnoreCase(ZkEvmClient.bttAddressInZkEvm)) {
        Assert.assertEquals(tokenPrecision, ZkEvmClient.tokenPrecision18);
        Assert.assertEquals(tokenPrecision, tokenInfo.getIntValue("tokenPrecision"));
        Assert.assertEquals(tokenType, ZkEvmClient.tokenTypeErc20);
        Assert.assertEquals(balance, "0");
        Assert.assertEquals(tokenInfo.getString("address"), ZkEvmClient.bttAddressInTron);
        Assert.assertEquals(mainTokenName, "BitTorrent");
        Assert.assertEquals(mainSymbol, "BTT");
        Assert.assertEquals(logo, "https://static.bt.io/production/logo/1002000.png");

      } else if (address.equalsIgnoreCase(ZkEvmClient.usddAddressInZkEvm)) {
        Assert.assertEquals(tokenPrecision, ZkEvmClient.tokenPrecision18);
        Assert.assertEquals(tokenPrecision, tokenInfo.getIntValue("tokenPrecision"));
        Assert.assertEquals(tokenType, ZkEvmClient.tokenTypeErc20);
        Assert.assertEquals(balance, "0");
        Assert.assertEquals(tokenInfo.getString("address"), ZkEvmClient.usddAddressInTron);
        Assert.assertEquals(mainTokenName, "Decentralized USD");
        Assert.assertEquals(mainSymbol, "USDD");
        Assert.assertEquals(logo, "https://usdd-images.s3.amazonaws.com/images/USDD.png");
      } else if (address.equalsIgnoreCase(ZkEvmClient.maticAddressInZkEvm)) {
        Assert.assertEquals(tokenPrecision, ZkEvmClient.tokenPrecision18);
        Assert.assertEquals(tokenPrecision, tokenInfo.getIntValue("tokenPrecision"));
        Assert.assertEquals(tokenType, ZkEvmClient.tokenTypeErc20);
        Assert.assertEquals(balance, "0");
        Assert.assertEquals(tokenInfo.getString("address"), ZkEvmClient.maticAddressInTron);
        Assert.assertEquals(mainTokenName, "Matic Token");
        Assert.assertEquals(mainSymbol, "MATIC");
        Assert.assertEquals(logo, "https://static.bt.io/production/BTTC_Logo/MATIC.svg");
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
      if (address.equalsIgnoreCase(ZkEvmClient.usdtAddressInTron)) {
        Assert.assertEquals(tokenPrecision, ZkEvmClient.tokenPrecision6);
        Assert.assertEquals(tokenType, ZkEvmClient.tokenTypeTrc20);
        Assert.assertEquals(balance, String.valueOf(FullFlow.depositRealUsdtAmount));
        Assert.assertEquals(tokenInfo.getString("address"), ZkEvmClient.usdtAddressInZkEvm);
        Assert.assertEquals(tokenPrecision, tokenInfo.getIntValue("tokenPrecision"));
        Assert.assertEquals(mainTokenName, "Tether USD");
        Assert.assertEquals(mainSymbol, "USDT");
        Assert.assertEquals(logo, "https://static.bt.io/production/BTTC_Logo/USDT.svg");
      } else if (address.equalsIgnoreCase(ZkEvmClient.zeroAddressInZkEvm)) {
        Assert.assertEquals(tokenPrecision, ZkEvmClient.tokenPrecision6);
        Assert.assertEquals(tokenInfo.getIntValue("tokenPrecision"), ZkEvmClient.tokenPrecision18);
        Assert.assertEquals(tokenType, ZkEvmClient.tokenTypeTrx);
        Assert.assertEquals(balance, "0");
        Assert.assertEquals(tokenInfo.getString("address"), ZkEvmClient.zeroAddressInZkEvm);
        Assert.assertEquals(mainTokenName, "TRX");
        Assert.assertEquals(mainSymbol, "TRX");
        Assert.assertEquals(logo, "https://static.bt.io/production/upload/logo/TNUC9Qb1rRpS5CbWLmNMxXBjyFoydXjWFR.png?t=1598430824415");
      } else if (address.equalsIgnoreCase(ZkEvmClient.bttAddressInTron)) {
        Assert.assertEquals(tokenPrecision, 18);
        Assert.assertEquals(tokenPrecision, tokenInfo.getIntValue("tokenPrecision"));
        Assert.assertEquals(tokenType, ZkEvmClient.tokenTypeTrc20);
        Assert.assertEquals(balance, "0");
        Assert.assertEquals(tokenInfo.getString("address"), ZkEvmClient.bttAddressInZkEvm);
        Assert.assertEquals(mainTokenName, "BitTorrent");
        Assert.assertEquals(mainSymbol, "BTT");
        Assert.assertEquals(logo, "https://static.bt.io/production/logo/1002000.png");
      } else if (address.equalsIgnoreCase(ZkEvmClient.usddAddressInTron)) {
        Assert.assertEquals(tokenPrecision, ZkEvmClient.tokenPrecision18);
        Assert.assertEquals(tokenPrecision, tokenInfo.getIntValue("tokenPrecision"));
        Assert.assertEquals(tokenType, ZkEvmClient.tokenTypeTrc20);
        Assert.assertEquals(balance, "0");
        Assert.assertEquals(tokenInfo.getString("address"), ZkEvmClient.usddAddressInZkEvm);
        Assert.assertEquals(mainTokenName, "Decentralized USD");
        Assert.assertEquals(mainSymbol, "USDD");
        Assert.assertEquals(logo, "https://usdd-images.s3.amazonaws.com/images/USDD.png");
      } else if (address.equalsIgnoreCase(ZkEvmClient.maticAddressInTron)) {
        Assert.assertEquals(tokenPrecision, ZkEvmClient.tokenPrecision18);
        Assert.assertEquals(tokenPrecision, tokenInfo.getIntValue("tokenPrecision"));
        Assert.assertEquals(tokenType, ZkEvmClient.tokenTypeTrc20);
        Assert.assertEquals(balance, "0");
        Assert.assertEquals(tokenInfo.getString("address"), ZkEvmClient.maticAddressInZkEvm);
        Assert.assertEquals(mainTokenName, "Matic Token");
        Assert.assertEquals(mainSymbol, "MATIC");
        Assert.assertEquals(logo, "https://static.bt.io/production/BTTC_Logo/MATIC.svg");
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
