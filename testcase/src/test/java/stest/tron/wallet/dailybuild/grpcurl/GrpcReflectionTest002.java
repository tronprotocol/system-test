package stest.tron.wallet.dailybuild.grpcurl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.ParserConfig;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.protobuf.ByteString;
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
    Assert.assertTrue(returnString.contains("transaction"));
  }

  @Test(enabled = true, description = "GetAccountNet")
  public void test005GetAccountNet() {
    String addressBase64 = Base64.getEncoder().encodeToString(foundationAddress);
    String data = String.format("{\"address\":\"%s\"}", addressBase64);
    String requestUrl = "protocol.Wallet/GetAccountNet";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    logger.info(returnString);
    Assert.assertNotNull(returnString);
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

    String returnStringPBFT = PublicMethed.gRPCurlRequest(data, requestUrl, pbftnode);
    Assert.assertEquals(returnStringPBFT, returnString);
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
    Assert.assertTrue(returnString.contains("{"));
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
    PublicMethed.sendcoin(newAccount.getAddress(), 1000000000, foundationAddress, foundationKey, blockingStubFull);

    String data = String.format("{\"owner_address\":\"%s\",\"owner\":{\"type\":0,\"id\":0,\"permission_name\":\"owner\"," +
        "\"threshold\":1,\"keys\":[{\"address\":\"%s\",\"weight\":1}]},\"actives\":[{\"type\":2,\"id\":2," +
        "\"permission_name\":\"active\",\"threshold\":1,\"operations\":\"f/8fwAM+8w8AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=\"," +
        "\"keys\":[{\"address\":\"%s\",\"weight\":1}]}]}",
        newAccountBase64,
        newAccountBase64,
        newAccountBase64
        );
    String requestUrl = "protocol.Wallet/AccountPermissionUpdate";
    String returnString =PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains(newAccountBase64));
    Assert.assertTrue(returnString.contains("transaction"));
    Assert.assertTrue(returnString.contains("ref_block_bytes"));
    Assert.assertTrue(returnString.contains("expiration"));
    Assert.assertTrue(returnString.contains("raw_data"));
  }

  @Test(enabled = true, description = "test CancelAllUnfreezeV2")
  public void test015CancelAllUnfreezeV2(){
    ECKey newAccount = new ECKey(Utils.getRandom());
    String newAccountKey = ByteArray.toHexString(newAccount.getPrivKeyBytes());
    PublicMethed.sendcoin(newAccount.getAddress(), 100000000, foundationAddress, foundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.freezeBalanceV2(newAccount.getAddress(), 50000000, 1, newAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.unFreezeBalanceV2(newAccount.getAddress(), newAccountKey, 25000000, 1, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String ownerAddressBase64 = Base64.getEncoder().encodeToString(newAccount.getAddress());
    String data = String.format("{\"owner_address\":\"%s\"}", ownerAddressBase64);
    String requestUrl = "protocol.Wallet/CancelAllUnfreezeV2";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains(ownerAddressBase64));
    Assert.assertTrue(returnString.contains("transaction"));
    Assert.assertTrue(returnString.contains("ref_block_bytes"));
    Assert.assertTrue(returnString.contains("expiration"));
    Assert.assertTrue(returnString.contains("raw_data"));
  }

  @Test(enabled = true, description = "test CreateAccount")
  public void test016CreateAccount() {
    ECKey newAccount = new ECKey(Utils.getRandom());
    String newAccountBase64 = Base64.getEncoder().encodeToString(newAccount.getAddress());
    String ownerAddressBase64 = Base64.getEncoder().encodeToString(foundationAddress);
    String requestUrl = "protocol.Wallet/CreateAccount";
    String data = String
        .format(
            "{\"owner_address\":\"%s\",\"account_address\":\"%s\",\"type\":0}",
            ownerAddressBase64,
            newAccountBase64
        );
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info("tx is: " + returnString);
    Assert.assertTrue(returnString.contains("raw_data"));
  }

  @Test(enabled = true, description = "test CreateAssetIssue")
  public void test017CreateAssetIssue(){
    ECKey newAccount = new ECKey(Utils.getRandom());
    String newAccountBase64 = Base64.getEncoder().encodeToString(newAccount.getAddress());
    PublicMethed.sendcoin(newAccount.getAddress(), 2000000000, foundationAddress, foundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String data = String.format("{\"owner_address\":\"%s\",\"name\":\"Qml0VG9ycmVudA==\",\"abbr\":\"QkNU\"," +
        "\"total_supply\":1000000,\"frozen_supply\":[{\"frozen_amount\":1,\"frozen_days\":1}]," +
        "\"trx_num\":1,\"precision\":6,\"num\":1,\"start_time\":%d,\"end_time\":%d," +
        "\"description\":\"Z1JQQ3VybEFzc2V0\",\"url\":\"d3d3LnVybC5jb20=\",\"free_asset_net_limit\":1000," +
        "\"public_free_asset_net_limit\":2000}",
        newAccountBase64,
        System.currentTimeMillis(),
        System.currentTimeMillis() + 100000L
        );
    String requestUrl = "protocol.Wallet/CreateAssetIssue";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertFalse(returnString.contains("transaction"));
    Assert.assertTrue(returnString.contains("ref_block_bytes"));
    Assert.assertTrue(returnString.contains("expiration"));
    Assert.assertTrue(returnString.contains("raw_data"));
  }


  @Test(enabled = true, description = "test CreateAssetIssue2")
  public void test018CreateAssetIssue2(){
    ECKey newAccount = new ECKey(Utils.getRandom());
    String newAccountBase64 = Base64.getEncoder().encodeToString(newAccount.getAddress());
    PublicMethed.sendcoin(newAccount.getAddress(), 2000000000, foundationAddress, foundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String data = String.format("{\"owner_address\":\"%s\",\"name\":\"Qml0VG9ycmVudA==\",\"abbr\":\"QkNU\"," +
            "\"total_supply\":1000000,\"frozen_supply\":[{\"frozen_amount\":1,\"frozen_days\":1}]," +
            "\"trx_num\":1,\"precision\":6,\"num\":1,\"start_time\":%d,\"end_time\":%d," +
            "\"description\":\"Z1JQQ3VybEFzc2V0\",\"url\":\"d3d3LnVybC5jb20=\",\"free_asset_net_limit\":1000," +
            "\"public_free_asset_net_limit\":2000}",
        newAccountBase64,
        System.currentTimeMillis(),
        System.currentTimeMillis() + 100000L
    );
    String requestUrl = "protocol.Wallet/CreateAssetIssue2";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("transaction"));
    Assert.assertTrue(returnString.contains("ref_block_bytes"));
    Assert.assertTrue(returnString.contains("expiration"));
    Assert.assertTrue(returnString.contains("raw_data"));
  }

  @Test(enabled = true, description = "test CreateCommonTransaction")
  public void test019CreateCommonTransaction() {
    ECKey newAccount = new ECKey(Utils.getRandom());
    String newAccountBase64 = Base64.getEncoder().encodeToString(newAccount.getAddress());
    String ownerAddressBase64 = Base64.getEncoder().encodeToString(foundationAddress);
    String data = String.format(
        "{\"raw_data\":[{\"contract\":{\"type\":\"TransferContract\",\"parameter\":{\"@type\":\"type.googleapis.com/protocol.TransferContract\",\"owner_address\":\"%s\",\"to_address\":\"%s\",\"amount\":100000000}}}]}",
        ownerAddressBase64,
        newAccountBase64
    );
    String requestUrl = "protocol.Wallet/CreateCommonTransaction";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("transaction"));
    Assert.assertTrue(returnString.contains("ref_block_bytes"));
    Assert.assertTrue(returnString.contains("expiration"));
    Assert.assertTrue(returnString.contains("raw_data"));
  }

  @Test(enabled = true, description = "test CreateWitness")
  public void test020CreateWitness() {
    String ownerAddressBase64 = Base64.getEncoder().encodeToString(foundationAddress);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String data = String.format("{\"owner_address\":\"%s\",\"url\":\"d3d3LmdycGN1cmx0ZXN0LmNvbQ==\"}",
        ownerAddressBase64
        );
    String requestUrl = "protocol.Wallet/CreateWitness";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("ref_block_bytes"));
    Assert.assertTrue(returnString.contains("expiration"));
    Assert.assertTrue(returnString.contains("raw_data"));
  }

  @Test(enabled = true, description = "test CreateWitness2")
  public void test021CreateWitness2() {
    String ownerAddressBase64 = Base64.getEncoder().encodeToString(foundationAddress);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String data = String.format("{\"owner_address\":\"%s\",\"url\":\"d3d3LmdycGN1cmx0ZXN0LmNvbQ==\"}",
        ownerAddressBase64
    );
    String requestUrl = "protocol.Wallet/CreateWitness2";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("transaction"));
    Assert.assertTrue(returnString.contains("ref_block_bytes"));
    Assert.assertTrue(returnString.contains("expiration"));
    Assert.assertTrue(returnString.contains("raw_data"));
  }

  @Test(enabled = true, description = "test DelegateResource")
  public void test022DelegateResource() {
    ECKey newAccount = new ECKey(Utils.getRandom());
    String newAccountBase64 = Base64.getEncoder().encodeToString(newAccount.getAddress());
    String ownerAddressBase64 = Base64.getEncoder().encodeToString(foundationAddress);
    PublicMethed.sendcoin(newAccount.getAddress(), 100000000, foundationAddress, foundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.freezeBalanceV2(newAccount.getAddress(), 50000000,
        1, ByteArray.toHexString(newAccount.getPrivKeyBytes()),blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String data = String.format("{\"owner_address\":\"%s\",\"resource\":\"ENERGY\",\"balance\":50000000,\"receiver_address\":\"%s\"}",
        newAccountBase64,
        ownerAddressBase64
    );
    String requestUrl = "protocol.Wallet/DelegateResource";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("transaction"));
    Assert.assertTrue(returnString.contains("ref_block_bytes"));
    Assert.assertTrue(returnString.contains("expiration"));
    Assert.assertTrue(returnString.contains("raw_data"));
  }

  @Test(enabled = true, description = "DeployContract")
  public void test023DeployContract() {
    String ownerAddressBase64 = Base64.getEncoder().encodeToString(foundationAddress);
    String data = String.format("{\"owner_address\":\"%s\",\"new_contract\":{\"abi\":{\"entrys\":[{\"inputs\":[{" +
            "\"name\":\"value\",\"type\":\"uint256\"}],\"name\":\"foo\",\"outputs\":[{\"name\":\"\",\"type\":" +
            "\"uint256\"}],\"stateMutability\":\"Nonpayable\",\"type\":\"Function\"}]},\"bytecode\":" +
            "\"NjA4MDYwNDA1MjM0ODAxNTYxMDAxMDU3NjAwMDgwZmQ1YjUwZDM4MDE1NjEwMDFkNTc2MDAwODBmZDViNTBkMjgwMTU2MTAwMmE" +
            "1NzYwMDA4MGZkNWI1MDYwYjg4MDYxMDAzOTYwMDAzOTYwMDBmM2ZlNjA4MDYwNDA1MjM0ODAxNTYwMGY1NzYwMDA4MGZkNWI1MGQzOD" +
            "AxNTYwMWI1NzYwMDA4MGZkNWI1MGQyODAxNTYwMjc1NzYwMDA4MGZkNWI1MDYwMDQzNjEwNjA0MDU3NjAwMDM1NjBlMDFjODA2MzJmY" +
            "mViZDM4MTQ2MDQ1NTc1YjYwMDA4MGZkNWI2MDU4NjA1MDM2NjAwNDYwNmE1NjViNjAwMDgxOTA1NTkwNTY1YjYwNDA1MTkwODE1MjY" +
            "wMjAwMTYwNDA1MTgwOTEwMzkwZjM1YjYwMDA2MDIwODI4NDAzMTIxNTYwN2I1NzYwMDA4MGZkNWI1MDM1OTE5MDUwNTZmZWEyNjQ3ND" +
            "cyNmY2ZTU4MjIxMjIwYzg2Njk3MTVmZTk1MzgxZWUzMTVlMTExOTA2NGZmODBhODEyMWRkZmM1Mjc0N2E1NTE2MWQwZDRmNGRkYTJh" +
            "NTY0NzM2ZjZjNjM0MzAwMDgxMjAwMzM=\",\"name\":\"MyContract\",\"consume_user_resource_percent\":100}}",
        ownerAddressBase64
        );
    String requestUrl = "protocol.Wallet/DeployContract";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("transaction"));
    Assert.assertTrue(returnString.contains("ref_block_bytes"));
    Assert.assertTrue(returnString.contains("expiration"));
    Assert.assertTrue(returnString.contains("raw_data"));
  }

  @Test(enabled = true, description = "test FreezeBalanceV2")
  public void test024FreezeBalanceV2() {
    String ownerAddressBase64 = Base64.getEncoder().encodeToString(foundationAddress);
    String data = String.format("{\"owner_address\":\"%s\",\"frozen_balance\":100000000,\"resource\":1}", ownerAddressBase64);
    String requestUrl = "protocol.Wallet/FreezeBalanceV2";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("transaction"));
    Assert.assertTrue(returnString.contains("ref_block_bytes"));
    Assert.assertTrue(returnString.contains("expiration"));
    Assert.assertTrue(returnString.contains("raw_data"));
  }

  @Test(enabled = true, description = "test GetAccountBalance")
  public void test025GetAccountBalance() {
    String ownerAddressBase64 = Base64.getEncoder().encodeToString(foundationAddress);
    Protocol.Block block =  PublicMethed.getBlock(2L, blockingStubFull);
    byte[] hash = block.getBlockHeader().getRawData().getParentHash().toByteArray();
    String hashBase64 = Base64.getEncoder().encodeToString(hash);
    String data = String.format("{\"account_identifier\":{\"address\":\"%s\"},\"block_identifier\":{\"number\":1,\"hash\":\"%s\"}}",
        ownerAddressBase64,
        hashBase64
        );
    String requestUrl = "protocol.Wallet/GetAccountBalance";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("balance"));
    Assert.assertTrue(returnString.contains("10000000000000000"));
  }

  @Test(enabled = true, description = "test GetAccountById")
  public void test026GetAccountById() {
    ECKey newAccount = new ECKey(Utils.getRandom());
    PublicMethed.sendcoin(newAccount.getAddress(), 100000000, foundationAddress, foundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String utf8AccountId = "grpcUrlTest";
    PublicMethed.setAccountId(utf8AccountId.getBytes(), newAccount.getAddress(),ByteArray.toHexString(newAccount.getPrivKeyBytes()), blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String data = "{\"account_id\":\"Z3JwY1VybFRlc3Q=\"}";
    String requestUrl = "protocol.Wallet/GetAccountById";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    JSONObject accountInfo = JSONObject.parseObject(returnString);
    Assert.assertEquals(accountInfo.getLongValue("balance"), 100000000L);
    Assert.assertTrue(returnString.contains("account_id"));

    String requestUrlSolidity = "protocol.WalletSolidity/GetAccountById";
    String returnStringSolidity = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, soliditynode);
    Assert.assertTrue(returnStringSolidity.contains("account_id"));
    Assert.assertTrue(returnStringSolidity.contains("Z3JwY1VybFRlc3Q="));
    String returnStringPBFT = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, pbftnode);
    Assert.assertTrue(returnStringPBFT.contains("account_id"));
    Assert.assertTrue(returnStringPBFT.contains("Z3JwY1VybFRlc3Q="));


  }

  @Test(enabled = true, description = "test GetAssetIssueByAccount")
  public void test027GetAssetIssueByAccount() {
    String ownerAddressBase64 = Base64.getEncoder().encodeToString(jsonRpcOwnerAddress);
    String data = String.format("{\"address\":\"%s\"}", ownerAddressBase64);
    String requestUrl = "protocol.Wallet/GetAssetIssueByAccount";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("assetIssue"));
    Assert.assertTrue(returnString.contains("public_free_asset_net_limit"));
  }

  @Test(enabled = true, description = "test GetAssetIssueById")
  public void test028GetAssetIssueById() {
    String data = "{\"value\":\"MTAwMDAwMQ==\"}";
    String requestUrl = "protocol.Wallet/GetAssetIssueById";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("owner_address"));
    Assert.assertTrue(returnString.contains("total_supply"));
    Assert.assertTrue(returnString.contains("description"));

    String requestUrlSolidity = "protocol.WalletSolidity/GetAssetIssueById";
    String returnStringSolidity = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, soliditynode);
    Assert.assertEquals(returnStringSolidity, returnString);
    String returnStringPBFT = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, pbftnode);
    Assert.assertEquals(returnStringPBFT, returnString);
  }

  @Test(enabled = true, description = "test GetAssetIssueByName")
  public void test029GetAssetIssueByName() {
    String data = "{\"value\":\"anNvbnJwYy10ZXN0\"}";
    String requestUrl = "protocol.Wallet/GetAssetIssueByName";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("owner_address"));
    Assert.assertTrue(returnString.contains("total_supply"));
    Assert.assertTrue(returnString.contains("description"));

    String requestUrlSolidity = "protocol.WalletSolidity/GetAssetIssueByName";
    String returnStringSolidity = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, soliditynode);
    Assert.assertEquals(returnStringSolidity, returnString);
    String returnStringPBFT = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, pbftnode);
    Assert.assertEquals(returnStringPBFT, returnString);
  }

  @Test(enabled = true, description = "test GetAssetIssueList")
  public void test030GetAssetIssueList() {
    String requestUrl = "protocol.Wallet/GetAssetIssueList";
    String returnString = PublicMethed.gRPCurlRequest(null, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("owner_address"));
    Assert.assertTrue(returnString.contains("total_supply"));
    Assert.assertTrue(returnString.contains("description"));

    String requestUrlSolidity = "protocol.WalletSolidity/GetAssetIssueList";
    String returnStringSolidity = PublicMethed.gRPCurlRequest(null, requestUrlSolidity, soliditynode);
    logger.info(returnStringSolidity);
    Assert.assertTrue(returnStringSolidity.contains("owner_address"));
    Assert.assertTrue(returnStringSolidity.contains("total_supply"));
    Assert.assertTrue(returnStringSolidity.contains("description"));
    String returnStringPBFT = PublicMethed.gRPCurlRequest(null, requestUrlSolidity, pbftnode);
    logger.info(returnStringPBFT);
    Assert.assertTrue(returnStringPBFT.contains("owner_address"));
    Assert.assertTrue(returnStringPBFT.contains("total_supply"));
    Assert.assertTrue(returnStringPBFT.contains("description"));
  }

  @Test(enabled = true, description = "test GetAssetIssueListByName")
  public void test031GetAssetIssueListByName() {
    String data = "{\"value\":\"anNvbnJwYy10ZXN0\"}";
    String requestUrl = "protocol.Wallet/GetAssetIssueListByName";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("owner_address"));
    Assert.assertTrue(returnString.contains("total_supply"));
    Assert.assertTrue(returnString.contains("description"));

    String requestUrlSolidity = "protocol.WalletSolidity/GetAssetIssueListByName";
    String returnStringSolidity = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, soliditynode);
    Assert.assertEquals(returnStringSolidity, returnString);
    String returnStringPBFT = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, pbftnode);
    Assert.assertEquals(returnStringPBFT, returnString);
  }

  @Test(enabled = true, description = "test GetAvailableUnfreezeCount")
  public void test032GetAvailableUnfreezeCount() {
    String ownerAddressBase64 = Base64.getEncoder().encodeToString(jsonRpcOwnerAddress);
    String data = String.format("{\"owner_address\":\"%s\"}", ownerAddressBase64);
    String requestUrl = "protocol.Wallet/GetAvailableUnfreezeCount";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("count"));
    Assert.assertTrue(returnString.contains("32"));

    String requestUrlSolidity = "protocol.WalletSolidity/GetAvailableUnfreezeCount";
    String returnStringSolidity = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, soliditynode);
    Assert.assertEquals(returnStringSolidity, returnString);
    String returnStringPBFT = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, pbftnode);
    Assert.assertEquals(returnStringPBFT, returnString);
  }

  @Test(enabled = true, description = "test GetBandwidthPrices")
  public void test033GetBandwidthPrices() {
    String requestUrl = "protocol.Wallet/GetBandwidthPrices";
    String returnString = PublicMethed.gRPCurlRequest(null, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("prices"));

    String requestUrlSolidity = "protocol.WalletSolidity/GetBandwidthPrices";
    String returnStringSolidity = PublicMethed.gRPCurlRequest(null, requestUrlSolidity, soliditynode);
    Assert.assertEquals(returnStringSolidity, returnString);
    String returnStringPBFT = PublicMethed.gRPCurlRequest(null, requestUrlSolidity, pbftnode);
    Assert.assertEquals(returnStringPBFT, returnString);
  }

  @Test(enabled = true, description = "test GetBlock")
  public void test034GetBlock() {
    String data = "{\"id_or_num\":\"1\",\"detail\":true}";
    String requestUrl = "protocol.Wallet/GetBlock";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("block_header"));
    Assert.assertTrue(returnString.contains("raw_data"));
    Assert.assertTrue(returnString.contains("witness_address"));

    String requestUrlSolidity = "protocol.WalletSolidity/GetBlock";
    String returnStringSolidity = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, soliditynode);
    Assert.assertEquals(returnStringSolidity, returnString);
    String returnStringPBFT = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, pbftnode);
    Assert.assertEquals(returnStringPBFT, returnString);
  }

  @Test(enabled = true, description = "test GetBlockBalanceTrace")
  public void test035GetBlockBalanceTrace() {
    Protocol.Block block =  PublicMethed.getBlock(2L, blockingStubFull);
    byte[] hash = block.getBlockHeader().getRawData().getParentHash().toByteArray();
    String hashBase64 = Base64.getEncoder().encodeToString(hash);
    String data = String.format("{\"number\":1,\"hash\":\"%s\"}",
        hashBase64
    );
    String requestUrl = "protocol.Wallet/GetBlockBalanceTrace";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("block_identifier"));
    Assert.assertTrue(returnString.contains("timestamp"));
  }

  @Test(enabled = true, description = "test GetBlockByLatestNum")
  public void test036GetBlockByLatestNum() {
    String data = "{\"num\":1}";
    String requestUrl = "protocol.Wallet/GetBlockByLatestNum";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    logger.info(returnString);
    Assert.assertNotNull(returnString);
    Assert.assertTrue(returnString.contains("block_header"));
    Assert.assertTrue(returnString.contains("witness_signature"));
    Assert.assertTrue(returnString.contains("number"));
  }

  @Test(enabled = true, description = "test GetBrokerageInfo")
  public void test037GetBrokerageInfo() {
    String ownerAddressBase64 = Base64.getEncoder().encodeToString(foundationAddress);
    String data = String.format("{\"value\":\"%s\"}", ownerAddressBase64) ;
    String requestUrl = "protocol.Wallet/GetBrokerageInfo";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("num"));
    Assert.assertTrue(returnString.contains("20"));

    String requestUrlSolidity = "protocol.WalletSolidity/GetBrokerageInfo";
    String returnStringSolidity = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, soliditynode);
    Assert.assertEquals(returnStringSolidity, returnString);
    String returnStringPBFT = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, pbftnode);
    Assert.assertEquals(returnStringPBFT, returnString);
  }

  @Test(enabled = true, description = "test GetBurnTrx")
  public void test038GetBurnTrx() {
    String requestUrl = "protocol.Wallet/GetBurnTrx";
    String returnString = PublicMethed.gRPCurlRequest(null, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("num"));
    JSONObject burnInfo = JSONObject.parseObject(returnString);
    Long burnNum = burnInfo.getLong("num");
    Assert.assertTrue(burnNum > 0L);

    String requestUrlSolidity = "protocol.WalletSolidity/GetBurnTrx";
    String returnStringSolidity = PublicMethed.gRPCurlRequest(null, requestUrlSolidity, soliditynode);
    Assert.assertTrue(returnStringSolidity.contains("num"));
    String returnStringPBFT = PublicMethed.gRPCurlRequest(null, requestUrlSolidity, pbftnode);
    Assert.assertTrue(returnStringPBFT.contains("num"));
  }

  @Test(enabled = true, description = "test GetCanDelegatedMaxSize")
  public void test039GetCanDelegatedMaxSize() {
    ECKey newAccount = new ECKey(Utils.getRandom());
    String newAccountBase64 = Base64.getEncoder().encodeToString(newAccount.getAddress());
    PublicMethed.sendcoin(newAccount.getAddress(), 100000000, foundationAddress, foundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.freezeBalanceV2(newAccount.getAddress(), 50000000,
        1, ByteArray.toHexString(newAccount.getPrivKeyBytes()),blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String data = String.format("{\"type\":\"1\",\"owner_address\":\"%s\"}", newAccountBase64);
    String requestUrl = "protocol.Wallet/GetCanDelegatedMaxSize";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("max_size"));
    Assert.assertTrue(returnString.contains("50000000"));

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String requestUrlSolidity = "protocol.WalletSolidity/GetCanDelegatedMaxSize";
    String returnStringSolidity = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, soliditynode);
    Assert.assertEquals(returnStringSolidity, returnString);
    String returnStringPBFT = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, pbftnode);
    Assert.assertEquals(returnStringPBFT, returnString);
  }


  @Test(enabled = true, description = "test GetCanWithdrawUnfreezeAmount")
  public void test040GetCanWithdrawUnfreezeAmount() {
    Long sendAmount = 100000000L;
    ECKey newAccount = new ECKey(Utils.getRandom());
    String newAccountBase64 = Base64.getEncoder().encodeToString(newAccount.getAddress());
    PublicMethed.sendcoin(newAccount.getAddress(), sendAmount, foundationAddress, foundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.freezeBalanceV2(newAccount.getAddress(), sendAmount/2,
        1, ByteArray.toHexString(newAccount.getPrivKeyBytes()),blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.unFreezeBalanceV2(newAccount.getAddress(),ByteArray.toHexString(newAccount.getPrivKeyBytes()),
        sendAmount/4,
        1,
        blockingStubFull
        );
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long queryTime = System.currentTimeMillis() + 1000000000L;
    String data = String.format("{\"timestamp\":\"%s\",\"owner_address\":\"%s\"}", queryTime, newAccountBase64);
    String requestUrl = "protocol.Wallet/GetCanWithdrawUnfreezeAmount";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("amount"));
    Assert.assertTrue(returnString.contains("25000000"));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String requestUrlSolidity = "protocol.WalletSolidity/GetCanWithdrawUnfreezeAmount";
    String returnStringSolidity = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, soliditynode);
    Assert.assertEquals(returnStringSolidity, returnString);
    String returnStringPBFT = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, pbftnode);
    Assert.assertEquals(returnStringPBFT, returnString);


    String requestUrl2 = "protocol.Wallet/UnfreezeBalanceV2";
    String data2 = String.format("{\"owner_address\":\"%s\",\"unfreeze_balance\":\"%d\",\"resource\":1}",
        newAccountBase64,
        sendAmount/4
    );
    String returnString2 = PublicMethed.gRPCurlRequest(data2, requestUrl2, fullnode);
    Assert.assertNotNull(returnString2);
    logger.info(returnString2);
    Assert.assertTrue(returnString2.contains("UnfreezeBalanceV2Contract"));
    Assert.assertTrue(returnString2.contains(String.valueOf(sendAmount/4)));
  }

  @Test(enabled = true, description = "test GetChainParameters")
  public void test041GetChainParameters() {
    String requestUrl = "protocol.Wallet/GetChainParameters";
    String returnString = PublicMethed.gRPCurlRequest(null, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("chainParameter"));
    Assert.assertTrue(returnString.contains("getUnfreezeDelayDays"));
  }

  @Test(enabled = true, description = "test GetDelegatedResourceAccountIndexV2")
  public void test042GetDelegatedResourceAccountIndexV2() {
    Long sendAmount = 100000000L;
    ECKey newAccount = new ECKey(Utils.getRandom());
    String newAccountBase64 = Base64.getEncoder().encodeToString(newAccount.getAddress());
    ECKey resourceReceiver = new ECKey(Utils.getRandom());
    String receiverAccountBase64 = Base64.getEncoder().encodeToString(resourceReceiver.getAddress());
    PublicMethed.sendcoin(newAccount.getAddress(), sendAmount, foundationAddress, foundationKey, blockingStubFull);
    PublicMethed.sendcoin(resourceReceiver.getAddress(), sendAmount, foundationAddress, foundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.freezeBalanceV2(newAccount.getAddress(), sendAmount/2,
        1, ByteArray.toHexString(newAccount.getPrivKeyBytes()),blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.delegateResourceV2(newAccount.getAddress(),
        sendAmount/4,
        1,
        resourceReceiver.getAddress(),
        ByteArray.toHexString(newAccount.getPrivKeyBytes()),
        blockingStubFull
        );
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String data = String.format("{\"value\":\"%s\"}", newAccountBase64);
    String requestUrl = "protocol.Wallet/GetDelegatedResourceAccountIndexV2";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains(receiverAccountBase64));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String requestUrlSolidity = "protocol.WalletSolidity/GetDelegatedResourceAccountIndexV2";
    String returnStringSolidity = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, soliditynode);
    Assert.assertEquals(returnStringSolidity, returnString);
    String returnStringPBFT = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, pbftnode);
    Assert.assertEquals(returnStringPBFT, returnString);

    String data2 = String.format("{\"fromAddress\":\"%s\",\"toAddress\":\"%s\"}", newAccountBase64, receiverAccountBase64);
    String requestUrl2 = "protocol.Wallet/GetDelegatedResourceV2";
    String returnString2 = PublicMethed.gRPCurlRequest(data2, requestUrl2, fullnode);
    Assert.assertNotNull(returnString2);
    logger.info(returnString2);
    Assert.assertTrue(returnString2.contains("frozen_balance_for_energy"));
    Assert.assertTrue(returnString2.contains("25000000"));

    requestUrlSolidity = "protocol.WalletSolidity/GetDelegatedResourceV2";
    returnStringSolidity = PublicMethed.gRPCurlRequest(data2, requestUrlSolidity, soliditynode);
    Assert.assertEquals(returnStringSolidity, returnString2);
    returnStringPBFT = PublicMethed.gRPCurlRequest(data2, requestUrlSolidity, pbftnode);
    Assert.assertEquals(returnStringPBFT, returnString2);

    String requestUrl3 = "protocol.Wallet/UnDelegateResource";
    String data3 = String.format("{\"owner_address\":\"%s\",\"resource\":\"ENERGY\",\"balance\":%d,\"receiver_address\":\"%s\"}",
        newAccountBase64,
        sendAmount/4,
        receiverAccountBase64
    );
    String returnString3 = PublicMethed.gRPCurlRequest(data3, requestUrl3, fullnode);
    Assert.assertNotNull(returnString3);
    logger.info(returnString3);
    Assert.assertTrue(returnString3.contains("UnDelegateResourceContract"));
    Assert.assertTrue(returnString3.contains("25000000"));
  }

  @Test(enabled = true, description = "test ParticipateAssetIssue")
  public void test043ParticipateAssetIssue() {
    String trc10OwnerBase64 = Base64.getEncoder().encodeToString(jsonRpcOwnerAddress);
    Protocol.Account accountInfo = PublicMethed.queryAccount(jsonRpcOwnerAddress, blockingStubFull);
    String assetNameBase64 = Base64.getEncoder().encodeToString(accountInfo.getAssetIssuedID().toByteArray());
    String partnerBase64 = Base64.getEncoder().encodeToString(foundationAddress);
    String requestUrl = "protocol.Wallet/ParticipateAssetIssue";
    String data = String.format("{\"owner_address\":\"%s\",\"to_address\":\"%s\",\"asset_name\":\"%s\",\"amount\":1}",
        partnerBase64,
        trc10OwnerBase64,
        assetNameBase64
        );
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("raw_data"));
    Assert.assertTrue(returnString.contains("asset_name"));
    Assert.assertTrue(returnString.contains("type.googleapis.com/protocol.ParticipateAssetIssueContract"));
  }


  @Test(enabled = true, description = "test ParticipateAssetIssue2")
  public void test044ParticipateAssetIssue2() {
    String trc10OwnerBase64 = Base64.getEncoder().encodeToString(jsonRpcOwnerAddress);
    Protocol.Account accountInfo = PublicMethed.queryAccount(jsonRpcOwnerAddress, blockingStubFull);
    String assetNameBase64 = Base64.getEncoder().encodeToString(accountInfo.getAssetIssuedID().toByteArray());
    String partnerBase64 = Base64.getEncoder().encodeToString(foundationAddress);
    String requestUrl = "protocol.Wallet/ParticipateAssetIssue2";
    String data = String.format("{\"owner_address\":\"%s\",\"to_address\":\"%s\",\"asset_name\":\"%s\",\"amount\":1}",
        partnerBase64,
        trc10OwnerBase64,
        assetNameBase64
    );
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("raw_data"));
    Assert.assertTrue(returnString.contains("asset_name"));
    Assert.assertTrue(returnString.contains("type.googleapis.com/protocol.ParticipateAssetIssueContract"));
  }

  @Test(enabled = true, description = "test TransferAsset")
  public void test045TransferAsset() {
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
    String requestUrl = "protocol.Wallet/TransferAsset";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("raw_data"));
    Assert.assertTrue(returnString.contains("TransferAssetContract"));
  }

  @Test(enabled = true, description = "test UnfreezeAsset")
  public void test046UnfreezeAsset() {
    String fromBase64 = Base64.getEncoder().encodeToString(jsonRpcOwnerAddress);
    Protocol.Account accountInfo = PublicMethed.queryAccount(jsonRpcOwnerAddress, blockingStubFull);
    String assetName = accountInfo.getAssetIssuedID().toStringUtf8();
    logger.info(assetName);
    String data = String.format(
        "{\"owner_address\":\"%s\"}",
        fromBase64
    );
    String requestUrl = "protocol.Wallet/UnfreezeAsset";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("raw_data"));
    Assert.assertTrue(returnString.contains("type.googleapis.com/protocol.UnfreezeAssetContract"));
  }

  @Test(enabled = true, description = "test UnfreezeAsset2")
  public void test047UnfreezeAsset2() {
    String fromBase64 = Base64.getEncoder().encodeToString(jsonRpcOwnerAddress);
    Protocol.Account accountInfo = PublicMethed.queryAccount(jsonRpcOwnerAddress, blockingStubFull);
    String assetName = accountInfo.getAssetIssuedID().toStringUtf8();
    logger.info(assetName);
    String data = String.format(
        "{\"owner_address\":\"%s\"}",
        fromBase64
    );
    String requestUrl = "protocol.Wallet/UnfreezeAsset2";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("transaction"));
    Assert.assertTrue(returnString.contains("raw_data"));
    Assert.assertTrue(returnString.contains("type.googleapis.com/protocol.UnfreezeAssetContract"));
  }

  @Test(enabled = true, description = "test UpdateAsset2")
  public void test048UpdateAsset2() {
    String ownerAddressBase64 = Base64.getEncoder().encodeToString(jsonRpcOwnerAddress);
    String data = String.format("{\"owner_address\":\"%s\",\"description\":\"dGVzdEdycGN1cmw=\",\"url\":\"d3d3LmNjLmNvbQ==\"}", ownerAddressBase64) ;
    String requestUrl = "protocol.Wallet/UpdateAsset2";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("transaction"));
    Assert.assertTrue(returnString.contains("type.googleapis.com/protocol.UpdateAssetContract"));
  }

  @Test(enabled = true, description = "test UpdateAsset")
  public void test049UpdateAsset() {
    String ownerAddressBase64 = Base64.getEncoder().encodeToString(jsonRpcOwnerAddress);
    String data = String.format("{\"owner_address\":\"%s\",\"description\":\"dGVzdEdycGN1cmw=\",\"url\":\"d3d3LmNjLmNvbQ==\"}", ownerAddressBase64) ;
    String requestUrl = "protocol.Wallet/UpdateAsset";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("type.googleapis.com/protocol.UpdateAssetContract"));
  }

}
