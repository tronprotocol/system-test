package stest.tron.wallet.dailybuild.jsonrpc;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.HttpMethod;
import stest.tron.wallet.common.client.utils.JsonRpcBase;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;


@Slf4j

public class BuildTransaction001 extends JsonRpcBase {

  JSONArray jsonRpcReceives = new JSONArray();
  //String txid;
  private JSONObject responseContent;
  private HttpResponse response;
  String transactionString;
  String transactionSignString;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] receiverAddress = ecKey1.getAddress();
  final String receiverKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext()
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }



  @Test(enabled = true, description = "Json rpc api of buildTransaction for transfer trx", groups = {"daily", "serial"})
  public void test01JsonRpcApiTestOfBuildTransactionForTransferTrx() throws Exception {
    final Long beforeRecevierBalance = HttpMethod.getBalance(httpFullNode, receiverAddress);


    JsonObject param = new JsonObject();
    param.addProperty("from", ByteArray.toHexString(jsonRpcOwnerAddress));
    param.addProperty("to", ByteArray.toHexString(receiverAddress));
    param.addProperty("value", "0x1");
    JsonArray params = new JsonArray();
    params.add(param);
    JsonObject requestBody = getJsonRpcBody("buildTransaction",params);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethod.parseResponseContent(response);
    transactionString = responseContent.getJSONObject("result").getString("transaction");
    transactionSignString = HttpMethod.gettransactionsign(httpFullNode, transactionString,
        jsonRpcOwnerKey);
    response = HttpMethod.broadcastTransaction(httpFullNode, transactionSignString);
    Assert.assertTrue(HttpMethod.verificationResult(response));

    HttpMethod.waitToProduceOneBlock(httpFullNode);
    Long afterRecevierBalance = HttpMethod.getBalance(httpFullNode, receiverAddress);

    Assert.assertEquals(afterRecevierBalance - beforeRecevierBalance,1L);

  }

  @Test(enabled = true, description = "Json rpc api of buildTransaction for transfer trc10", groups = {"daily", "serial"})
  public void test02JsonRpcApiTestOfBuildTransactionForTransferTrc10() throws Exception {
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    final Long beforeTokenBalance = PublicMethod.getAssetBalanceByAssetId(ByteString
        .copyFromUtf8(jsonRpcAssetId), receiverKey, blockingStubFull);
    JsonObject param = new JsonObject();
    param.addProperty("from", ByteArray.toHexString(jsonRpcOwnerAddress));
    param.addProperty("to", ByteArray.toHexString(receiverAddress));
    param.addProperty("tokenId", Long.valueOf(jsonRpcAssetId));
    param.addProperty("tokenValue", 1);
    JsonArray params = new JsonArray();
    params.add(param);
    JsonObject requestBody = getJsonRpcBody("buildTransaction",params);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethod.parseResponseContent(response);
    transactionString = responseContent.getJSONObject("result").getString("transaction");
    transactionSignString = HttpMethod.gettransactionsign(httpFullNode, transactionString,
        jsonRpcOwnerKey);
    response = HttpMethod.broadcastTransaction(httpFullNode, transactionSignString);
    Assert.assertTrue(HttpMethod.verificationResult(response));

    HttpMethod.waitToProduceOneBlock(httpFullNode);
    Long afterTokenBalance = PublicMethod.getAssetBalanceByAssetId(ByteString
        .copyFromUtf8(jsonRpcAssetId), receiverKey, blockingStubFull);

    Assert.assertEquals(afterTokenBalance - beforeTokenBalance,1L);

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
