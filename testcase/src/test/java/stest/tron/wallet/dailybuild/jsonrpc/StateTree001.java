package stest.tron.wallet.dailybuild.jsonrpc;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannelBuilder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.HttpMethod;
import stest.tron.wallet.common.client.utils.JsonRpcBase;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;


@Slf4j
public class StateTree001 extends JsonRpcBase {
  private JSONObject responseContent;
  private HttpResponse response;

  ECKey getBalanceECKey = new ECKey(Utils.getRandom());
  byte[] getBalanceTestAddress = getBalanceECKey.getAddress();
  String getBalanceTestKey = ByteArray.toHexString(getBalanceECKey.getPrivKeyBytes());

  private final String foundationKey001 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final Long sendAmount = 20000000L;
  private final Long transferAmount = 2L;

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    if(!stateRootIsOpen()) {
      throw new SkipException("Skipping this state root test case");
    }
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext()
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    PublicMethod.printAddress(getBalanceTestKey);
  }




  @Test(enabled = true, description = "State tree with eth_getBalance", groups = {"daily", "serial"})
  public void test01StateTreeWithEthGetBalance() throws Exception {
    PublicMethod.sendcoin(getBalanceTestAddress,sendAmount,
        PublicMethod.getFinalAddress(foundationKey001),foundationKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    final Long beforeBalance = sendAmount;
    final Long beforeBlockNumber = blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build())
        .getBlockHeader().getRawData().getNumber();
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    PublicMethod.sendcoin(PublicMethod.getFinalAddress(foundationKey001),transferAmount,
        getBalanceTestAddress,getBalanceTestKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    final Long afterBalance = PublicMethod.queryAccount(getBalanceTestAddress,blockingStubFull)
        .getBalance();
    Assert.assertEquals((long)(beforeBalance - afterBalance),(long)transferAmount);
    final Long afterBlockNumber = blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build())
        .getBlockHeader().getRawData().getNumber();




    //Assert before balance
    JsonArray params = new JsonArray();
    params.add("0x" + ByteArray.toHexString(getBalanceTestAddress).substring(2));
    params.add("0x" + Long.toHexString(beforeBlockNumber));
    JsonObject requestBody = getJsonRpcBody("eth_getBalance", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethod.parseResponseContent(response);
    String balance = responseContent.getString("result").substring(2);
    Long assertBalance = Long.parseLong(balance, 16);
    Assert.assertEquals(assertBalance,beforeBalance);





    //Assert after balance
    params = new JsonArray();
    params.add("0x" + ByteArray.toHexString(getBalanceTestAddress).substring(2));
    params.add("0x" + Long.toHexString(afterBlockNumber));
    requestBody = getJsonRpcBody("eth_getBalance", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethod.parseResponseContent(response);
    balance = responseContent.getString("result").substring(2);
    assertBalance = Long.parseLong(balance, 16);
    Assert.assertEquals(assertBalance,afterBalance);



    //State tree not open didn't support block number
    params = new JsonArray();
    params.add("0x" + ByteArray.toHexString(getBalanceTestAddress).substring(2));
    params.add("0x" + Long.toHexString(afterBlockNumber));
    requestBody = getJsonRpcBody("eth_getBalance", params);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethod.parseResponseContent(response);
    String wrongMessage = responseContent.getJSONObject("error").getString("message");
    Assert.assertEquals(wrongMessage,"QUANTITY not supported, just support TAG as latest");
  }


  @Test(enabled = true, description = "State tree with tron_getAssets", groups = {"daily", "serial"})
  public void test02StateTreeWithTronGetAssets() throws Exception {
    Assert.assertTrue(PublicMethod.transferAsset(getBalanceTestAddress, jsonRpcAssetId.getBytes(),sendAmount,
        jsonRpcOwnerAddress,jsonRpcOwnerKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    final Long beforeBalance = sendAmount;
    final Long beforeBlockNumber = blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build())
        .getBlockHeader().getRawData().getNumber();
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(PublicMethod.transferAsset(getBalanceTestAddress, jsonRpcAssetId.getBytes(),transferAmount,
        jsonRpcOwnerAddress,jsonRpcOwnerKey, blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    final Long afterBalance = sendAmount + transferAmount;
    final Long afterBlockNumber = blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build())
        .getBlockHeader().getRawData().getNumber();


    //Assert before trc10 balance
    JsonArray params = new JsonArray();
    params.add("0x" + ByteArray.toHexString(getBalanceTestAddress).substring(2));
    params.add("0x" + Long.toHexString(beforeBlockNumber));
    JsonObject requestBody = getJsonRpcBody("tron_getAssets", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethod.parseResponseContent(response);
    JSONArray jsonArray = responseContent.getJSONArray("result");
    Assert.assertEquals(jsonArray.size(),1);
    Long tokenId = Long.parseLong(jsonArray.getJSONObject(0).getString("key").substring(2),16);
    Long assertBalance = Long.parseLong(jsonArray.getJSONObject(0).getString("value").substring(2),16);
    Assert.assertEquals(assertBalance,beforeBalance);
    Assert.assertEquals(String.valueOf(tokenId),jsonRpcAssetId);


    //Assert after balance
    params = new JsonArray();
    params.add("0x" + ByteArray.toHexString(getBalanceTestAddress).substring(2));
    params.add("0x" + Long.toHexString(afterBlockNumber));
    requestBody = getJsonRpcBody("tron_getAssets", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethod.parseResponseContent(response);
    jsonArray = responseContent.getJSONArray("result");
    Assert.assertEquals(jsonArray.size(),1);
    tokenId = Long.parseLong(jsonArray.getJSONObject(0).getString("key").substring(2),16);
    assertBalance = Long.parseLong(jsonArray.getJSONObject(0).getString("value").substring(2),16);
    Assert.assertEquals(assertBalance,afterBalance);
    Assert.assertEquals(String.valueOf(tokenId),jsonRpcAssetId);


    //State tree not open didn't support block number
    params = new JsonArray();
    params.add("0x" + ByteArray.toHexString(getBalanceTestAddress).substring(2));
    params.add("0x" + Long.toHexString(afterBlockNumber));
    requestBody = getJsonRpcBody("tron_getAssets", params);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethod.parseResponseContent(response);
    String wrongMessage = responseContent.getJSONObject("error").getString("message");
    Assert.assertEquals(wrongMessage,"QUANTITY not supported, just support TAG as latest");






  }


  @Test(enabled = true, description = "State tree with tron_getAssetById", groups = {"daily", "serial"})
  public void test03StateTreeWithTronGetAssetById() throws Exception {
    ECKey getBalanceECKey = new ECKey(Utils.getRandom());
    byte[] getBalanceTestAddress = getBalanceECKey.getAddress();
    String getBalanceTestKey = ByteArray.toHexString(getBalanceECKey.getPrivKeyBytes());
    PublicMethod.printAddress(getBalanceTestKey);
    Assert.assertTrue(PublicMethod.transferAsset(getBalanceTestAddress, jsonRpcAssetId.getBytes(),sendAmount,
        jsonRpcOwnerAddress,jsonRpcOwnerKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    final Long beforeBalance = sendAmount;
    final Long beforeBlockNumber = blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build())
        .getBlockHeader().getRawData().getNumber();
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(PublicMethod.transferAsset(getBalanceTestAddress, jsonRpcAssetId.getBytes(),transferAmount,
        jsonRpcOwnerAddress,jsonRpcOwnerKey, blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    final Long afterBalance = sendAmount + transferAmount;
    final Long afterBlockNumber = blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build())
        .getBlockHeader().getRawData().getNumber();


    //Assert before trc10 balance
    JsonArray params = new JsonArray();
    params.add("0x" + ByteArray.toHexString(getBalanceTestAddress).substring(2));
    params.add("0x" + Long.toHexString(Long.valueOf(jsonRpcAssetId)));
    params.add("0x" + Long.toHexString(beforeBlockNumber));
    JsonObject requestBody = getJsonRpcBody("tron_getAssetById", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethod.parseResponseContent(response);
    Long tokenId = Long.parseLong(responseContent.getJSONObject("result").getString("key").substring(2),16);
    Long assertBalance = Long.parseLong(responseContent.getJSONObject("result").getString("value").substring(2),16);
    Assert.assertEquals(assertBalance,beforeBalance);
    Assert.assertEquals(String.valueOf(tokenId),jsonRpcAssetId);


    //Assert after balance
    params = new JsonArray();
    params.add("0x" + ByteArray.toHexString(getBalanceTestAddress).substring(2));
    params.add("0x" + Long.toHexString(Long.valueOf(jsonRpcAssetId)));
    params.add("0x" + Long.toHexString(afterBlockNumber));
    requestBody = getJsonRpcBody("tron_getAssetById", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethod.parseResponseContent(response);
    tokenId = Long.parseLong(responseContent.getJSONObject("result").getString("key").substring(2),16);
    assertBalance = Long.parseLong(responseContent.getJSONObject("result").getString("value").substring(2),16);

    Assert.assertEquals(assertBalance,afterBalance);
    Assert.assertEquals(String.valueOf(tokenId),jsonRpcAssetId);


    //State tree not open didn't support block number
    params = new JsonArray();
    params.add("0x" + ByteArray.toHexString(getBalanceTestAddress).substring(2));
    params.add("0x" + Long.toHexString(Long.valueOf(jsonRpcAssetId)));
    params.add("0x" + Long.toHexString(afterBlockNumber));
    requestBody = getJsonRpcBody("tron_getAssetById", params);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethod.parseResponseContent(response);
    String wrongMessage = responseContent.getJSONObject("error").getString("message");
    Assert.assertEquals(wrongMessage,"QUANTITY not supported, just support TAG as latest");






  }


  @Test(enabled = true, description = "State tree with eth_call", groups = {"daily", "serial"})
  public void test04StateTreeWithEthCall() throws Exception {
    String selector = "transfer(address,uint256)";
    String addressParam =
        "000000000000000000000000"
            + ByteArray.toHexString(getBalanceTestAddress).substring(2); // [0,3)

    String transferValueParam = "0000000000000000000000000000000000000000000000000000000000000100";
    String paramString = addressParam + transferValueParam;
    String trc20Txid01 =
        PublicMethod.triggerContract(
            ByteArray.fromHexString(trc20AddressHex),
            selector,
            paramString,
            true,
            0,
            maxFeeLimit,
            "0",
            0,
            jsonRpcOwnerAddress,
            jsonRpcOwnerKey,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethod.getTransactionInfoById(trc20Txid01,blockingStubFull).get()
        .getLogCount() == 1);


    final Long beforeBalance = Long.parseLong("100",16);
    final Long beforeBlockNumber = blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build())
        .getBlockHeader().getRawData().getNumber();
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    trc20Txid01 =
        PublicMethod.triggerContract(
            ByteArray.fromHexString(trc20AddressHex),
            selector,
            paramString,
            true,
            0,
            maxFeeLimit,
            "0",
            0,
            jsonRpcOwnerAddress,
            jsonRpcOwnerKey,
            blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethod.getTransactionInfoById(trc20Txid01,blockingStubFull).get()
        .getLogCount() == 1);

    final Long afterBalance = beforeBalance + beforeBalance;
    final Long afterBlockNumber = blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build())
        .getBlockHeader().getRawData().getNumber();


    //Assert before trc20 balance
    JsonObject param = new JsonObject();
    HttpMethod.waitToProduceOneBlock(httpFullNode);
    param.addProperty("from", ByteArray.toHexString(getBalanceTestAddress));
    param.addProperty("to", trc20AddressHex);
    param.addProperty("gas", "0x0");
    param.addProperty("gasPrice", "0x0");
    param.addProperty("value", "0x0");
    //balanceOf(address) keccak encode
    param.addProperty("data", "0x70a08231" + addressParam);


    JsonArray params = new JsonArray();
    params.add(param);
    params.add("0x" + Long.toHexString(beforeBlockNumber));

    JsonObject requestBody = getJsonRpcBody("eth_call", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethod.parseResponseContent(response);
    String balance = responseContent.getString("result").substring(2);
    Long assertBalance = Long.parseLong(balance, 16);
    Assert.assertEquals(assertBalance,beforeBalance);


    //Assert after balance
    params = new JsonArray();
    params.add(param);
    params.add("0x" + Long.toHexString(afterBlockNumber));
    requestBody = getJsonRpcBody("eth_call", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethod.parseResponseContent(response);
    balance = responseContent.getString("result").substring(2);
    assertBalance = Long.parseLong(balance, 16);
    Assert.assertEquals(assertBalance,afterBalance);



    //State tree not open didn't support block number
    params = new JsonArray();
    params.add(param);
    params.add("0x" + Long.toHexString(afterBlockNumber));
    requestBody = getJsonRpcBody("eth_call", params);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethod.parseResponseContent(response);
    String wrongMessage = responseContent.getJSONObject("error").getString("message");
    Assert.assertEquals(wrongMessage,"QUANTITY not supported, just support TAG as latest");
  }


  @Test(enabled = true, description = "State tree with eth_getCode", groups = {"daily", "serial"})
  public void test05StateTreeWithEthGetCode() throws Exception {
    String getCodeFromGetContract = ByteArray
        .toHexString(PublicMethod.getContract(selfDestructAddressByte,blockingStubFull)
            .getBytecode().toByteArray());

    logger.info("Get contract bytecode: " + getCodeFromGetContract);

    JsonArray params = new JsonArray();
    params.add(ByteArray.toHexString(selfDestructAddressByte));
    params.add("latest");

    JsonObject requestBody = getJsonRpcBody("eth_getCode", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethod.parseResponseContent(response);
    String getCodeFromLatest= responseContent.getString("result").substring(2);
    logger.info("Latest getCode:" + getCodeFromLatest);

    //Assert.assertEquals(getCodeFromJsonRpc,getCodeFromGetContract);



    final Long beforeBlockNumber = blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build())
        .getBlockHeader().getRawData().getNumber();
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    String txid02 =
        PublicMethod.triggerContract(
            selfDestructAddressByte,
            "kill()",
            "#",
            false,
            0,
            maxFeeLimit,
            "0",
            0,
            jsonRpcOwnerAddress,
            jsonRpcOwnerKey,
            blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertEquals(PublicMethod.getTransactionInfoById(txid02,blockingStubFull).get()
        .getReceipt().getResult(), contractResult.SUCCESS);
    final Long afterBlockNumber = blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build())
        .getBlockHeader().getRawData().getNumber();


    //Assert before selfDestruct eth_getCode
    params = new JsonArray();
    params.add(ByteArray.toHexString(selfDestructAddressByte));
    params.add("0x" + Long.toHexString(beforeBlockNumber));

    requestBody = getJsonRpcBody("eth_getCode", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethod.parseResponseContent(response);
    String getCodeFromBeforeKill = responseContent.getString("result").substring(2);
    logger.info("Before kill : " + getCodeFromBeforeKill);

    Assert.assertEquals(getCodeFromBeforeKill,getCodeFromLatest);


    //Assert after self destruct
    params = new JsonArray();
    params.add(ByteArray.toHexString(selfDestructAddressByte));
    params.add("0x" + Long.toHexString(afterBlockNumber));
    requestBody = getJsonRpcBody("eth_getCode", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethod.parseResponseContent(response);
    String getCodeFromAfterKill = responseContent.getString("result");
    logger.info("After kill : " + getCodeFromAfterKill);
    Assert.assertEquals(getCodeFromAfterKill,"0x");



    //State tree not open didn't support block number
    params = new JsonArray();
    params.add(ByteArray.toHexString(selfDestructAddressByte));
    params.add("0x" + Long.toHexString(beforeBlockNumber));
    requestBody = getJsonRpcBody("eth_getCode", params);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethod.parseResponseContent(response);
    String wrongMessage = responseContent.getJSONObject("error").getString("message");
    Assert.assertEquals(wrongMessage,"QUANTITY not supported, just support TAG as latest");
  }

  @Test(enabled = true, description = "State tree with eth_getStorageAt", groups = {"daily", "serial"})
  public void test06StateTreeWithEthGetStorageAt() throws Exception {
    JsonArray params = new JsonArray();
    params.add(contractAddressFrom58);
    params.add("0x2");
    params.add("latest");
    JsonObject requestBody = getJsonRpcBody("eth_getStorageAt", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethod.parseResponseContent(response);
    String result = responseContent.getString("result").substring(2);
    long beforePos2 = Long.parseLong(result, 16);
    logger.info("beforePos2:" + beforePos2);
    final Long beforeBlockNumber = blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build())
        .getBlockHeader().getRawData().getNumber();

    String txid00 =
        PublicMethod.triggerContract(
            ByteArray.fromHexString(contractAddressFrom58),
            "changePos2()",
            "#",
            false,
            0,
            maxFeeLimit,
            "0",
            0,
            jsonRpcOwnerAddress,
            jsonRpcOwnerKey,
            blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertEquals(PublicMethod.getTransactionInfoById(txid00,blockingStubFull).get()
        .getReceipt().getResult(), contractResult.SUCCESS);

    final Long afterBlockNumber = blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build())
        .getBlockHeader().getRawData().getNumber();


    //Assert before pos2 eth_getStorageAt
    params = new JsonArray();
    params.add(contractAddressFrom58);
    params.add("0x2");
    params.add("0x" + Long.toHexString(beforeBlockNumber));

    requestBody = getJsonRpcBody("eth_getStorageAt", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethod.parseResponseContent(response);
    Long beforeNumberEthGetStorageAt = Long.parseLong(responseContent.getString("result").substring(2),16);
    logger.info("Before change pos2 : " + beforeNumberEthGetStorageAt);

    Assert.assertEquals((long)beforeNumberEthGetStorageAt,beforePos2);


    //Assert after change pos2
    params = new JsonArray();
    params.add(contractAddressFrom58);
    params.add("0x2");
    params.add("0x" + Long.toHexString(afterBlockNumber));
    requestBody = getJsonRpcBody("eth_getStorageAt", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethod.parseResponseContent(response);
    Long afterNumberEthGetStorageAt = Long.parseLong(responseContent.getString("result").substring(2),16);

    Assert.assertEquals((long)afterNumberEthGetStorageAt,2);



    //State tree not open didn't support block number
    params = new JsonArray();
    params.add(contractAddressFrom58);
    params.add("0x2");
    params.add("0x" + Long.toHexString(afterBlockNumber));
    requestBody = getJsonRpcBody("eth_getStorageAt", params);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethod.parseResponseContent(response);
    String wrongMessage = responseContent.getJSONObject("error").getString("message");
    Assert.assertEquals(wrongMessage,"QUANTITY not supported, just support TAG as latest");
  }


  @Test(enabled = false, description = "eth_getStorageAt with create2 address."
      + "this case has been moved to Accounts001.java", groups = {"daily", "serial"})
  public void test07StateTreeWithEthGetStorageAt() {
    JsonArray params = new JsonArray();
    params.add(create2AddressFrom41);
    params.add("0x2");
    params.add("latest");
    JsonObject requestBody = getJsonRpcBody("eth_getStorageAt", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethod.parseResponseContent(response);
    String result = responseContent.getString("result").substring(2);
    long beforePos2 = Long.parseLong(result, 16);
    logger.info("beforePos2:" + beforePos2);
    final Long beforeBlockNumber = blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build())
        .getBlockHeader().getRawData().getNumber();

    String txid01 =
        PublicMethod.triggerContract(
            ByteArray.fromHexString(create2AddressFrom41),
            "changePos2()",
            "#",
            false,
            0,
            maxFeeLimit,
            "0",
            0,
            jsonRpcOwnerAddress,
            jsonRpcOwnerKey,
            blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertEquals(PublicMethod.getTransactionInfoById(txid01,blockingStubFull).get()
        .getReceipt().getResult(), contractResult.SUCCESS);

    final Long afterBlockNumber = blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build())
        .getBlockHeader().getRawData().getNumber();


    //Assert before pos2 eth_getStorageAt
    params = new JsonArray();
    params.add(create2AddressFrom41);
    params.add("0x2");
    params.add("0x" + Long.toHexString(beforeBlockNumber));

    requestBody = getJsonRpcBody("eth_getStorageAt", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethod.parseResponseContent(response);
    Long beforeNumberEthGetStorageAt = Long.parseLong(responseContent.getString("result").substring(2),16);
    logger.info("Before change pos2 : " + beforeNumberEthGetStorageAt);

    Assert.assertEquals((long)beforeNumberEthGetStorageAt,beforePos2);


    //Assert after change pos2
    params = new JsonArray();
    params.add(create2AddressFrom41);
    params.add("0x2");
    params.add("0x" + Long.toHexString(afterBlockNumber));
    requestBody = getJsonRpcBody("eth_getStorageAt", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethod.parseResponseContent(response);
    Long afterNumberEthGetStorageAt = Long.parseLong(responseContent.getString("result").substring(2),16);

    Assert.assertEquals((long)afterNumberEthGetStorageAt,2);



    //State tree not open didn't support block number
    params = new JsonArray();
    params.add(create2AddressFrom41);
    params.add("0x2");
    params.add("0x" + Long.toHexString(afterBlockNumber));
    requestBody = getJsonRpcBody("eth_getStorageAt", params);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethod.parseResponseContent(response);
    String wrongMessage = responseContent.getJSONObject("error").getString("message");
    Assert.assertEquals(wrongMessage,"QUANTITY not supported, just support TAG as latest");
  }





  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

}
