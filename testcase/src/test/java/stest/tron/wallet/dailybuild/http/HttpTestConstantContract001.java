package stest.tron.wallet.dailybuild.http;

import com.alibaba.fastjson.JSONObject;
import java.util.HashMap;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.*;

@Slf4j
public class HttpTestConstantContract001 {

  private static String contractName;
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] assetOwnerAddress = ecKey2.getAddress();
  String assetOwnerKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  String contractAddress;
  Long amount = 2048000000L;
  private JSONObject responseContent;
  private HttpResponse response;
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(0);
  private String httpnode1 = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(1);
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Deploy constant contract by http")
  public void test1DeployConstantContract() {
    PublicMethed.printAddress(assetOwnerKey);
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.sendCoin(httpnode, fromAddress, assetOwnerAddress, amount, testKey002);
    String jsonRpcOwnerKey =
        Configuration.getByPath("testng.conf").getString("defaultParameter.jsonRpcOwnerKey");
    byte[] jsonRpcOwnerAddress = PublicMethed.getFinalAddress(jsonRpcOwnerKey);
    response = HttpMethed.getAccount(httpnode, jsonRpcOwnerAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    String tokenId = responseContent.getString("asset_issued_ID");
    response = HttpMethed.transferAsset(httpnode, jsonRpcOwnerAddress, assetOwnerAddress, tokenId, 10L, jsonRpcOwnerKey);

    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    String filePath = "src/test/resources/soliditycode/constantContract001.sol";
    contractName = "testConstantContract";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    String txid = HttpMethed
        .deployContractGetTxid(httpnode, contractName, abi, code, 1000000L, 1000000000L, 100,
            11111111111111L, 100L, Integer.valueOf(tokenId), 5L, assetOwnerAddress, assetOwnerKey);

    HttpMethed.waitToProduceOneBlock(httpnode);
    logger.info(txid);
    response = HttpMethed.getTransactionById(httpnode, txid);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(!responseContent.getString("contract_address").isEmpty());
    contractAddress = responseContent.getString("contract_address");

    response = HttpMethed.getTransactionInfoById(httpnode, txid);
    responseContent = HttpMethed.parseResponseContent(response);
    String receiptString = responseContent.getString("receipt");
    Assert
        .assertEquals(HttpMethed.parseStringContent(receiptString).getString("result"), "SUCCESS");
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get constant contract by http")
  public void test2GetConstantContract() {
    response = HttpMethed.getContract(httpnode, contractAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(responseContent.getString("consume_user_resource_percent"), "100");
    Assert.assertEquals(responseContent.getString("contract_address"), contractAddress);
    Assert.assertEquals(responseContent.getString("origin_address"),
        ByteArray.toHexString(assetOwnerAddress));
    Assert.assertEquals(responseContent.getString("origin_energy_limit"), "11111111111111");
    Assert.assertEquals(responseContent.getString("name"), contractName);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Trigger constant contract without parameterString by http")
  public void test3TriggerConstantContract() {
    String param1 =
        "000000000000000000000000000000000000000000000000000000000000000" + Integer.toHexString(3);
    String param2 =
        "00000000000000000000000000000000000000000000000000000000000000" + Integer.toHexString(30);
    logger.info(param1);
    logger.info(param2);
    String param = param1 + param2;
    logger.info(ByteArray.toHexString(assetOwnerAddress));
    response = HttpMethed.triggerConstantContract(httpnode, assetOwnerAddress, contractAddress,
        "testPure(uint256,uint256)", param, 1000000000L, assetOwnerKey);
    HttpMethed.waitToProduceOneBlock(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(!responseContent.getString("transaction").isEmpty());
    JSONObject transactionObject = HttpMethed
        .parseStringContent(responseContent.getString("transaction"));
    Assert.assertTrue(!transactionObject.getString("raw_data").isEmpty());
    Assert.assertTrue(!transactionObject.getString("raw_data_hex").isEmpty());
    Assert.assertTrue(responseContent.getIntValue("energy_used") > 400
        && responseContent.getIntValue("energy_used") < 500);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Trigger constant contract with call_value")
  public void test4TriggerConstantContract() {
    String method = "testCallValue()";
    String param = null;
    response = HttpMethed
        .triggerConstantContractWithData(
            httpnode, fromAddress, contractAddress, method, param, null, 10, 0, 0);
    HttpMethed.waitToProduceOneBlock(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(!responseContent.getString("transaction").isEmpty());
    JSONObject transactionObject = responseContent.getJSONObject("transaction");

    boolean result = responseContent.getJSONObject("result").getBoolean("result");
    Assert.assertTrue(result);
    Assert.assertTrue(!transactionObject.getString("raw_data").isEmpty());
    Assert.assertTrue(!transactionObject.getString("raw_data_hex").isEmpty());
    Assert.assertEquals(211, responseContent.getIntValue("energy_used") );
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = " estimate kill function by triggerconstant")
  public void test5TriggerConstantContract() {
    String assetOwnerAddress41 = ByteArray.toHexString(assetOwnerAddress);
    String method = "killme(address)";
    String param = "0000000000000000000000"+assetOwnerAddress41;
    response = HttpMethed
        .triggerConstantContractWithData(
            httpnode, fromAddress, contractAddress, method, param, null, 0, 0, 0);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("triggerconstant result: " + responseContent);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(!responseContent.getString("transaction").isEmpty());
    JSONObject transactionObject = responseContent.getJSONObject("transaction");

    boolean result = responseContent.getJSONObject("result").getBoolean("result");
    Assert.assertTrue(result);
    Assert.assertTrue(!transactionObject.getString("raw_data").isEmpty());
    Assert.assertTrue(!transactionObject.getString("raw_data_hex").isEmpty());
    long energyRequiredTriggerConstant =  responseContent.getIntValue("energy_used");

    response = HttpMethed
        .getEstimateEnergy(httpnode1, fromAddress, ByteArray.fromHexString(contractAddress), method, param, null,false, 0, 0, 0);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("estimate result: " + responseContent.toJSONString());
    long energyRequiredEstimate = responseContent.getLong("energy_required");
    ManagedChannel channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    WalletGrpc.WalletBlockingStub blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    final Long energyFee = PublicMethed.getChainParametersValue("getEnergyFee", blockingStubFull);
    Assert.assertTrue((energyRequiredEstimate - energyRequiredTriggerConstant) * energyFee <= 1000000L);
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethed.freedResource(httpnode, assetOwnerAddress, fromAddress, assetOwnerKey);
    HttpMethed.disConnect();
  }
}