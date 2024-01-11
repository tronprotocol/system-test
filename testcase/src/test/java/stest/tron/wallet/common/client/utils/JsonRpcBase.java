package stest.tron.wallet.common.client.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.ChainParameters;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.services.Util;

// import java.util.*;

@Slf4j
public class JsonRpcBase {

  public final String foundationAccountKey =
      Configuration.getByPath("testng.conf").getString("foundationAccount.key1");
  public final byte[] foundationAccountAddress = PublicMethed.getFinalAddress(foundationAccountKey);
  private final String witnessKey001 =
      Configuration.getByPath("testng.conf").getString("witness.key1");
  private final String witnessKey002 =
      Configuration.getByPath("testng.conf").getString("witness.key2");
  private final byte[] witness001Address = PublicMethed.getFinalAddress(witnessKey001);
  private final byte[] witness002Address = PublicMethed.getFinalAddress(witnessKey002);
  public static final String jsonRpcOwnerKey =
      Configuration.getByPath("testng.conf").getString("defaultParameter.jsonRpcOwnerKey");
  public static final byte[] jsonRpcOwnerAddress = PublicMethed.getFinalAddress(jsonRpcOwnerKey);
  public static final String jsonRpcOwnerAddressString =
      PublicMethed.getAddressString(jsonRpcOwnerKey);
  public static String jsonRpcNode =
      Configuration.getByPath("testng.conf").getStringList("jsonRpcNode.ip.list").get(0);
  public static String jsonRpcNodeForSolidity =
      Configuration.getByPath("testng.conf").getStringList("jsonRpcNode.ip.list").get(1);
  public static String stateTreeNode =
      Configuration.getByPath("testng.conf").getStringList("jsonRpcNode.ip.list").get(2);
  public static String httpFullNode =
      Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list").get(0);
  public static String httpsolidityNode =
      Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list").get(3);
  public static String ethHttpsNode =
      Configuration.getByPath("testng.conf").getStringList("ethHttpsNode.host.list").get(0);

  public ManagedChannel channelFull = null;
  public WalletGrpc.WalletBlockingStub blockingStubFull = null;
  public ManagedChannel channelSolidity = null;
  public ManagedChannel channelPbft = null;
  public static String data = null;
  public String paramString = null;
  public WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  public WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubPbft = null;
  public String fullnode =
      Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list").get(0);

  public static long maxFeeLimit = 0L;
  public static String trc20AddressByteString;
  public static String trc20AddressHex;
  public static String contractAddressFrom58;
  public static String contractTrc20AddressFrom58;
  public static String create2AddressFrom41;
  public static String contractAddressFromHex;
  public static ByteString shieldAddressByteString;
  public static byte[] shieldAddressByte;
  public static byte[] selfDestructAddressByte;
  public static String shieldAddress;
  public static String deployTrc20Txid;
  public static String deployShieldTxid;
  public static String mint = "mint(uint256,bytes32[9],bytes32[2],bytes32[21])";
  public static String transfer =
      "transfer(bytes32[10][],bytes32[2][],bytes32[9][],bytes32[2],bytes32[21][])";
  public static String burn =
      "burn(bytes32[10],bytes32[2],uint256,bytes32[2],address,"
          + "bytes32[3],bytes32[9][],bytes32[21][])";
  // public Wallet wallet = new Wallet();
  static HttpResponse response;
  static HttpPost httppost;
  static JSONObject responseContent;
  public static Integer scalingFactorLogarithm = 0;
  public static Long totalSupply = 1000000000000L;
  public static String name = "jsonrpc-test";
  public static String jsonRpcAssetId;
  public static Long blockNum;
  public static Long blockNumForTrc20;
  public static String blockNumHex;
  public static String blockId;
  public static String txid;
  public static String trc20Txid;
  public HashMap<Long, Long> proposalMap = new HashMap<>();
  public HashMap<Long, Long> secondProposalMap = new HashMap<>();
  public static long waitMaxTime = 610000L;

  /** constructor. */
  @BeforeSuite(enabled = true, description = "Deploy json rpc test case resource")
  public void deployJsonRpcUseResource() throws Exception {
    maxFeeLimit =
            Configuration.getByPath("testng.conf").getLong("defaultParameter.maxFeeLimit");
    logger.info("maxFeeLimit: " + maxFeeLimit);
    // Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext().build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    Assert.assertTrue(
        PublicMethed.sendcoin(
            witness001Address,
            2048000000L,
            foundationAccountAddress,
            foundationAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    getCommitData();
    openProposal(0, proposalMap);
    waitMaxTime = secondProposalMap.get(0L) * 2 + 10000L; //reload max wait time from proposal list
    waitProposalApprove(ProposalEnum.getMaxCpuTimeOfOneTx.getProposalName(), 80,  blockingStubFull);
    openProposal(1, secondProposalMap);
    waitProposalApprove(ProposalEnum.getAllowCancelAllUnfreezeV2.getProposalName(), 1,blockingStubFull);
    Assert.assertTrue(
        PublicMethed.sendcoin(
            jsonRpcOwnerAddress,
            2048000000L,
            foundationAccountAddress,
            foundationAccountKey,
            blockingStubFull));
    if (PublicMethed.queryAccount(jsonRpcOwnerAddress, blockingStubFull).getAssetV2Count() == 0L) {
      Assert.assertTrue(
          PublicMethed.sendcoin(
              jsonRpcOwnerAddress,
              2048000000L,
              foundationAccountAddress,
              foundationAccountKey,
              blockingStubFull));
      PublicMethed.waitProduceNextBlock(blockingStubFull);

      // Create a new Asset Issue
      Assert.assertTrue(
          PublicMethed.createAssetIssue(
              jsonRpcOwnerAddress,
              name,
              totalSupply,
              1,
              1,
              System.currentTimeMillis() + 5000,
              System.currentTimeMillis() + 1000000000,
              1,
              "description",
              "urlurlurl",
              2000L,
              2000L,
              1L,
              1L,
              jsonRpcOwnerKey,
              blockingStubFull));

      PublicMethed.waitProduceNextBlock(blockingStubFull);
    }

    response = HttpMethed.getAccount(httpFullNode, jsonRpcOwnerAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    jsonRpcAssetId = responseContent.getString("asset_issued_ID");

    deployContract();
    triggerContract();
    deployTrc20Contract();
    deploySelfDestructContract();
    deployCreate2Contract();
  }

  void getCommitData() {
    List<String> commitList = Configuration.getByPath("testng.conf").getStringList("commitData.commit.list");
    for (String ent : commitList) {
      String[] str = ent.split(":");
      logger.info(str[0] + " : " + str[1]);
      proposalMap.put(Long.valueOf(str[0]), Long.valueOf(str[1]));
    }

    List<String> secondCommitList = Configuration.getByPath("testng.conf").getStringList("commitData.secondCommit.list");
    for (String ent : secondCommitList) {
      String[] str = ent.split(":");
      logger.info(str[0] + " : " + str[1]);
      secondProposalMap.put(Long.valueOf(str[0]), Long.valueOf(str[1]));
    }

  }

  /** constructor. */
  public void waitProposalApprove(String proposalName, long proposalValue,
                                         WalletGrpc.WalletBlockingStub blockingStubFull) {
    Long currentTime = System.currentTimeMillis();
    while (System.currentTimeMillis() <= currentTime + waitMaxTime) {
      //max wait time is 300s + 310s
      ChainParameters chainParameters = blockingStubFull
          .getChainParameters(GrpcAPI.EmptyMessage.newBuilder().build());
      Optional<ChainParameters> getChainParameters = Optional.ofNullable(chainParameters);
      for (Protocol.ChainParameters.ChainParameter op : getChainParameters.get().getChainParameterList()) {
        if (proposalName.equalsIgnoreCase(op.getKey()) && (op.getValue() == proposalValue)) {
          logger.info(proposalName + ":  Proposal has been approval");
          return;
        }
      }
      PublicMethed.waitProduceNextBlock(blockingStubFull);
    }
  }

  /** constructor. */
  public void openProposal(int openIndex, HashMap<Long, Long> proposalMap)  {

    if (openIndex == 0 && (ProposalGetAllowMarketTransactionIsOpen() || proposalMap.size() == 0)) {
      System.out.println("no need to open proposal");
      return;
    }
    PublicMethed.sendcoin(witness001Address,10000000000L,foundationAccountAddress,foundationAccountKey,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(
        PublicMethed.createProposal(
            witness001Address, witnessKey001, proposalMap, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    GrpcAPI.ProposalList proposalList =
        blockingStubFull.listProposals(GrpcAPI.EmptyMessage.newBuilder().build());
    Optional<GrpcAPI.ProposalList> listProposals = Optional.ofNullable(proposalList);
    final Integer proposalId = listProposals.get().getProposalsCount();
    logger.info(Integer.toString(proposalId));
    Assert.assertTrue(
        PublicMethed.approveProposal(
            witness001Address, witnessKey001, proposalId, true, blockingStubFull));
    Assert.assertTrue(
        PublicMethed.approveProposal(
            witness002Address, witnessKey002, proposalId, true, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    // Get proposal list after approve
    proposalList = blockingStubFull.listProposals(GrpcAPI.EmptyMessage.newBuilder().build());
    listProposals = Optional.ofNullable(proposalList);
    logger.info(Integer.toString(listProposals.get().getProposals(0).getApprovalsCount()));
    Assert.assertTrue(listProposals.get().getProposals(0).getApprovalsCount() == 2);
  }

  /** constructor. */
  public boolean ProposalGetAllowMarketTransactionIsOpen() {
    Protocol.ChainParameters chainParameters = blockingStubFull
        .getChainParameters(GrpcAPI.EmptyMessage.newBuilder().build());
    Optional<Protocol.ChainParameters> getChainParameters = Optional.ofNullable(chainParameters);
    for (Protocol.ChainParameters.ChainParameter op : getChainParameters.get().getChainParameterList()) {
      if("getAllowMarketTransaction".equalsIgnoreCase(op.getKey()) && (op.getValue() == 1)){
        logger.info("1111111: " + op.toString());
        return true;
      }
    }
    return false;
  }

  /** constructor. */
  public void deployContract() throws Exception {
    final Long beforeTokenBalance =
        PublicMethed.getAssetBalanceByAssetId(
            ByteString.copyFromUtf8(jsonRpcAssetId), jsonRpcOwnerKey, blockingStubFull);

    JsonObject param = new JsonObject();
    param.addProperty("from", ByteArray.toHexString(jsonRpcOwnerAddress));
    param.addProperty("name", "transferTokenContract");
    param.addProperty("gas", "0x245498");
    String filePath = "./src/test/resources/soliditycode/contractTrcToken001.sol";
    String contractName = "tokenTest";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    System.out.println("CODE:" + code);
    String abi = retMap.get("abI").toString();
    System.out.println("abi:" + abi);

    param.addProperty("abi", abi);
    param.addProperty("data", code);
    param.addProperty("consumeUserResourcePercent", 100);
    param.addProperty("originEnergyLimit", 11111111111111L);
    param.addProperty("value", "0x1f4");
    param.addProperty("tokenId", Long.valueOf(jsonRpcAssetId));
    param.addProperty("tokenValue", 1);
    JsonArray params = new JsonArray();
    params.add(param);
    JsonObject requestBody = getJsonRpcBody("buildTransaction", params);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String transactionString = responseContent.getJSONObject("result").getString("transaction");
    String transactionSignString =
        HttpMethed.gettransactionsign(httpFullNode, transactionString, jsonRpcOwnerKey);

    responseContent = HttpMethed.parseStringContent(transactionString);
    final String txid = responseContent.getString("txID");
    response = HttpMethed.broadcastTransaction(httpFullNode, transactionSignString);
    org.junit.Assert.assertTrue(HttpMethed.verificationResult(response));

    HttpMethed.waitToProduceOneBlock(httpFullNode);
    Long afterTokenBalance =
        PublicMethed.getAssetBalanceByAssetId(
            ByteString.copyFromUtf8(jsonRpcAssetId), jsonRpcOwnerKey, blockingStubFull);

    org.junit.Assert.assertEquals(beforeTokenBalance - afterTokenBalance, 1L);

    response = HttpMethed.getTransactionById(httpFullNode, txid);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    org.junit.Assert.assertTrue(!responseContent.getString("contract_address").isEmpty());
    contractAddressFrom58 = responseContent.getString("contract_address");
    logger.info("contractAddressFrom58:" + contractAddressFrom58);
  }

  /** constructor. */
  public void triggerContract() throws Exception {
    final Long beforeTokenBalance =
        PublicMethed.getAssetBalanceByAssetId(
            ByteString.copyFromUtf8(jsonRpcAssetId), foundationAccountKey, blockingStubFull);
    final Long beforeBalance = HttpMethed.getBalance(httpFullNode, jsonRpcOwnerAddress);
    JsonObject param = new JsonObject();
    param.addProperty("from", "0x" + ByteArray.toHexString(jsonRpcOwnerAddress).substring(2));
    param.addProperty("to", "0x" + contractAddressFrom58);

    String addressParam =
        "000000000000000000000000"
            + ByteArray.toHexString(foundationAccountAddress).substring(2); // [0,3)

    String tokenIdParam =
        "00000000000000000000000000000000000000000000000000000000000"
            + Integer.toHexString(Integer.valueOf(jsonRpcAssetId));

    String tokenValueParam = "0000000000000000000000000000000000000000000000000000000000000001";
    paramString = addressParam + tokenIdParam + tokenValueParam;
    logger.info("paramString:" + paramString);

    String selector = "TransferTokenTo(address,trcToken,uint256)";
    // exit(1);
    param.addProperty("data", "0x" + Util.parseMethod(selector, paramString));
    data = "0x" + Util.parseMethod(selector, paramString);
    param.addProperty("gas", "0x245498");
    param.addProperty("value", "0x1389");
    param.addProperty("tokenId", Long.valueOf(jsonRpcAssetId));
    param.addProperty("tokenValue", 1);
    JsonArray params = new JsonArray();
    params.add(param);
    JsonObject requestBody = getJsonRpcBody("buildTransaction", params);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String transactionString = responseContent.getJSONObject("result").getString("transaction");
    logger.info("transactionString : " + transactionString);
    String transactionSignString =
        HttpMethed.gettransactionsign(httpFullNode, transactionString, jsonRpcOwnerKey);
    logger.info("transactionSignString:" + transactionSignString);
    responseContent = HttpMethed.parseStringContent(transactionString);
    txid = responseContent.getString("txID");
    logger.info("triggerTxid:" + txid);

    response = HttpMethed.broadcastTransaction(httpFullNode, transactionSignString);
    logger.info("response:" + response);
    HttpMethed.verificationResult(response);
    org.junit.Assert.assertTrue(HttpMethed.verificationResult(response));

    HttpMethed.waitToProduceOneBlock(httpFullNode);
    Long afterTokenBalance =
        PublicMethed.getAssetBalanceByAssetId(
            ByteString.copyFromUtf8(jsonRpcAssetId), foundationAccountKey, blockingStubFull);
    Long afterBalance = HttpMethed.getBalance(httpFullNode, jsonRpcOwnerAddress);

    org.junit.Assert.assertEquals(beforeTokenBalance - afterTokenBalance, -1L);
    org.junit.Assert.assertTrue(beforeBalance - afterBalance >= 5000);

    blockNum =
        (PublicMethed.getTransactionInfoById(txid, blockingStubFull).get().getBlockNumber());
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    response = HttpMethed.getBlockByNum(httpFullNode, blockNum);
    org.junit.Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    blockId = responseContent.get("blockID").toString();
  }

  /** constructor. */
  public void deployTrc20Contract() throws InterruptedException {
    String contractName = "shieldTrc20Token";

    String abi = Configuration.getByPath("testng.conf").getString("abi.abi_shieldTrc20Token");
    String code = Configuration.getByPath("testng.conf").getString("code.code_shieldTrc20Token");
    String constructorStr = "constructor(uint256,string,string)";
    String data = totalSupply.toString() + "," + "\"TokenTRC20\"" + "," + "\"zen20\"";
    logger.info("data:" + data);
    deployTrc20Txid =
        PublicMethed.deployContractWithConstantParame(
            contractName,
            abi,
            code,
            constructorStr,
            data,
            "",
            maxFeeLimit,
            0L,
            100,
            null,
            jsonRpcOwnerKey,
            jsonRpcOwnerAddress,
            blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("deployTrc20Txid：" + deployTrc20Txid);
    response = HttpMethed.getTransactionById(httpFullNode, deployTrc20Txid);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    org.junit.Assert.assertTrue(!responseContent.getString("contract_address").isEmpty());
    contractTrc20AddressFrom58 = responseContent.getString("contract_address");
    logger.info("contractTrc20AddressFrom58:" + contractTrc20AddressFrom58);

    //   NewFilterId = createNewFilterId();

    Optional<TransactionInfo> infoById =
        PublicMethed.getTransactionInfoById(deployTrc20Txid, blockingStubFull);

    trc20AddressHex = ByteArray.toHexString(infoById.get().getContractAddress().toByteArray());
    byte[] trc20Address = infoById.get().getContractAddress().toByteArray();

    String selector = "transfer(address,uint256)";
    String addressParam =
        "000000000000000000000000"
            + ByteArray.toHexString(foundationAccountAddress).substring(2); // [0,3)
    String transferValueParam = "0000000000000000000000000000000000000000000000000000000000000001";
    String paramString = addressParam + transferValueParam;
    trc20Txid =
        PublicMethed.triggerContract(
            trc20Address,
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
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    blockNumForTrc20 =

            (PublicMethed.getTransactionInfoById(trc20Txid, blockingStubFull)
                .get()
                .getBlockNumber());
  }


  /** constructor. */
  public void deploySelfDestructContract() throws InterruptedException {
    String filePath = "./src/test/resources/soliditycode/contractGrammar002test6Grammar013.sol";
    String contractName = "Counter";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    selfDestructAddressByte = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, jsonRpcOwnerKey,
        jsonRpcOwnerAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.getContract(selfDestructAddressByte,blockingStubFull).hasAbi());



  }

  /** constructor. */
  public void deployCreate2Contract() {
    String filePath = "./src/test/resources/soliditycode/contractTrcToken001.sol";
    String contractName = "C";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    byte[] cAddressByte = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, jsonRpcOwnerKey,
        jsonRpcOwnerAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(!PublicMethed.getContract(cAddressByte,blockingStubFull).getBytecode().isEmpty());
    String methedStr = "createWithSalted(bytes32)";
    String argsStr = "1232";
    String txid = PublicMethed.triggerContract(cAddressByte, methedStr, argsStr,
        false, 0, maxFeeLimit, jsonRpcOwnerAddress, jsonRpcOwnerKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Protocol.TransactionInfo infoById =
        PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("Trigger InfobyId: " + infoById);
    Assert.assertEquals(Protocol.TransactionInfo.code.SUCESS, infoById.getResult());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS, infoById.getReceipt().getResult());
    create2AddressFrom41 = "41" + ByteArray.toHexString(infoById.getContractResult(0).toByteArray()).substring(24);
    logger.info("create2AddressFrom41: " + create2AddressFrom41);

  }

  /** constructor. */
  public static HttpResponse getEthHttps(String ethHttpsNode, JsonObject jsonRpcObject) {
    try {
      String requestUrl = "https://" + ethHttpsNode + "/v3/dfb752dd45204b8daae74249f4653584";
      response = HttpMethed.createConnect(requestUrl, jsonRpcObject);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static HttpResponse getJsonRpc(String jsonRpcNode, JsonObject jsonRpcObject) {
    try {
      String requestUrl = "http://" + jsonRpcNode + "/jsonrpc";
      response = HttpMethed.createConnect(requestUrl, jsonRpcObject);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static HttpResponse getJsonRpc(String jsonRpcNode, JsonArray jsonRpcArray) {
    try {
      String requestUrl = "http://" + jsonRpcNode + "/jsonrpc";
      response = HttpMethed.createConnect(requestUrl, jsonRpcArray);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static JsonObject getJsonRpcBody(String method) {
    return getJsonRpcBody(method, new JsonArray(), 1);
  }

  /** constructor. */
  public static JsonObject getJsonRpcBody(String method, JsonArray params) {
    return getJsonRpcBody(method, params, 1);
  }

  /** constructor. */
  public static JsonObject getJsonRpcBody(String method, JsonArray params, Integer id) {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("jsonrpc", "2.0");
    jsonObject.addProperty("method", method);
    jsonObject.add("params", params);
    jsonObject.addProperty("id", id);

    return jsonObject;
  }

  public Boolean stateRootIsOpen() {
    JsonArray params = new JsonArray();
    params.add("0x" + ByteArray.toHexString(foundationAccountAddress).substring(2));
    params.add("0x" + Long.toHexString(1));
    JsonObject requestBody = getJsonRpcBody("eth_getBalance", params);
    response = getJsonRpc(stateTreeNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String resStr = responseContent.toJSONString();
    logger.info(resStr);

    if(resStr.contains("QUANTITY not supported, just support TAG as latest")){
      return false;
    }else {
      return true;
    }
  }
}
