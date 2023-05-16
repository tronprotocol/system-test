package stest.tron.wallet.common.client.utils;

import bridge.v1.BridgeServiceGrpc;
import bridge.v1.Zkevm;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.tron.api.WalletGrpc;

import stest.tron.wallet.common.client.Configuration;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;

@Slf4j
public class PublicMethodForZkEvm {

  static final String zkEvmNode = Configuration.getByPath("testng.conf").getString("zkEvm.grpc.host");
  private static ManagedChannel channelZkEvm = ManagedChannelBuilder.forTarget(zkEvmNode).usePlaintext(true).build();
  private static BridgeServiceGrpc.BridgeServiceBlockingStub blockingStubZkEvm = BridgeServiceGrpc.newBlockingStub(channelZkEvm);

  static final String zkEvmHttpNode = Configuration.getByPath("testng.conf").getString("zkEvm.http.host");
  static Integer connectionTimeout =
    Configuration.getByPath("testng.conf").getInt("defaultParameter.httpConnectionTimeout");
  static Integer soTimeout =
    Configuration.getByPath("testng.conf").getInt("defaultParameter.httpSoTimeout");


  public static Zkevm.CheckAPIResponse checkAPI() {
    Zkevm.CheckAPIRequest.Builder builder = Zkevm.CheckAPIRequest.newBuilder();
    return blockingStubZkEvm.checkAPI(builder.build());
  }

  public static Zkevm.GetBridgesResponse getBridges(String destAddr, Integer offset, Integer limit) {
    Zkevm.GetBridgesRequest.Builder builder = Zkevm.GetBridgesRequest.newBuilder();
    builder.setDestAddr(destAddr);
    builder.setOffset(offset);
    builder.setLimit(limit);
    return blockingStubZkEvm.getBridges(builder.build());
  }

  public static Zkevm.GetBridgeResponse getBridge(Integer netId, Integer depositCnt) {
    Zkevm.GetBridgeRequest.Builder builder = Zkevm.GetBridgeRequest.newBuilder();
    builder.setNetId(netId);
    builder.setDepositCnt(depositCnt);
    return blockingStubZkEvm.getBridge(builder.build());
  }

  public static Zkevm.GetProofResponse getProof(Integer netId, Integer depositCnt) {
    Zkevm.GetProofRequest.Builder builder = Zkevm.GetProofRequest.newBuilder();
    builder.setNetId(netId);
    builder.setDepositCnt(depositCnt);
    return blockingStubZkEvm.getProof(builder.build());
  }

  public static Zkevm.GetClaimsResponse getClaims(String destAddr, Integer offset, Integer limit) {
    Zkevm.GetClaimsRequest.Builder builder = Zkevm.GetClaimsRequest.newBuilder();
    builder.setDestAddr(destAddr);
    builder.setOffset(offset);
    builder.setLimit(limit);
    return blockingStubZkEvm.getClaims(builder.build());
  }

  public static Zkevm.GetTokenWrappedResponse getTokenWrapped(String origTokenAddr, Integer origNet) {
    Zkevm.GetTokenWrappedRequest.Builder builder = Zkevm.GetTokenWrappedRequest.newBuilder();
    builder.setOrigTokenAddr(origTokenAddr);
    builder.setOrigNet(origNet);
    return blockingStubZkEvm.getTokenWrapped(builder.build());
  }

  public static Zkevm.GetTransactionInfoResponse getTransactionInfo(String fromHash) {
    Zkevm.GetTransactionInfoRequest.Builder builder = Zkevm.GetTransactionInfoRequest.newBuilder();
    builder.setFromHash(fromHash);
    return blockingStubZkEvm.getTransactionInfo(builder.build());
  }

  public static Zkevm.GetTransactionRecordResponse getTransactionRecord(
    String address,
    Integer start,
    Integer limit,
    Integer status,
    Integer type,
    Integer startTime,
    Integer endTime) {
    Zkevm.GetTransactionRecordRequest.Builder builder = Zkevm.GetTransactionRecordRequest.newBuilder();
    builder.setAddress(address);
    builder.setStart(start);
    builder.setStatus(status);
    builder.setLimit(limit);
    builder.setType(type);
    builder.setStartTime(startTime);
    builder.setEndTime(endTime);
    return blockingStubZkEvm.getTransactionRecord(builder.build());
  }

  public static Zkevm.GetAccountTokenBalanceResponse getAccountTokenBalance(String accountAddress, Integer fromChainId) {
    Zkevm.GetAccountTokenBalanceRequest.Builder builder = Zkevm.GetAccountTokenBalanceRequest.newBuilder();
    builder.setAccountAddress(accountAddress);
    builder.setFromChainId(fromChainId);
    return blockingStubZkEvm.getAccountTokenBalance(builder.build());
  }

  public static Zkevm.GetGasEstimateResponse gasEstimate(Integer fromChainId, String fromToken) {
    Zkevm.GetGasEstimateRequest.Builder builder = Zkevm.GetGasEstimateRequest.newBuilder();
    builder.setFromChainId(fromChainId);
    builder.setFromToken(fromToken);
    return blockingStubZkEvm.getGasEstimate(builder.build());
  }

  public static HttpResponse checkAPIHttp() {
    final String requestUrl = "http://" + zkEvmHttpNode + "/api";
    return httpGet(requestUrl, null);
  }

  public static HttpResponse getBridgesHttp(String destAddr, Integer offset, Integer limit) {
    JsonObject requestBody = new JsonObject();
    requestBody.addProperty("offset", offset);
    requestBody.addProperty("limit", limit);
    final String requestUrl = "http://" + zkEvmHttpNode + "/bridges/" + destAddr;
    return httpGet(requestUrl, requestBody);
  }

  public static HttpResponse getBridgeHttp(Integer netId, Integer depositCnt) {
    JsonObject requestBody = new JsonObject();
    requestBody.addProperty("net_id", netId);
    requestBody.addProperty("deposit_cnt", depositCnt);
    final String requestUrl = "http://" + zkEvmHttpNode + "/bridge";
    return httpGet(requestUrl, requestBody);
  }

  public static HttpResponse getProofHttp(Integer netId, Integer depositCnt) {
    JsonObject requestBody = new JsonObject();
    requestBody.addProperty("net_id", netId);
    requestBody.addProperty("deposit_cnt", depositCnt);
    final String requestUrl = "http://" + zkEvmHttpNode + "/merkle-proof";
    return httpGet(requestUrl, requestBody);
  }

  public static HttpResponse getClaimsHttp(String destAddr, Integer offset, Integer limit) {
    JsonObject requestBody = new JsonObject();
    requestBody.addProperty("offset", offset);
    requestBody.addProperty("limit", limit);
    final String requestUrl = "http://" + zkEvmHttpNode + "/claims/" + destAddr;
    return httpGet(requestUrl, requestBody);
  }

  public static HttpResponse getTokenWrappedHttp(String origTokenAddr, Integer origNet) {
    JsonObject requestBody = new JsonObject();
    requestBody.addProperty("orig_token_addr", origTokenAddr);
    requestBody.addProperty("orig_net", origNet);
    final String requestUrl = "http://" + zkEvmHttpNode + "/tokenwrapped";
    return httpGet(requestUrl, requestBody);
  }

  public static HttpResponse getTransactionInfoHttp(String fromHash) {
    JsonObject requestBody = new JsonObject();
    requestBody.addProperty("from_hash", fromHash);
    final String requestUrl = "http://" + zkEvmHttpNode + "/transaction-info";
    return httpGet(requestUrl, requestBody);
  }

  public static HttpResponse getTransactionRecordHttp(
    String address,
    Integer start,
    Integer limit,
    Integer status,
    Integer type,
    Integer startTime,
    Integer endTime) {
    JsonObject requestBody = new JsonObject();
    requestBody.addProperty("address", address);
    requestBody.addProperty("start", start);
    requestBody.addProperty("limit", limit);
    requestBody.addProperty("status", status);
    requestBody.addProperty("type", type);
    requestBody.addProperty("start_time", startTime);
    requestBody.addProperty("end_time", endTime);
    final String requestUrl = "http://" + zkEvmHttpNode + "/transaction-record";
    return httpGet(requestUrl, requestBody);
  }

  public static HttpResponse getAccountTokenBalanceHttp(String accountAddress, Integer fromChainId) {
    JsonObject requestBody = new JsonObject();
    requestBody.addProperty("account_address", accountAddress);
    requestBody.addProperty("from_chain_id", fromChainId);
    final String requestUrl = "http://" + zkEvmHttpNode + "/getAccountTokenBalance";
    return httpGet(requestUrl, requestBody);
  }

  public static HttpResponse gasEstimateHttp(Integer fromChainId, String fromToken) {
    JsonObject requestBody = new JsonObject();
    requestBody.addProperty("from_token", fromToken);
    requestBody.addProperty("from_chain_id", fromChainId);
    final String requestUrl = "http://" + zkEvmHttpNode + "/gas-estimate";
    return httpGet(requestUrl, requestBody);
  }


  public static HttpResponse httpGet(String url, JsonObject requestBody) {
    HttpClient httpClient = new DefaultHttpClient();
    HttpGet httpGet = new HttpGet(url);
    HttpResponse response;
    try {
      httpClient
        .getParams()
        .setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, connectionTimeout);
      httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, soTimeout);
      httpGet.setHeader("Content-type", "application/json; charset=utf-8");
      httpGet.setHeader("Connection", "Close");
      if (requestBody != null) {
        URIBuilder uriBuild = new URIBuilder(httpGet.getURI());
        requestBody.entrySet().forEach((k)-> {
          uriBuild.setParameter(k.getKey(), k.getValue().getAsString());
        });
        httpGet.setURI(uriBuild.build());
      }

      logger.info(httpGet.toString());
      response = httpClient.execute(httpGet);
    } catch (Exception e) {
      e.printStackTrace();
      httpGet.releaseConnection();
      return null;
    }
    return response;
  }


  public static JSONObject parseResponseContent(HttpResponse response) {
    try {
      String result = EntityUtils.toString(response.getEntity());
      StringEntity entity = new StringEntity(result, Charset.forName("UTF-8"));
      response.setEntity(entity);
      JSONObject obj = JSONObject.parseObject(result);
      return obj;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  //bytes address to 0x String
  public static String getETHAddress(byte[] address) {
    if (address.length == 0) {
      return null;
    }
    String prefix = ByteArray.toHexString(address).substring(0, 2);
    if (!prefix.equals("41")) {
      return null;
    }
    return "0x" + ByteArray.toHexString(address).substring(2);
  }

}

