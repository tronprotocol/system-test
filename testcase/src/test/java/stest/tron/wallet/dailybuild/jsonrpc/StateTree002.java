package stest.tron.wallet.dailybuild.jsonrpc;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import org.tron.protos.contract.SmartContractOuterClass;
import stest.tron.wallet.common.client.utils.*;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;


@Slf4j

public class StateTree002 extends JsonRpcBase {
  private JSONObject responseContent;
  private HttpResponse response;

  ECKey callerECKey = new ECKey(Utils.getRandom());
  byte[] callerAddress = callerECKey.getAddress();
  String callerPrivKey = ByteArray.toHexString(callerECKey.getPrivKeyBytes());

  byte[] callerContract;
  byte[] calledContract;
  byte[] cContract;
  Long afterBlockNumber01;
  Long afterBlockNumber02;
  Long afterBlockNumber03;
  Long afterBlockNumber04;


  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    PublicMethed.printAddress(callerPrivKey);
    Assert.assertTrue(PublicMethed.sendcoin(callerAddress, 10000000000L, foundationAccountAddress,
        foundationAccountKey, blockingStubFull));
//    jsonRpcAssetId = "1000001";
    Assert.assertTrue(PublicMethed.transferAsset(callerAddress, jsonRpcAssetId.getBytes(), 300L,
        jsonRpcOwnerAddress,jsonRpcOwnerKey, blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String filePath = "./src/test/resources/soliditycode/stateTree01.sol";
    String contractName = "callerContract";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    callerContract = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        100L, 100, 1000L, jsonRpcAssetId,
        100,null, callerPrivKey, callerAddress, blockingStubFull);

    contractName = "calledContract";
    retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    calledContract = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        100L, 100, 1000L, jsonRpcAssetId,
        100, null, callerPrivKey, callerAddress, blockingStubFull);

    contractName = "c";
    retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    cContract = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, 1000L, jsonRpcAssetId,
        100, null, callerPrivKey, callerAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethed.getContract(callerContract,
        blockingStubFull);
    Assert.assertTrue(smartContract.hasAbi());

    smartContract = PublicMethed.getContract(calledContract, blockingStubFull);
    Assert.assertTrue(smartContract.hasAbi());

    smartContract = PublicMethed.getContract(cContract, blockingStubFull);
    Assert.assertTrue(smartContract.hasAbi());
  }


  @Test(enabled = true, description = "eth_call get trx balance after contract delegate call ")
  public void test01StateTreeWithEthCall() {

    String parmes = "\"" + Base58.encode58Check(calledContract)
        + "\",\"" + Base58.encode58Check(cContract) + "\"";
    String method = "sendToB(address,address)";
    String txid = PublicMethed.triggerContract(callerContract, method, parmes, false,
        0, maxFeeLimit, callerAddress, callerPrivKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Protocol.TransactionInfo infoById =
        PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("Trigger InfobyId: " + infoById);
    Assert.assertEquals(Protocol.TransactionInfo.code.SUCESS, infoById.getResult());
    Assert.assertEquals(contractResult.SUCCESS, infoById.getReceipt().getResult());

    afterBlockNumber01 = blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build())
        .getBlockHeader().getRawData().getNumber();

    checkResult01();
  }


  @Test(enabled = true, description = "eth_call get trx balance after contract call ")
  public void test02tateTreeWithEthCall() {
    String parmes = "\"" + Base58.encode58Check(calledContract)
        + "\",\"" + Base58.encode58Check(cContract) + "\"";
    String method = "sendToB2(address,address)";
    String txid = PublicMethed.triggerContract(callerContract, method, parmes, false,
        0, maxFeeLimit, callerAddress, callerPrivKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Protocol.TransactionInfo infoById =
        PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("Trigger InfobyId: " + infoById);
    Assert.assertEquals(Protocol.TransactionInfo.code.SUCESS, infoById.getResult());
    Assert.assertEquals(contractResult.SUCCESS, infoById.getReceipt().getResult());

    afterBlockNumber02 = blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build())
        .getBlockHeader().getRawData().getNumber();
    checkResult02();
    checkResult01();
  }

  @Test(enabled = true, description = "eth_call get trc10 balance after contract delegate call ")
  public void test03StateTreeWithEthCall() {
    String method = "transferAssetIndelegateCall(address,address,address,uint256,trcToken)";
    String param =
        "\"" + Base58.encode58Check(calledContract) + "\",\"" + Base58.encode58Check(cContract)
            + "\",\"" + Base58.encode58Check(callerContract)
            + "\",1,\"" + jsonRpcAssetId + "\"";

    String txid = PublicMethed.triggerContract(callerContract, method,
        param, false, 0, 1000000000L, "0",
        0, callerAddress, callerPrivKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Protocol.TransactionInfo infoById =
        PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("Trigger InfobyId: " + infoById);
    Assert.assertEquals(Protocol.TransactionInfo.code.SUCESS, infoById.getResult());
    Assert.assertEquals(contractResult.SUCCESS, infoById.getReceipt().getResult());

    afterBlockNumber03 = blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build())
        .getBlockHeader().getRawData().getNumber();
    checkResult03();
    checkResult02();
  }

  @Test(enabled = true, description = "eth_call get trc10 balance after contract call ")
  public void test04StateTreeWithEthCall() {
    String method = "transferAssetInCall(address,address,address,uint256,trcToken)";
    String param =
        "\"" + Base58.encode58Check(calledContract) + "\",\"" + Base58.encode58Check(cContract)
            + "\",\"" + Base58.encode58Check(callerContract)
            + "\",1,\"" + jsonRpcAssetId + "\"";

    String txid = PublicMethed.triggerContract(callerContract, method,
        param, false, 0, 1000000000L, "0",
        0, callerAddress, callerPrivKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Protocol.TransactionInfo infoById =
        PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("Trigger InfobyId: " + infoById);
    Assert.assertEquals(Protocol.TransactionInfo.code.SUCESS, infoById.getResult());
    Assert.assertEquals(contractResult.SUCCESS, infoById.getReceipt().getResult());

    afterBlockNumber04 = blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build())
        .getBlockHeader().getRawData().getNumber();
    checkResult04();
    checkResult03();
  }



  void checkResult01() {
    //eth_call  get callerContract balance
    String addressParam = "000000000000000000000000"
            + ByteArray.toHexString(callerContract).substring(2);
    JsonObject param = new JsonObject();
    param.addProperty("from", ByteArray.toHexString(callerAddress));
    param.addProperty("to", ByteArray.toHexString(callerContract));
    param.addProperty("gas", "0x0");
    param.addProperty("gasPrice", "0x0");
    param.addProperty("value", "0x0");
    //getBalance(address)keccak encode
    param.addProperty("data", "0xf8b2cb4f" + addressParam);
    JsonArray params = new JsonArray();
    params.add(param);
    params.add("0x" + Long.toHexString(afterBlockNumber01));

    JsonObject requestBody = getJsonRpcBody("eth_call", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String balance = responseContent.getString("result").substring(2);
    Long callerContractBalance1 = Long.parseLong(balance, 16);
    Assert.assertEquals(95, callerContractBalance1.longValue());

    //eth_call  get calledContract balance
    addressParam = "000000000000000000000000"
            + ByteArray.toHexString(cContract).substring(2);
    param.addProperty("data", "0xf8b2cb4f" + addressParam);
    requestBody = getJsonRpcBody("eth_call", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    balance = responseContent.getString("result").substring(2);
    Long cContractBalance1 = Long.parseLong(balance, 16);
    Assert.assertEquals(5, cContractBalance1.longValue());

    //eth_call get callerContract balance with jsonObject blockNumber
    addressParam = "000000000000000000000000"
            + ByteArray.toHexString(callerContract).substring(2);
    param.addProperty("data", "0xf8b2cb4f" + addressParam);
    params.remove(1);
    JsonObject blockNumAndHash = new JsonObject();
    blockNumAndHash.addProperty("blockNumber",String.valueOf(afterBlockNumber01));
    params.add(blockNumAndHash);
    requestBody = getJsonRpcBody("eth_call", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    balance = responseContent.getString("result").substring(2);
    callerContractBalance1 = Long.parseLong(balance, 16);
    Assert.assertEquals(95, callerContractBalance1.longValue());

    //eth_call get cContract balance with jsonObject blockNumber
    addressParam = "000000000000000000000000"
        + ByteArray.toHexString(cContract).substring(2);
    param.addProperty("data", "0xf8b2cb4f" + addressParam);
    requestBody = getJsonRpcBody("eth_call", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    balance = responseContent.getString("result").substring(2);
    cContractBalance1 = Long.parseLong(balance, 16);
    Assert.assertEquals(5, cContractBalance1.longValue());


    //eth_getBalance callerContract
    params = new JsonArray();
    params.add("0x" + ByteArray.toHexString(callerContract).substring(2));
    params.add("0x" + Long.toHexString(afterBlockNumber01));
    requestBody  = getJsonRpcBody("eth_getBalance", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    balance = responseContent.getString("result").substring(2);
    Long callerContractBalance2 = Long.parseLong(balance, 16);
    Assert.assertEquals(callerContractBalance1,callerContractBalance2);

    //eth_getBalance crContract
    params = new JsonArray();
    params.add("0x" + ByteArray.toHexString(cContract).substring(2));
    params.add("0x" + Long.toHexString(afterBlockNumber01));
    requestBody  = getJsonRpcBody("eth_getBalance", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    balance = responseContent.getString("result").substring(2);
    Long cContractBalance2 = Long.parseLong(balance, 16);
    Assert.assertEquals(cContractBalance1,cContractBalance2);
  }

  void checkResult02() {
    //eth_call  get calledContract balance
    String addressParam =
        "000000000000000000000000"
            + ByteArray.toHexString(calledContract).substring(2);
    JsonObject param = new JsonObject();
    param.addProperty("from", ByteArray.toHexString(callerAddress));
    param.addProperty("to", ByteArray.toHexString(callerContract));
    param.addProperty("gas", "0x0");
    param.addProperty("gasPrice", "0x0");
    param.addProperty("value", "0x0");
    //balanceOf(address) keccak encode
    param.addProperty("data", "0xf8b2cb4f" + addressParam);
    JsonArray params = new JsonArray();
    params.add(param);
    params.add("0x" + Long.toHexString(afterBlockNumber02));

    JsonObject requestBody = getJsonRpcBody("eth_call", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    System.out.println("-------: "+responseContent.toJSONString());
    String balance = responseContent.getString("result").substring(2);
    Long calledContractBalance1 = Long.parseLong(balance, 16);
    Assert.assertEquals(95, calledContractBalance1.longValue());

    //eth_call  get cContract balance
    addressParam =
        "000000000000000000000000"
            + ByteArray.toHexString(cContract).substring(2);
    param.addProperty("data", "0xf8b2cb4f" + addressParam);
    requestBody = getJsonRpcBody("eth_call", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    balance = responseContent.getString("result").substring(2);
    Long cContractBalance1 = Long.parseLong(balance, 16);
    Assert.assertEquals(10, cContractBalance1.longValue());

    //eth_getBalance calledContract
    params = new JsonArray();
    params.add("0x" + ByteArray.toHexString(calledContract).substring(2));
    params.add("0x" + Long.toHexString(afterBlockNumber02));
    requestBody  = getJsonRpcBody("eth_getBalance", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    balance = responseContent.getString("result").substring(2);
    Long calledContractBalance2 = Long.parseLong(balance, 16);
    Assert.assertEquals(calledContractBalance1,calledContractBalance2);

    //eth_getBalance cContract
    params = new JsonArray();
    params.add("0x" + ByteArray.toHexString(cContract).substring(2));
    params.add("0x" + Long.toHexString(afterBlockNumber02));
    requestBody  = getJsonRpcBody("eth_getBalance", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    balance = responseContent.getString("result").substring(2);
    Long cContractBalance2 = Long.parseLong(balance, 16);
    Assert.assertEquals(cContractBalance1,cContractBalance2);
  }

  void checkResult03() {
    //get callerContract token balance by tron_getAssetById
    JsonArray params = new JsonArray();
    params.add("0x" + ByteArray.toHexString(callerContract).substring(2));
    params.add("0x" + Long.toHexString(Long.valueOf(jsonRpcAssetId)));
    params.add("0x" + Long.toHexString(afterBlockNumber03));
    JsonObject requestBody = getJsonRpcBody("tron_getAssetById", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    Long tokenId = Long.parseLong(responseContent.getJSONObject("result").getString("key").substring(2),16);
    Long callerContractTokenBalance01 = Long.parseLong(responseContent.getJSONObject("result").getString("value").substring(2),16);
    Assert.assertEquals(101, callerContractTokenBalance01.longValue());
    Assert.assertEquals(String.valueOf(tokenId),jsonRpcAssetId);

    //get cContract token balance by tron_getAssetById
    params = new JsonArray();
    params.add("0x" + ByteArray.toHexString(cContract).substring(2));
    params.add("0x" + Long.toHexString(Long.valueOf(jsonRpcAssetId)));
    params.add("0x" + Long.toHexString(afterBlockNumber03));
    requestBody = getJsonRpcBody("tron_getAssetById", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    tokenId = Long.parseLong(responseContent.getJSONObject("result").getString("key").substring(2),16);
    Long cTokenBalance01 = Long.parseLong(responseContent.getJSONObject("result").getString("value").substring(2),16);
    Assert.assertEquals(99, cTokenBalance01.longValue());
    Assert.assertEquals(String.valueOf(tokenId),jsonRpcAssetId);

    //get callerContract token balance by eth_call
    String addressParam = "000000000000000000000000"
        + ByteArray.toHexString(callerContract).substring(2)
        + "00000000000000000000000000000000000000000000000000000000000"
        + Long.toHexString(Long.valueOf(jsonRpcAssetId));
    JsonObject param = new JsonObject();
    param.addProperty("from", ByteArray.toHexString(callerAddress));
    param.addProperty("to", ByteArray.toHexString(callerContract));
    param.addProperty("gas", "0x0");
    param.addProperty("gasPrice", "0x0");
    param.addProperty("value", "0x0");
    //getTokenBalance(address,trcToken) keccak encode
    param.addProperty("data", "0x5cae14a7" + addressParam);
    params = new JsonArray();
    params.add(param);
    params.add("0x" + Long.toHexString(afterBlockNumber03));

    requestBody = getJsonRpcBody("eth_call", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String balance = responseContent.getString("result").substring(2);
    Long callerContractTokenBalance02 = Long.parseLong(balance, 16);
    Assert.assertEquals(callerContractTokenBalance01.longValue(), callerContractTokenBalance02.longValue());

    //get cContract token balance by eth_call
    addressParam = "000000000000000000000000"
        + ByteArray.toHexString(cContract).substring(2)
        + "00000000000000000000000000000000000000000000000000000000000"
        + Long.toHexString(Long.valueOf(jsonRpcAssetId));
    param.addProperty("data", "0x5cae14a7" + addressParam);
    requestBody = getJsonRpcBody("eth_call", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    balance = responseContent.getString("result").substring(2);
    Long cTokenBalance02 = Long.parseLong(balance, 16);
    Assert.assertEquals(cTokenBalance01.longValue(), cTokenBalance02.longValue());

    //eth_call get callerContract token balance with jsonObject blockNumber
    addressParam = "000000000000000000000000"
        + ByteArray.toHexString(callerContract).substring(2)
        + "00000000000000000000000000000000000000000000000000000000000"
        + Long.toHexString(Long.valueOf(jsonRpcAssetId));
    param.addProperty("data", "0x5cae14a7" + addressParam);
    params.remove(1);
    JsonObject blockNumAndHash = new JsonObject();
    blockNumAndHash.addProperty("blockNumber",String.valueOf(afterBlockNumber03));
    params.add(blockNumAndHash);
    requestBody = getJsonRpcBody("eth_call", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    balance = responseContent.getString("result").substring(2);
    Long callerContractTokenBalance03 = Long.parseLong(balance, 16);
    Assert.assertEquals(callerContractTokenBalance01, callerContractTokenBalance03);

    //eth_call get cContract balance with jsonObject blockNumber
    addressParam = "000000000000000000000000"
        + ByteArray.toHexString(cContract).substring(2)
        + "00000000000000000000000000000000000000000000000000000000000"
        + Long.toHexString(Long.valueOf(jsonRpcAssetId));
    param.addProperty("data", "0x5cae14a7" + addressParam);
    requestBody = getJsonRpcBody("eth_call", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    balance = responseContent.getString("result").substring(2);
    Long cTokenBalance03 = Long.parseLong(balance, 16);
    Assert.assertEquals(cTokenBalance01, cTokenBalance03);
  }

  void checkResult04() {
    //get callerContract token balance by tron_getAssetById
    JsonArray params = new JsonArray();
    params.add("0x" + ByteArray.toHexString(callerContract).substring(2));
    params.add("0x" + Long.toHexString(Long.valueOf(jsonRpcAssetId)));
    params.add("0x" + Long.toHexString(afterBlockNumber04));
    JsonObject requestBody = getJsonRpcBody("tron_getAssetById", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    Long tokenId = Long.parseLong(responseContent.getJSONObject("result").getString("key").substring(2),16);
    Long callerContractTokenBalance01 = Long.parseLong(responseContent.getJSONObject("result").getString("value").substring(2),16);
    Assert.assertEquals(102, callerContractTokenBalance01.longValue());
    Assert.assertEquals(String.valueOf(tokenId),jsonRpcAssetId);

    //get cContract token balance by tron_getAssetById
    params = new JsonArray();
    params.add("0x" + ByteArray.toHexString(cContract).substring(2));
    params.add("0x" + Long.toHexString(Long.valueOf(jsonRpcAssetId)));
    params.add("0x" + Long.toHexString(afterBlockNumber04));
    requestBody = getJsonRpcBody("tron_getAssetById", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    tokenId = Long.parseLong(responseContent.getJSONObject("result").getString("key").substring(2),16);
    Long cTokenBalance01 = Long.parseLong(responseContent.getJSONObject("result").getString("value").substring(2),16);
    Assert.assertEquals(98, cTokenBalance01.longValue());
    Assert.assertEquals(String.valueOf(tokenId),jsonRpcAssetId);

    //get callerContract token balance by eth_call
    String addressParam = "000000000000000000000000"
        + ByteArray.toHexString(callerContract).substring(2)
        + "00000000000000000000000000000000000000000000000000000000000"
        + Long.toHexString(Long.valueOf(jsonRpcAssetId));
    JsonObject param = new JsonObject();
    param.addProperty("from", ByteArray.toHexString(callerAddress));
    param.addProperty("to", ByteArray.toHexString(callerContract));
    param.addProperty("gas", "0x0");
    param.addProperty("gasPrice", "0x0");
    param.addProperty("value", "0x0");
    //getTokenBalance(address,trcToken) keccak encode
    param.addProperty("data", "0x5cae14a7" + addressParam);
    params = new JsonArray();
    params.add(param);
    params.add("0x" + Long.toHexString(afterBlockNumber04));

    requestBody = getJsonRpcBody("eth_call", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String balance = responseContent.getString("result").substring(2);
    Long callerContractTokenBalance02 = Long.parseLong(balance, 16);
    Assert.assertEquals(callerContractTokenBalance01.longValue(), callerContractTokenBalance02.longValue());

    //get cContract token balance by eth_call
    addressParam = "000000000000000000000000"
        + ByteArray.toHexString(cContract).substring(2)
        + "00000000000000000000000000000000000000000000000000000000000"
        + Long.toHexString(Long.valueOf(jsonRpcAssetId));
    param.addProperty("data", "0x5cae14a7" + addressParam);
    requestBody = getJsonRpcBody("eth_call", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    balance = responseContent.getString("result").substring(2);
    Long cTokenBalance02 = Long.parseLong(balance, 16);
    Assert.assertEquals(cTokenBalance01.longValue(), cTokenBalance02.longValue());

    //eth_call get callerContract token balance with jsonObject blockNumber
    addressParam = "000000000000000000000000"
        + ByteArray.toHexString(callerContract).substring(2)
        + "00000000000000000000000000000000000000000000000000000000000"
        + Long.toHexString(Long.valueOf(jsonRpcAssetId));
    param.addProperty("data", "0x5cae14a7" + addressParam);
    params.remove(1);
    JsonObject blockNumAndHash = new JsonObject();
    blockNumAndHash.addProperty("blockNumber",String.valueOf(afterBlockNumber04));
    params.add(blockNumAndHash);
    requestBody = getJsonRpcBody("eth_call", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    balance = responseContent.getString("result").substring(2);
    Long callerContractTokenBalance03 = Long.parseLong(balance, 16);
    Assert.assertEquals(callerContractTokenBalance01, callerContractTokenBalance03);

    //eth_call get cContract balance with jsonObject blockNumber
    addressParam = "000000000000000000000000"
        + ByteArray.toHexString(cContract).substring(2)
        + "00000000000000000000000000000000000000000000000000000000000"
        + Long.toHexString(Long.valueOf(jsonRpcAssetId));
    param.addProperty("data", "0x5cae14a7" + addressParam);
    requestBody = getJsonRpcBody("eth_call", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    balance = responseContent.getString("result").substring(2);
    Long cTokenBalance03 = Long.parseLong(balance, 16);
    Assert.assertEquals(cTokenBalance01, cTokenBalance03);
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethed.freedResource(callerAddress, callerPrivKey, foundationAccountAddress, blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

  }

}
