package stest.tron.wallet.dailybuild.grpcurl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.ParserConfig;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.*;

import java.util.Base64;
import java.util.Optional;

@Slf4j
public class GrpcReflectionTest002 {

  private final String foundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] foundationAddress = PublicMethed.getFinalAddress(foundationKey);

  ECKey key = new ECKey(Utils.getRandom());
  private final byte[] receiverAddress = key.getAddress();


  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletGrpc.WalletBlockingStub searchBlockingStubFull = null;
  private ManagedChannel channelSolidity = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;

  private ManagedChannel channelRealSolidity = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubRealSolidity = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private final Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);

  private String realSoliditynode =
      Configuration.getByPath("testng.conf").getStringList("solidityNode.ip.list").get(1);

  private String pbftnode =
      Configuration.getByPath("testng.conf").getStringList("solidityNode.ip.list")
          .get(3);
  private String contractBase64Address = null; //use for some cases query
  private String transferTxIdBase64 = null; //use for some cases query

  private String blockHashBase64 = null; //use for some cases query

  public static final String jsonRpcOwnerKey =
      Configuration.getByPath("testng.conf").getString("defaultParameter.jsonRpcOwnerKey");
  public static final byte[] jsonRpcOwnerAddress = PublicMethed.getFinalAddress(jsonRpcOwnerKey);



  @BeforeClass(enabled = true)
  public void beforeClass() throws Exception {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext()
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext()
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);

    channelRealSolidity =
        ManagedChannelBuilder.forTarget(realSoliditynode).usePlaintext().build();
    blockingStubRealSolidity = WalletSolidityGrpc.newBlockingStub(channelRealSolidity);
    ParserConfig.getGlobalInstance().setAutoTypeSupport(true);
  }


  @Test(enabled = true, description = "test GetTransactionInfoByBlockNum with grpcurl")
  public void test001GetTransactionInfoByBlockNum() {
    ECKey receiver = new ECKey(Utils.getRandom());
    String txId = PublicMethed.sendcoinGetTransactionId(receiver.getAddress(), 10000000, foundationAddress, foundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> trx = PublicMethed.getTransactionInfoById(txId, blockingStubFull);
    long blockNumber = trx.get().getBlockNumber();
    Assert.assertTrue(blockNumber > 0L);
    String txIdBase64 = Base64.getEncoder().encodeToString(ByteArray.fromHexString(txId));
    transferTxIdBase64 = txIdBase64;
    String data = String.format("{\"num\":%s}", blockNumber);
    String requestUrl = "protocol.Wallet/GetTransactionInfoByBlockNum";
    String returnData = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    JSONObject trxData = JSONObject.parseObject(returnData);
    Assert.assertNotNull(trxData);
    logger.info("trxData: " + trxData.toJSONString());
    Assert.assertNotNull(trxData.getJSONArray("transactionInfo"));
    Assert.assertTrue(trxData.getJSONArray("transactionInfo").size() > 0);
    Assert.assertTrue(trxData.toJSONString().contains(transferTxIdBase64));

  }

  @Test(enabled = true, description = "test BroadcastTransaction with grpcurl")
  public void test002BroadcastTransaction() {
    ECKey receiver = new ECKey(Utils.getRandom());
    Protocol.Transaction trx = PublicMethed.sendcoinForTransaction(
        receiver.getAddress(),
        10000000L,
        foundationAddress,
        foundationKey,
        blockingStubFull
    );
    TransactionCapsule trxCapsule = new TransactionCapsule(trx);
    trxCapsule.sign(ByteArray.fromHexString(foundationKey));
    JSONObject trxSign = TransactionUtils.printTransactionToJSON(trxCapsule.getInstance(), false);
    logger.info("before changeBase64 : ");
    logger.info(trxSign.toJSONString());

    // change signature to Base64
    String signHex = trxSign.getJSONArray("signature").get(0).toString();
    String signBase64 = Base64.getEncoder().encodeToString(ByteArray.fromHexString(signHex));
    JSONArray signatureBase64 = new JSONArray();
    signatureBase64.add(signBase64);
    trxSign.put("signature", signatureBase64);

    // change txID to Base64
    String txID = trxSign.getString("txID");
    String txIDBase64 = Base64.getEncoder().encodeToString(ByteArray.fromHexString(txID));
    trxSign.put("txID", txIDBase64);

    // change owner_address and to_address
    String ownerAddress = trxSign
        .getJSONObject("raw_data")
        .getJSONArray("contract")
        .getJSONObject(0)
        .getJSONObject("parameter")
        .getJSONObject("value")
        .getString("owner_address");
    String toAddress = trxSign
        .getJSONObject("raw_data")
        .getJSONArray("contract")
        .getJSONObject(0)
        .getJSONObject("parameter")
        .getJSONObject("value")
        .getString("to_address");
    String ownerAddressBase64 = Base64.getEncoder().encodeToString(ByteArray.fromHexString(ownerAddress));
    String toAddressBase64 = Base64.getEncoder().encodeToString(ByteArray.fromHexString(toAddress));
    JSONObject value = trxSign
        .getJSONObject("raw_data")
        .getJSONArray("contract")
        .getJSONObject(0)
        .getJSONObject("parameter")
        .getJSONObject("value");
    value.put("owner_address", ownerAddressBase64);
    value.put("to_address", toAddressBase64);
    JSONObject parameter = trxSign
        .getJSONObject("raw_data")
        .getJSONArray("contract")
        .getJSONObject(0)
        .getJSONObject("parameter");
    String type = parameter.getString("type_url");
    parameter.remove("value");
    parameter.put("owner_address", ownerAddressBase64);
    parameter.put("to_address", toAddressBase64);
    parameter.put("amount", value.getLongValue("amount"));
    parameter.put("@type", type);
    parameter.remove("type_url");
    JSONArray contract = trxSign
        .getJSONObject("raw_data")
        .getJSONArray("contract");
    JSONObject contractObj = new JSONObject();
    contractObj.put("type", contract.getJSONObject(0).getString("type"));
    contractObj.put("parameter", parameter);
    contract.clear();
    contract.add(contractObj);
    JSONObject rawData = trxSign.getJSONObject("raw_data");
    rawData.put("contract", contract);
    trxSign.put("raw_data", rawData);

    // change ref_block_bytes to base64
    String refBlockBytes = trxSign.getJSONObject("raw_data").getString("ref_block_bytes");
    String refBlockBytesBase64 = Base64.getEncoder().encodeToString(ByteArray.fromHexString(refBlockBytes));
    String refBlockHash = trxSign.getJSONObject("raw_data").getString("ref_block_hash");
    String reBlockHashBase64 = Base64.getEncoder().encodeToString(ByteArray.fromHexString(refBlockHash));
    JSONObject raw = trxSign.getJSONObject("raw_data");
    raw.put("ref_block_bytes", refBlockBytesBase64);
    raw.put("ref_block_hash", reBlockHashBase64);
    trxSign.put("raw_data", raw);
    trxSign.remove("visible");
    trxSign.remove("txID");
    trxSign.remove("raw_data_hex");
    logger.info("afterChangeBase64: ");
    logger.info(trxSign.toJSONString());
    // request gRPCUrl
    String data = trxSign.toJSONString();
    String requestUrl = "protocol.Wallet/BroadcastTransaction";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    logger.info(returnString);
    JSONObject returnJson = JSONObject.parseObject(returnString);
    Assert.assertTrue(returnJson.getBoolean("result"));
  }

  @Test(enabled = true, description = "test GetBlockByLatestNum2")
  public void test003GetBlockByLatestNum2() {
    String data = "{\"num\":1}";
    String requestUrl = "protocol.Wallet/GetBlockByLatestNum2";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    logger.info(returnString);
    Assert.assertNotNull(returnString);
    Assert.assertTrue(returnString.contains("block_header"));
    Assert.assertTrue(returnString.contains("witness_signature"));
    Assert.assertTrue(returnString.contains("number"));
  }

  @Test(enabled = true, description = "test TransferAsset2")
  public void test004TransferAsset2() {
    ECKey receiver = new ECKey(Utils.getRandom());
    String receiverBase64 = Base64.getEncoder().encodeToString(receiver.getAddress());
    String fromBase64 = Base64.getEncoder().encodeToString(jsonRpcOwnerAddress);
    Protocol.Account accountInfo = PublicMethed.queryAccount(jsonRpcOwnerAddress, blockingStubFull);
    String assetName = accountInfo.getAssetIssuedID().toStringUtf8();
    String assetNameBase64 = Base64.getEncoder().encodeToString(ByteArray.fromString(assetName));
    logger.info(assetName);
    String data = String.format(
        "{\"asset_name\":\"%s\",\"owner_address\":\"%s\",\"to_address\":\"%s\",\"amount\":1}",
        assetNameBase64,
        fromBase64,
        receiverBase64
    );
    String requestUrl = "protocol.Wallet/TransferAsset2";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    logger.info(returnString);
  }

  @Test(enabled = true, description = "GetAccountNet")
  public void test005GetAccountNet() {
    String addressBase64 = Base64.getEncoder().encodeToString(foundationAddress);
    String data = String.format("{\"address\":\"%s\"}", addressBase64);
    String requestUrl = "protocol.Wallet/GetAccountNet";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    logger.info(returnString);
    Assert.assertNotNull(returnString);
    Assert.assertTrue(returnString.contains("assetNetUsed"));
    Assert.assertTrue(returnString.contains("TotalNetLimit"));
  }

  @Test(enabled = true, description = "test Solidity GetBlockByNum")
  public void test006GetBlockByNum() {
    String data = "{\"num\":1}";
    String requestUrl = "protocol.WalletSolidity/GetBlockByNum";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, soliditynode);
    JSONObject blockData = JSONObject.parseObject(returnString);
    logger.info(blockData.toJSONString());
    Long blockId = blockData.getJSONObject("block_header").getJSONObject("raw_data").getLong("number");
    Assert.assertEquals(blockId.longValue(), 1L);
    blockHashBase64 = blockData.getJSONObject("block_header").getJSONObject("raw_data").getString("parentHash");
  }

  @Test(enabled = true, description = "test GetAccountResource")
  public void test007GetAccountResource() {
    String addressBase64 = Base64.getEncoder().encodeToString(foundationAddress);
    String data = String.format("{\"address\":\"%s\"}", addressBase64);
    String requestUrl = "protocol.Wallet/GetAccountResource";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    JSONObject resourceData = JSONObject.parseObject(returnString);
    logger.info(resourceData.toJSONString());
    Assert.assertNotNull(resourceData);
    Assert.assertTrue(resourceData.toJSONString().contains("TotalNetLimit"));
  }

  @Test(enabled = true, description = "test GetBlockById")
  public void test008GetBlockById() {
    String data = String.format("{\"value\":\"%s\"}", blockHashBase64);
    String requestUrl = "protocol.Wallet/GetBlockById";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    logger.info(returnString);
    Assert.assertNotNull(returnString);
    Assert.assertTrue(returnString.contains("block_header"));
  }

  @Test(enabled = true, description = "test GetAccount ")
  public void test009GetAccount() {
    String addressBase64 = Base64.getEncoder().encodeToString(foundationAddress);
    String data = String.format("{\"address\":\"%s\"}", addressBase64);
    String requestUrl = "protocol.WalletSolidity/GetAccount";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, soliditynode);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("balance"));
    Assert.assertTrue(returnString.contains(addressBase64));

    String returnStringPBFT = PublicMethed.gRPCurlRequest(data, requestUrl, pbftnode);
    logger.info("Account: " + returnStringPBFT);
    Assert.assertTrue(returnStringPBFT.contains("balance"));
    Assert.assertTrue(returnStringPBFT.contains(addressBase64));
  }

  @Test(enabled = true, description = "test GetBlockByLimitNext")
  public void test010GetBlockByLimitNext() {
    String requestUrl = "protocol.Wallet/GetBlockByLimitNext";
    String data = "{\"startNum\":0,\"endNum\":1}";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    JSONObject blockData = JSONObject.parseObject(returnString);
    logger.info(blockData.toJSONString());
    JSONArray blocks = blockData.getJSONArray("block");
    Assert.assertTrue(blocks.size() > 0L);
  }

  @Test(enabled = true, description = "test GetTransactionListFromPending")
  public void test011GetTransactionListFromPending() {
    String requestUrl = "protocol.Wallet/GetTransactionListFromPending";
    String returnString = PublicMethed.gRPCurlRequest(null, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info("TransactionList: " + returnString);
  }

  @Test(enabled = true, description = "test ListNodes")
  public void test012ListNodes() {
    String requestUrl = "protocol.Wallet/ListNodes";
    String returnString = PublicMethed.gRPCurlRequest(null, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    Assert.assertTrue(returnString.contains("nodes"));
  }

  @Test(enabled = true, description = "test CreateAccount2")
  public void test013CreateAccount2() {
    ECKey newAccount = new ECKey(Utils.getRandom());
    String newAccountBase64 = Base64.getEncoder().encodeToString(newAccount.getAddress());
    String ownerAddressBase64 = Base64.getEncoder().encodeToString(foundationAddress);
    String requestUrl = "protocol.Wallet/CreateAccount2";
    String data = String
        .format(
       "{\"owner_address\":\"%s\",\"account_address\":\"%s\",\"type\":0}",
            ownerAddressBase64,
            newAccountBase64
    );
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info("tx is: " + returnString);
    Assert.assertTrue(returnString.contains("transaction"));
  }

  @Test(enabled = true, description = "test AccountPermissionUpdate")
  public void test014AccountPermissionUpdate() {
    ECKey newAccount = new ECKey(Utils.getRandom());
    String newAccountBase64 = Base64.getEncoder().encodeToString(newAccount.getAddress());
    PublicMethed.sendcoin(newAccount.getAddress(), 10000000, foundationAddress, foundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }




}
