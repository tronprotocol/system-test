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
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import org.tron.protos.contract.SmartContractOuterClass;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.*;

import java.util.HashMap;
import java.util.Optional;
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

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String filePath = "./src/test/resources/soliditycode/contractTrcToken078.sol";
    String contractName = "callerContract";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    callerContract = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        100L, 100, null, callerPrivKey,
        callerAddress, blockingStubFull);

    contractName = "calledContract";
    retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    calledContract = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        100L, 100, null, callerPrivKey,
        callerAddress, blockingStubFull);

    contractName = "c";
    retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    cContract = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, callerPrivKey,
        callerAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethed.getContract(callerContract,
        blockingStubFull);
    Assert.assertTrue(smartContract.hasAbi());

    smartContract = PublicMethed.getContract(calledContract, blockingStubFull);
    Assert.assertTrue(smartContract.hasAbi());

    smartContract = PublicMethed.getContract(cContract, blockingStubFull);
    Assert.assertTrue(smartContract.hasAbi());
  }



  @Test(enabled = true, description = "State tree with eth_call after contract delegate call ")
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

    checkResult();
  }


  @Test(enabled = true, description = "State tree with eth_call after contract call ")
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

    final Long afterBlockNumber = blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build())
        .getBlockHeader().getRawData().getNumber();

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
    params.add("0x" + Long.toHexString(afterBlockNumber));

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
    params.add("0x" + Long.toHexString(afterBlockNumber));
    requestBody  = getJsonRpcBody("eth_getBalance", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    balance = responseContent.getString("result").substring(2);
    Long calledContractBalance2 = Long.parseLong(balance, 16);
    Assert.assertEquals(calledContractBalance1,calledContractBalance2);

    //eth_getBalance cContract
    params = new JsonArray();
    params.add("0x" + ByteArray.toHexString(cContract).substring(2));
    params.add("0x" + Long.toHexString(afterBlockNumber));
    requestBody  = getJsonRpcBody("eth_getBalance", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    balance = responseContent.getString("result").substring(2);
    Long cContractBalance2 = Long.parseLong(balance, 16);
    Assert.assertEquals(cContractBalance1,cContractBalance2);

    checkResult();
  }



  void checkResult() {
    //eth_call  get callerContract balance
    String addressParam =
        "000000000000000000000000"
            + ByteArray.toHexString(callerContract).substring(2);
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
    params.add("0x" + Long.toHexString(afterBlockNumber01));

    JsonObject requestBody = getJsonRpcBody("eth_call", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String balance = responseContent.getString("result").substring(2);
    Long callerContractBalance1 = Long.parseLong(balance, 16);
    Assert.assertEquals(95, callerContractBalance1.longValue());

    //eth_call  get calledContract balance
    addressParam =
        "000000000000000000000000"
            + ByteArray.toHexString(cContract).substring(2);
    param.addProperty("data", "0xf8b2cb4f" + addressParam);
    requestBody = getJsonRpcBody("eth_call", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    balance = responseContent.getString("result").substring(2);
    Long cContractBalance1 = Long.parseLong(balance, 16);
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
