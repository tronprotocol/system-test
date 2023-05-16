package stest.tron.wallet.zkevm;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import stest.tron.wallet.common.client.utils.*;

@Slf4j
public class ZkEvmBridgeApiTest001 {


  @BeforeClass
  public void beforeClass() {
    FullFlow.fromHashTrxNileToZkEvm = "d9ec149f5df18b740ac4abe6ef9aae2618733ac7e5fb3368d1d7b09e076aea9b";
    FullFlow.fromHashUsdtNileToZkEvmFirst = "2f535d066d8a81dfc31adf4a6b908d6bbd9801a47326098363d67e05a04937d4";
    FullFlow.fromHashUsdtNileToZkEvmSecond = "cf5509500812a0790e1550d53c6a90628f28784765201628008907eb5c32101c";
    FullFlow.destAddress = ByteArray.fromHexString("416105e8B80e22f7d4BFa536132B2FD161D9D2df0a");

  }

  @Test(enabled = true)
  public void test01ApiAvailable() {
    HttpResponse response = PublicMethodForZkEvm.checkAPIHttp();
    JSONObject responseContent = PublicMethodForZkEvm.parseResponseContent(response);
    Assert.assertNotNull(responseContent.getString("api"));
    Assert.assertEquals("v1", responseContent.getString("api"));
  }

  @Test(enabled = true)
  public void test01BridgeTransactionInfoTrxToNile() {
    String fromHash = FullFlow.fromHashTrxNileToZkEvm;
    Assert.assertNotNull(fromHash);
    HttpResponse response = PublicMethodForZkEvm.getTransactionInfoHttp(fromHash);
    JSONObject responseContent = PublicMethodForZkEvm.parseResponseContent(response);
    logger.info(responseContent.toJSONString());
    Assert.assertEquals(responseContent.getIntValue("code"), 0);
    Assert.assertEquals(responseContent.getString("msg"), "request success");
    JSONObject transaction = responseContent.getJSONObject("transaction");
    Assert.assertNotNull(transaction);
    Assert.assertEquals(transaction.getString("src_txid"), "0x" + fromHash);
    String foundationAddress = PublicMethodForZkEvm.getETHAddress(PublicMethed.getFinalAddress(ZkEvmClient.nileFoundationKey));
    Assert.assertEquals(transaction.getString("from_address"), foundationAddress);
    String destAddress = PublicMethodForZkEvm.getETHAddress(FullFlow.destAddress);
    Assert.assertEquals(transaction.getString("to_address"), destAddress);
    Assert.assertEquals(transaction.getString("src_token_id"), "0x0000000000000000000000000000000000000000");
    Assert.assertEquals(transaction.getString("dest_token_id"), "0x0000000000000000000000000000000000000000");
    //to Assert Amount...

  }



}
