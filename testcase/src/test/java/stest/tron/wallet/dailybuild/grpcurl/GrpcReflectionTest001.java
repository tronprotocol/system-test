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
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Utils;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class GrpcReflectionTest001 {

  private final String foundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] foundationAddress = PublicMethed.getFinalAddress(foundationKey);

  private final String witnessKey = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] witnessAddress = PublicMethed.getFinalAddress(witnessKey);

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
  private String fullnode2 = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(1);
  private String contractBase64Address = null; //use for some cases query
  private String contractOwnerBase64Address = null;
  private String transferTxIdBase64 = null; //use for some cases query



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

  @Test(enabled = true, description = "test GetAccount use gRPCurl")
  public void test001GetAccount() {
    Long transferValue = 1000000000L;
    PublicMethed.sendcoin(receiverAddress, transferValue, foundationAddress, foundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String receiverAddressBase64 = Base64.getEncoder().encodeToString(receiverAddress);
    /*
      important！data can not contain space " " in json String
     */
    String data = String.format("{\"address\":\"%s\"}", receiverAddressBase64);
    String requestUrl = "protocol.Wallet/GetAccount";
    String returnData = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    JSONObject accountData = JSONObject.parseObject(returnData);
    Assert.assertEquals(accountData.getLong("balance").longValue(), transferValue.longValue());
  }

  @Test(enabled = true, description = "test getChainParameter use gRPCurl")
  public void test002GetChainParameters() {
    String requestUrl = "protocol.Wallet.GetChainParameters";
    String returnData = PublicMethed.gRPCurlRequest(null, requestUrl, fullnode);
    logger.info("parameters data: " + returnData);
    JSONObject parametersData = JSONObject.parseObject(returnData);
    Assert.assertTrue(parametersData.toJSONString().contains("getMaintenanceTimeInterval"));
  }

  @Test(enabled = true, description = "test getNowBlock use gRPCurl")
  public void test003GetNowBlock() {
    String requestUrl = "protocol.Wallet/GetNowBlock";
    String returnData = PublicMethed.gRPCurlRequest(null, requestUrl, fullnode);
    logger.info("block data : " + returnData);
    JSONObject parametersData = JSONObject.parseObject(returnData);
    Long blockId = parametersData.getJSONObject("block_header").getJSONObject("raw_data").getLong("number");
    Assert.assertTrue(blockId > 0);

    String requestUrlSolidity = "protocol.WalletSolidity/GetNowBlock";
    String returnDataSolidity = PublicMethed.gRPCurlRequest(null, requestUrlSolidity, soliditynode);
    logger.info("block data : " + returnDataSolidity);
    JSONObject parametersDataSolidity = JSONObject.parseObject(returnDataSolidity);
    Long blockIdSolidity = parametersDataSolidity.getJSONObject("block_header").getJSONObject("raw_data").getLong("number");
    Assert.assertTrue(blockIdSolidity > 0);

    String returnDataRealSolidity = PublicMethed.gRPCurlRequest(null, requestUrlSolidity, realSoliditynode);
    logger.info("block data : " + returnDataRealSolidity);
    JSONObject parametersDataRealSolidity = JSONObject.parseObject(returnDataRealSolidity);
    Long blockIdRealSolidity = parametersDataRealSolidity.getJSONObject("block_header").getJSONObject("raw_data").getLong("number");
    Assert.assertTrue(blockIdRealSolidity > 0);

    String returnDataPbft = PublicMethed.gRPCurlRequest(null, requestUrlSolidity, pbftnode);
    logger.info("block data : " + returnDataPbft);
    JSONObject parametersDataPbft = JSONObject.parseObject(returnDataPbft);
    Long blockIdPbft = parametersDataPbft.getJSONObject("block_header").getJSONObject("raw_data").getLong("number");
    Assert.assertTrue(blockIdPbft > 0);
  }

  @Test(enabled = true, description = "test TriggerConstantContract use gRPCurl")
  public void test004TriggerConstantContract() {
    ECKey deployer = new ECKey(Utils.getRandom());
    String deployerKey = ByteArray.toHexString(deployer.getPrivKeyBytes());
    PublicMethed.sendcoin(deployer.getAddress(), 1000000000L, foundationAddress,
        foundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String abi = "[{\"inputs\":[{\"internalType\":\"uint256\",\"name\":\"value\","
        + "\"type\":\"uint256\"}],\"name\":\"foo\",\"outputs\":[{\"internalType\":\"uint256\","
        + "\"name\":\"\",\"type\":\"uint256\"}],\"stateMutability\""
        + ":\"nonpayable\",\"type\":\"function\"}]";
    String code = "608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a5760"
        + "0080fd5b5060b8806100396000396000f3fe6080604052348015600f57600080fd5b50d38015601b576000"
        + "80fd5b50d28015602757600080fd5b506004361060405760003560e01c80632fbebd38146045575b600080f"
        + "d5b60586050366004606a565b600081905590565b60405190815260200160405180910390f35b60006020828"
        + "4031215607b57600080fd5b503591905056fea26474726f6e58221220c8669715fe95381ee315e1119"
        + "064ff80a8121ddfc52747a55161d0d4f4dda2a564736f6c63430008120033";
    byte[] contractAddress = PublicMethed.deployContract(
        "MyContract", abi, code,"", maxFeeLimit, 0L,
        100, null, deployerKey,
        deployer.getAddress(), blockingStubFull
    );
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String contractAddressBase64 = Base64.getEncoder().encodeToString(contractAddress);
    contractBase64Address = contractAddressBase64; // for other cases easily test
    String ownerAddressBase64 = Base64.getEncoder().encodeToString(deployer.getAddress());
    contractOwnerBase64Address = ownerAddressBase64;
    String data = String
        .format(
            "{\"owner_address\":\"%s\",\"contract_address\":\"%s\",\"data\":\"aH2qOgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB\"}",
            ownerAddressBase64, contractAddressBase64);
    String requestUrl = "protocol.Wallet/TriggerConstantContract";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    logger.info(returnString);
    JSONObject triggerResult = JSONObject.parseObject(returnString).getJSONObject("result");
    Assert.assertTrue(triggerResult.getBoolean("result"));
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSolidity);
    String requestUrlSolidity = "protocol.WalletSolidity/TriggerConstantContract";
    String returnStringSolidity = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, soliditynode);
    triggerResult = JSONObject.parseObject(returnStringSolidity).getJSONObject("result");
    Assert.assertTrue(triggerResult.getBoolean("result"));
    String returnStringPBFT = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, pbftnode);
    triggerResult = JSONObject.parseObject(returnStringPBFT).getJSONObject("result");
    Assert.assertTrue(triggerResult.getBoolean("result"));
  }

  @Test(enabled = true, description = "test getTransactionById")
  public void test005GetTransactionById() {
    ECKey newAccount = new ECKey(Utils.getRandom());
    String txId = PublicMethed
        .sendcoinGetTransactionId(newAccount.getAddress(),
            10000000L, foundationAddress, foundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String requestUrl = "protocol.Wallet/GetTransactionById";
    String txIdBase64 = Base64.getEncoder().encodeToString(ByteArray.fromHexString(txId));
    transferTxIdBase64 = txIdBase64;
    String data = String.format("{\"value\":\"%s\"}", txIdBase64);
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    JSONObject txData = JSONObject.parseObject(returnString);
    logger.info(txData.toJSONString());
    Assert.assertEquals(
        txData.getJSONArray("ret")
            .getJSONObject(0)
            .getString("contractRet"),
        "SUCCESS");
    // query solidity
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String requestUrlSolidity = "protocol.WalletSolidity/GetTransactionById";
    String returnStringSolidity = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, soliditynode);
    JSONObject txDataSolidity = JSONObject.parseObject(returnStringSolidity);
    logger.info(txDataSolidity.toJSONString());
    Assert.assertEquals(
        txDataSolidity.getJSONArray("ret")
            .getJSONObject(0)
            .getString("contractRet"),
        "SUCCESS");
    String returnStringPBFT = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, pbftnode);
    Assert.assertEquals(returnStringPBFT, returnStringSolidity);
  }

  @Test(enabled = true, description = "test getContract ")
  public void test006GetContract() {
    String data = String.format("{\"value\":\"%s\"}", contractBase64Address);
    String requestUrl = "protocol.Wallet/GetContract";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    JSONObject contractData = JSONObject.parseObject(returnString);
    logger.info(contractData.toJSONString());
    Assert.assertTrue(contractData.getString("contract_address").equals(contractBase64Address));
  }

  @Test(enabled = true, description = "test getBlock2")
  public void test007GetNowBlock2() {
    String requestUrl = "protocol.Wallet/GetNowBlock2";
    String returnString = PublicMethed.gRPCurlRequest(null, requestUrl, fullnode);
    JSONObject blockData = JSONObject.parseObject(returnString);
    logger.info(blockData.toJSONString());
    Long blockId = blockData.getJSONObject("block_header").getJSONObject("raw_data").getLong("number");
    Assert.assertTrue(blockId > 0);

    String requestUrlSolidity = "protocol.WalletSolidity/GetNowBlock2";
    String returnStringSolidity = PublicMethed.gRPCurlRequest(null, requestUrlSolidity, soliditynode);
    blockData = JSONObject.parseObject(returnStringSolidity);
    logger.info(blockData.toJSONString());
    blockId = blockData.getJSONObject("block_header").getJSONObject("raw_data").getLong("number");
    Assert.assertTrue(blockId > 0);

    String returnStringPBFT = PublicMethed.gRPCurlRequest(null, requestUrlSolidity, pbftnode);
    blockData = JSONObject.parseObject(returnStringPBFT);
    logger.info(blockData.toJSONString());
    blockId = blockData.getJSONObject("block_header").getJSONObject("raw_data").getLong("number");
    Assert.assertTrue(blockId > 0);
  }

  @Test(enabled = true, description = "test GetBlockByNum")
  public void test008GetBlockByNum() {
    String data = "{\"num\":1}";
    String requestUrl = "protocol.Wallet/GetBlockByNum";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    JSONObject blockData = JSONObject.parseObject(returnString);
    logger.info(blockData.toJSONString());
    Long blockId = blockData.getJSONObject("block_header").getJSONObject("raw_data").getLong("number");
    Assert.assertEquals(blockId.longValue(), 1L);
  }

  @Test(enabled = true, description = "test GetBlockByNum2")
  public void test008GetBlockByNum2() {
    String data = "{\"num\":1}";
    String requestUrl = "protocol.Wallet/GetBlockByNum2";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    JSONObject blockData = JSONObject.parseObject(returnString);
    logger.info(blockData.toJSONString());
    Long blockId = blockData.getJSONObject("block_header").getJSONObject("raw_data").getLong("number");
    Assert.assertEquals(blockId.longValue(), 1L);

    String requestUrlSolidity = "protocol.WalletSolidity/GetBlockByNum2";
    String returnStringSolidity = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, soliditynode);
    JSONObject blockDataSolidity = JSONObject.parseObject(returnStringSolidity);
    Long blockIdSolidity = blockDataSolidity.getJSONObject("block_header").getJSONObject("raw_data").getLong("number");
    Assert.assertEquals(blockIdSolidity.longValue(), 1L);
    String returnStringPBFT = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, pbftnode);
    Assert.assertEquals(returnStringPBFT, returnString);
  }

  @Test(enabled = true, description = "test getNodeInfo")
  public void test009GetNodeInfo() {
    String requestUrl = "protocol.Wallet/GetNodeInfo";
    String returnString = PublicMethed.gRPCurlRequest(null, requestUrl, fullnode);
    JSONObject nodeData = JSONObject.parseObject(returnString);
    logger.info(nodeData.toJSONString());
    Assert.assertNotNull(nodeData);
    Assert.assertTrue(nodeData.getLongValue("currentConnectCount") > 0L);
  }

  @Test(enabled = true, description = "test GetBlockByLimitNext2")
  public void test010GetBlockByLimitNext2() {
    String requestUrl = "protocol.Wallet/GetBlockByLimitNext2";
    String data = "{\"startNum\":0,\"endNum\":1}";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    JSONObject blockData = JSONObject.parseObject(returnString);
    logger.info(blockData.toJSONString());
    JSONArray blocks = blockData.getJSONArray("block");
    Assert.assertTrue(blocks.size() > 0L);
  }

  @Test(enabled = true, description = "test GetTransactionInfoById")
  public void test011GetTransactionInfoById() {
    String requestUrl = "protocol.Wallet/GetTransactionInfoById";
    String data = String.format("{\"value\":\"%s\"}", transferTxIdBase64);
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    JSONObject txData = JSONObject.parseObject(returnString);
    logger.info(txData.toJSONString());
    Assert.assertEquals(txData.getString("id"), transferTxIdBase64);

    String requestUrlSolidity = "protocol.WalletSolidity/GetTransactionInfoById";
    String returnStringSolidity = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, soliditynode);
    Assert.assertEquals(returnStringSolidity, returnString);
    String returnStringPBFT = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, pbftnode);
    Assert.assertEquals(returnStringPBFT, returnString);
  }

  @Test(enabled = true, description = "test CreateTransaction2")
  public void test012CreateTransaction2() {
    ECKey newAccount = new ECKey(Utils.getRandom());
    String requestUrl = "protocol.Wallet/CreateTransaction2";
    String from = Base64.getEncoder().encodeToString(foundationAddress);
    String to = Base64.getEncoder().encodeToString(newAccount.getAddress());
    String data = String
        .format("{\"owner_address\":\"%s\",\"to_address\":\"%s\",\"amount\":100000000}",
            from,
            to);
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    JSONObject txData = JSONObject.parseObject(returnString);
    logger.info(txData.toJSONString());
    Assert.assertTrue(txData.getJSONObject("result").getBoolean("result"));
  }

  @Test(enabled = true, description = "test TriggerContract")
  public void test013TriggerContract() {
    ECKey owner = new ECKey(Utils.getRandom());
    String ownerAddressBase64 = Base64.getEncoder().encodeToString(owner.getAddress());
    String data = String
        .format(
            "{\"owner_address\":\"%s\",\"contract_address\":\"%s\",\"data\":\"aH2qOgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB\"}",
            ownerAddressBase64, contractBase64Address);
    String requestUrl = "protocol.Wallet/TriggerContract";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info("tx is :" + returnString);
    Assert.assertTrue(returnString.contains("transaction"));
  }

  @Test(enabled = true, description = "test ClearContractABI")
  public void test014ClearContractABI(){
    String data = String.format("{\"owner_address\":\"%s\",\"contract_address\":\"%s\"}",contractOwnerBase64Address, contractBase64Address);
    String requestUrl = "protocol.Wallet/ClearContractABI";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("transaction"));
    Assert.assertTrue(returnString.contains("ref_block_bytes"));
    Assert.assertTrue(returnString.contains("expiration"));
    Assert.assertTrue(returnString.contains("raw_data"));
  }

  @Test(enabled = true, description = "test CreateTransaction")
  public void test015CreateTransaction() {
    ECKey newAccount = new ECKey(Utils.getRandom());
    String requestUrl = "protocol.Wallet/CreateTransaction";
    String from = Base64.getEncoder().encodeToString(foundationAddress);
    String to = Base64.getEncoder().encodeToString(newAccount.getAddress());
    String data = String
        .format("{\"owner_address\":\"%s\",\"to_address\":\"%s\",\"amount\":100000000}",
            from,
            to);
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    JSONObject txData = JSONObject.parseObject(returnString);
    logger.info(txData.toJSONString());
    Assert.assertTrue(returnString.contains("ref_block_bytes"));
    Assert.assertTrue(returnString.contains("expiration"));
    Assert.assertTrue(returnString.contains("raw_data"));
  }

  @Test(enabled = true, description = "test EstimateEnergy")
  public void test016EstimateEnergy() {
    String data = String
        .format(
            "{\"owner_address\":\"%s\",\"contract_address\":\"%s\",\"data\":\"aH2qOgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB\"}",
            contractOwnerBase64Address, contractBase64Address);
    String requestUrl = "protocol.Wallet/EstimateEnergy";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode2);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("CONTRACT_EXE_ERROR"));
    Assert.assertTrue(returnString.contains("UkVWRVJUIG9wY29kZSBleGVjdXRlZA=="));// REVERT
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSolidity);
    String requestUrlSolidity = "protocol.WalletSolidity/EstimateEnergy";
    String returnStringSolidity = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, soliditynode);
    Assert.assertTrue(returnStringSolidity.contains("" +
        "Q29udHJhY3QgdmFsaWRhdGUgZXJyb3IgOiB0aGlzIG5vZGUgZG9lcyBub3Qgc3VwcG9ydCBlc3RpbWF0ZSBlbmVyZ3k="));
    // this node does not support estimateEnergy
    String returnStringPBFT = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, pbftnode);
    Assert.assertEquals(returnStringPBFT, returnString);

  }

  @Test(enabled = true, description = "test GetContractInfo")
  public void test017GetContractInfo() {
    String data = String.format("{\"value\":\"%s\"}", contractBase64Address);
    String requestUrl = "protocol.Wallet/GetContractInfo";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("smart_contract"));
    Assert.assertTrue(returnString.contains("contract_state"));
  }

  @Test(enabled = true, description = "test GetEnergyPrices")
  public void test018GetGetEnergyPrices() {
    String requestUrl = "protocol.Wallet/GetEnergyPrices";
    String returnString = PublicMethed.gRPCurlRequest(null, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("prices"));
    Assert.assertTrue(returnString.contains("420"));

    String requestUrlSolidity = "protocol.WalletSolidity/GetEnergyPrices";
    String returnStringSolidity = PublicMethed.gRPCurlRequest(null, requestUrlSolidity, soliditynode);
    Assert.assertEquals(returnStringSolidity, returnString);
    String returnStringPBFT = PublicMethed.gRPCurlRequest(null, requestUrlSolidity, pbftnode);
    Assert.assertEquals(returnStringPBFT, returnString);
  }

  @Test(enabled = true, description = "test GetMemoFee")
  public void test019GetMemoFee() {
    String requestUrl = "protocol.Wallet/GetMemoFee";
    String returnString = PublicMethed.gRPCurlRequest(null, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("prices"));
  }

  @Test(enabled = true, description = "test GetNextMaintenanceTime")
  public void test020GetNextMaintenanceTime() {
    String requestUrl = "protocol.Wallet/GetNextMaintenanceTime";
    String returnString = PublicMethed.gRPCurlRequest(null, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("num"));
    JSONObject timeInfo = JSONObject.parseObject(returnString);
    Long time = timeInfo.getLong("num");
    Assert.assertTrue(time > System.currentTimeMillis());
  }

  @Test(enabled = true, description = "test GetPaginatedAssetIssueList")
  public void test021GetPaginatedAssetIssueList() {
    String data = "{\"offset\":0,\"limit\":10}";
    String requestUrl = "protocol.Wallet/GetPaginatedAssetIssueList";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("assetIssue"));
    Assert.assertTrue(returnString.contains("total_supply"));

    String requestUrlSolidity = "protocol.WalletSolidity/GetPaginatedAssetIssueList";
    String returnStringSolidity = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, soliditynode);
    logger.info(returnStringSolidity);
    Assert.assertTrue(returnStringSolidity.contains("assetIssue"));
    Assert.assertTrue(returnStringSolidity.contains("total_supply"));
    String returnStringPBFT = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, pbftnode);
    logger.info(returnStringPBFT);
    Assert.assertTrue(returnStringPBFT.contains("assetIssue"));
    Assert.assertTrue(returnStringPBFT.contains("total_supply"));
  }

  @Test(enabled = true, description = "test GetPaginatedProposalList")
  public void test022GetPaginatedProposalList() {
    String data = "{\"offset\":0,\"limit\":10}";
    String requestUrl = "protocol.Wallet/GetPaginatedProposalList";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("proposals"));
    Assert.assertTrue(returnString.contains("proposal_id"));
    Assert.assertTrue(returnString.contains("parameters"));
  }

  @Test(enabled = true, description = "test GetPendingSize")
  public void test023GetPendingSize() {
    String requestUrl = "protocol.Wallet/GetPendingSize";
    String returnString = PublicMethed.gRPCurlRequest(null, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.startsWith("{"));
  }

  @Test(enabled = true, description = "test GetRewardInfo")
  public void test023GetRewardInfo() {
    Optional<GrpcAPI.WitnessList> witnesses = PublicMethed.listWitnesses(blockingStubFull);
    byte[] witnessAddress = witnesses.get().getWitnessesList().get(0).getAddress().toByteArray();
    String ownerAddressBase64 = Base64.getEncoder().encodeToString(witnessAddress);
    String data = String.format("{\"value\":\"%s\"}", ownerAddressBase64);
    String requestUrl = "protocol.Wallet/GetRewardInfo";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("num"));
    JSONObject rewardData = JSONObject.parseObject(returnString);
    Assert.assertTrue(rewardData.getLongValue("num") > 0);

    String requestUrlSolidity = "protocol.WalletSolidity/GetRewardInfo";
    String returnStringSolidity = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, soliditynode);
    logger.info(returnStringSolidity);
    Assert.assertTrue(returnStringSolidity.contains("num"));
    rewardData = JSONObject.parseObject(returnStringSolidity);
    Assert.assertTrue(rewardData.getLongValue("num") > 0);
    String returnStringPBFT = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, pbftnode);
    rewardData = JSONObject.parseObject(returnStringPBFT);
    Assert.assertTrue(rewardData.getLongValue("num") > 0);
  }

  @Test(enabled = true, description = "test GetTransactionApprovedList")
  public void test024GetTransactionApprovedList() {
    String requestUrl = "protocol.Wallet/GetTransactionById";
    String data = String.format("{\"value\":\"%s\"}", transferTxIdBase64);
    String trxData = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(trxData);
    JSONObject returnTrx = JSONObject.parseObject(trxData);
    requestUrl = "protocol.Wallet/GetTransactionApprovedList";
    String returnString = PublicMethed.gRPCurlRequest(returnTrx.toJSONString(), requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("approved_list"));
  }


  @Test(enabled = true, description = "test GetTransactionFromPending")
  public void test025GetTransactionFromPending() {
    String requestUrl = "protocol.Wallet/GetTransactionFromPending";
    String data = String.format("{\"value\":\"%s\"}", "K0bj5EjrlNB8BNWh1Jmg/Bybke87zZJCIJmu8Zwz9xg=");
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.startsWith("{"));
  }

  @Test(enabled = true, description = "test GetTransactionCountByBlockNum")
  public void test026GetTransactionCountByBlockNum() {
    String requestUrl = "protocol.Wallet/GetTransactionCountByBlockNum";
    String data = String.format("{\"num\":\"%d\"}", 0);
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("num"));

    String requestUrlSolidity = "protocol.WalletSolidity/GetTransactionCountByBlockNum";
    String returnStringSolidity = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, soliditynode);
    Assert.assertTrue(returnStringSolidity.contains("num"));
    String returnStringPBFT = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, pbftnode);
    Assert.assertTrue(returnStringPBFT.contains("num"));

  }

  @Test(enabled = true, description = "test GetTransactionInfoByBlockNum")
  public void test027GetTransactionInfoByBlockNum() {
    String requestUrl = "protocol.Wallet/GetTransactionInfoById";
    String data = String.format("{\"value\":\"%s\"}", transferTxIdBase64);
    String trxData = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    JSONObject trx = JSONObject.parseObject(trxData);
    logger.info(trxData);
    Long blockNumber = trx.getLongValue("blockNumber");
    requestUrl = "protocol.Wallet/GetTransactionInfoByBlockNum";
    data = String.format("{\"num\":\"%d\"}", blockNumber);
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("transactionInfo"));
    Assert.assertTrue(returnString.contains(blockNumber.toString()));

    String requestUrlSolidity = "protocol.WalletSolidity/GetTransactionInfoByBlockNum";
    String returnStringSolidity = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, soliditynode);
    logger.info(returnStringSolidity);
    Assert.assertTrue(returnStringSolidity.contains("transactionInfo"));
//    String returnStringPBFT = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, pbftnode);
//    Assert.assertEquals(returnStringPBFT, returnString);
  }

  @Test(enabled = true, description = "test GetTransactionSignWeight")
  public void test028GetTransactionSignWeight() {
    String requestUrl = "protocol.Wallet/GetTransactionById";
    String data = String.format("{\"value\":\"%s\"}", transferTxIdBase64);
    String trxData = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(trxData);
    JSONObject returnTrx = JSONObject.parseObject(trxData);
    requestUrl = "protocol.Wallet/GetTransactionSignWeight";
    String returnString = PublicMethed.gRPCurlRequest(returnTrx.toJSONString(), requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("current_weight"));
    Assert.assertTrue(returnString.contains("permission"));
    JSONObject weightData = JSONObject.parseObject(returnString);
    Assert.assertEquals(weightData.getLongValue("current_weight"), 1L);
  }


  @Test(enabled = true, description = "test ListProposals")
  public void test029ListProposals() {
    String requestUrl = "protocol.Wallet/ListProposals";
    String returnString = PublicMethed.gRPCurlRequest(null, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("proposals"));
    Assert.assertTrue(returnString.contains("APPROVED"));
  }


  @Test(enabled = true, description = "test ListWitnesses")
  public void test030ListWitnesses() {
    String requestUrl = "protocol.Wallet/ListWitnesses";
    String returnString = PublicMethed.gRPCurlRequest(null, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("witnesses"));
    Assert.assertTrue(returnString.contains("voteCount"));
    Assert.assertTrue(returnString.contains("totalProduced"));

    String requestUrlSolidity = "protocol.WalletSolidity/ListWitnesses";
    String returnStringSolidity = PublicMethed.gRPCurlRequest(null, requestUrlSolidity, soliditynode);
    Assert.assertTrue(returnStringSolidity.contains("witnesses"));
    Assert.assertTrue(returnStringSolidity.contains("voteCount"));
    Assert.assertTrue(returnStringSolidity.contains("totalProduced"));
    String returnStringPBFT = PublicMethed.gRPCurlRequest(null, requestUrlSolidity, pbftnode);
    Assert.assertTrue(returnStringPBFT.contains("witnesses"));
    Assert.assertTrue(returnStringPBFT.contains("voteCount"));
    Assert.assertTrue(returnStringPBFT.contains("totalProduced"));
  }


  @Test(enabled = true, description = "test ProposalCreate")
  public void test031ProposalCreate() {
    String ownerAddressBase64 = Base64.getEncoder().encodeToString(witnessAddress);
    String requestUrl = "protocol.Wallet/ProposalCreate";
    String data =String.format("{\"owner_address\":\"%s\",\"parameters\":{\"62\":43200000000}}", ownerAddressBase64)  ;
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("type.googleapis.com/protocol.ProposalCreateContract"));
    Assert.assertTrue(returnString.contains("43200000000"));
  }


  @Test(enabled = true, description = "test ProposalApprove")
  public void test031ProposalApprove() {
    HashMap<Long, Long> proposalMap = new HashMap();
    proposalMap.put(62L, 43200000000L);
    PublicMethed.createProposal(witnessAddress, witnessKey, proposalMap, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    GrpcAPI.ProposalList proposalList = blockingStubFull.listProposals(GrpcAPI.EmptyMessage.newBuilder().build());
    Long newProposalId = proposalList.getProposals(0).getProposalId();
    String ownerAddressBase64 = Base64.getEncoder().encodeToString(witnessAddress);
    String requestUrl = "protocol.Wallet/ProposalApprove";
    String data =String.format("{\"owner_address\":\"%s\",\"proposal_id\":\"%d\",\"is_add_approval\":true}",
        ownerAddressBase64,
        newProposalId
        );
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("type.googleapis.com/protocol.ProposalApproveContract"));
    Assert.assertTrue(returnString.contains("is_add_approval"));
  }

  @Test(enabled = true, description = "test ProposalDelete")
  public void test032ProposalDelete() {
    GrpcAPI.ProposalList proposalList = blockingStubFull.listProposals(GrpcAPI.EmptyMessage.newBuilder().build());
    Long newProposalId = proposalList.getProposals(0).getProposalId();
    String ownerAddressBase64 = Base64.getEncoder().encodeToString(witnessAddress);
    String requestUrl = "protocol.Wallet/ProposalDelete";
    String data =String.format("{\"owner_address\":\"%s\",\"proposal_id\":\"%d\"}",
        ownerAddressBase64,
        newProposalId
    );
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("type.googleapis.com/protocol.ProposalDeleteContract"));
    Assert.assertTrue(returnString.contains("proposal_id"));
  }


  @Test(enabled = true, description = "test SetAccountId")
  public void test033SetAccountId() {
    String ownerAddressBase64 = Base64.getEncoder().encodeToString(foundationAddress);
    String requestUrl = "protocol.Wallet/SetAccountId";
    String data =String.format("{\"owner_address\":\"%s\",\"account_id\":\"dGVzdEdycGN1cmw=\"}",
        ownerAddressBase64
    );
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("type.googleapis.com/protocol.SetAccountIdContract"));
    Assert.assertTrue(returnString.contains("raw_data"));
  }

  @Test(enabled = true, description = "test UpdateAccount2")
  public void test034UpdateAccount2() {
    ECKey newAccount = new ECKey(Utils.getRandom());
    PublicMethed.sendcoin(newAccount.getAddress(), 10000000, foundationAddress,foundationKey,blockingStubFull);
    String ownerAddressBase64 = Base64.getEncoder().encodeToString(newAccount.getAddress());
    String data = String.format("{\"owner_address\":\"%s\",\"account_name\":\"Y2NjdGVzdA==\"}", ownerAddressBase64) ;
    String requestUrl = "protocol.Wallet/UpdateAccount2";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("transaction"));
    Assert.assertTrue(returnString.contains("type.googleapis.com/protocol.AccountUpdateContract"));

    String requestUrl2 = "protocol.Wallet/UpdateAccount";
    String returnString2 = PublicMethed.gRPCurlRequest(data, requestUrl2, fullnode);
    Assert.assertNotNull(returnString2);
    logger.info(returnString2);
    Assert.assertTrue(returnString2.contains("type.googleapis.com/protocol.AccountUpdateContract"));
  }

  @Test(enabled = true, description = "test UpdateAccount")
  public void test035UpdateAccount() {
    ECKey newAccount = new ECKey(Utils.getRandom());
    PublicMethed.sendcoin(newAccount.getAddress(), 10000000, foundationAddress,foundationKey,blockingStubFull);
    String ownerAddressBase64 = Base64.getEncoder().encodeToString(newAccount.getAddress());
    String data = String.format("{\"owner_address\":\"%s\",\"account_name\":\"Y2NjdGVzdA==\"}", ownerAddressBase64) ;
    String requestUrl = "protocol.Wallet/UpdateAccount";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("type.googleapis.com/protocol.AccountUpdateContract"));
  }


  @Test(enabled = true, description = "test UpdateBrokerage")
  public void test036UpdateBrokerage() {
    String ownerAddressBase64 = Base64.getEncoder().encodeToString(witnessAddress);
    String data = String.format("{\"owner_address\":\"%s\",\"brokerage\":1}", ownerAddressBase64) ;
    String requestUrl = "protocol.Wallet/UpdateBrokerage";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("transaction"));
    Assert.assertTrue(returnString.contains("type.googleapis.com/protocol.UpdateBrokerageContract"));
  }


  @Test(enabled = true, description = "test UpdateEnergyLimit")
  public void test037UpdateEnergyLimit() {
    String data = String.format("{\"owner_address\":\"%s\",\"contract_address\":\"%s\",\"origin_energy_limit\":1000}",
        contractOwnerBase64Address,
        contractBase64Address
        );
    String requestUrl = "protocol.Wallet/UpdateEnergyLimit";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("transaction"));
    Assert.assertTrue(returnString.contains("type.googleapis.com/protocol.UpdateEnergyLimitContract"));
  }

  @Test(enabled = true, description = "test UpdateSetting")
  public void test038UpdateSetting() {
    String data = String.format("{\"owner_address\":\"%s\",\"contract_address\":\"%s\",\"consume_user_resource_percent\":37}",
        contractOwnerBase64Address,
        contractBase64Address
    );
    String requestUrl = "protocol.Wallet/UpdateSetting";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("transaction"));
    Assert.assertTrue(returnString.contains("type.googleapis.com/protocol.UpdateSettingContract"));
  }

  @Test(enabled = true, description = "test UpdateWitness")
  public void test039UpdateWitness() {
    String ownerAddressBase64 = Base64.getEncoder().encodeToString(witnessAddress);
    String data = String.format("{\"owner_address\":\"%s\",\"update_url\":\"d3d3LmNjLmNvbQ==\"}",
        ownerAddressBase64
    );
    String requestUrl = "protocol.Wallet/UpdateWitness";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("type.googleapis.com/protocol.WitnessUpdateContract"));
    Assert.assertTrue(returnString.contains("update_url"));
  }

  @Test(enabled = true, description = "test UpdateWitness2")
  public void test040UpdateWitness2() {
    String ownerAddressBase64 = Base64.getEncoder().encodeToString(witnessAddress);
    String data = String.format("{\"owner_address\":\"%s\",\"update_url\":\"d3d3LmNjLmNvbQ==\"}",
        ownerAddressBase64
    );
    String requestUrl = "protocol.Wallet/UpdateWitness2";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("transaction"));
    Assert.assertTrue(returnString.contains("type.googleapis.com/protocol.WitnessUpdateContract"));
  }

  @Test(enabled = true, description = "test WithdrawBalance2")
  public void test041WithdrawBalance2() {
    String ownerAddressBase64 = Base64.getEncoder().encodeToString(witnessAddress);
    String data = String.format("{\"owner_address\":\"%s\"}",
        ownerAddressBase64
    );
    String requestUrl = "protocol.Wallet/WithdrawBalance2";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains(
        "Q29udHJhY3QgdmFsaWRhdGUgZXJyb3IgOiBBY2NvdW50WzQxMGJlODhhOTE4ZDc0ZDBkZ" +
            "mQ3MWRjODRiZDRhYmYwMzZkMDU2Mjk5MV0gaXMgYSBndWFyZCByZXByZXNlbnRhdGl2" +
            "ZSBhbmQgaXMgbm90IGFsbG93ZWQgdG8gd2l0aGRyYXcgQmFsYW5jZQ"));
  }

  @Test(enabled = true, description = "test WithdrawBalance")
  public void test042WithdrawBalance() {
    String ownerAddressBase64 = Base64.getEncoder().encodeToString(witnessAddress);
    String data = String.format("{\"owner_address\":\"%s\"}",
        ownerAddressBase64
    );
    String requestUrl = "protocol.Wallet/WithdrawBalance";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("{}"));
  }

  @Test(enabled = true, description = "test WithdrawExpireUnfreeze")
  public void test043WithdrawExpireUnfreeze() {
    String ownerAddressBase64 = Base64.getEncoder().encodeToString(foundationAddress);
    String data = String.format("{\"owner_address\":\"%s\"}",
        ownerAddressBase64
    );
    String requestUrl = "protocol.Wallet/WithdrawExpireUnfreeze";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("CONTRACT_VALIDATE_ERROR"));
    Assert.assertTrue(returnString.contains(
        "Q29udHJhY3QgdmFsaWRhdGUgZXJyb3IgOiBubyB1bkZyZWV6ZSBiYWxhbmNlIHRvIHdpdGhkcmF3IA=="));
  }

  @Test(enabled = true, description = "test VoteWitnessAccount2")
  public void test044VoteWitnessAccount2() {
    ECKey newAccount = new ECKey(Utils.getRandom());
    PublicMethed.sendcoin(newAccount.getAddress(), 100000000L, foundationAddress, foundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.freezeBalanceV2(newAccount.getAddress(), 100000000L, 1,
        ByteArray.toHexString(newAccount.getPrivKeyBytes()),
        blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String ownerAddressBase64 = Base64.getEncoder().encodeToString(newAccount.getAddress());
    String voteAddressBase64 = Base64.getEncoder().encodeToString(witnessAddress);

    String data = String.format("{\"owner_address\":\"%s\",\"votes\":[{\"vote_address\":\"%s\",\"vote_count\":1}]}",
        ownerAddressBase64,
        voteAddressBase64
    );
    String requestUrl = "protocol.Wallet/VoteWitnessAccount2";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(returnString.contains("transaction"));
    Assert.assertTrue(returnString.contains("type.googleapis.com/protocol.VoteWitnessContract"));

    requestUrl = "protocol.Wallet/VoteWitnessAccount";
    returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    logger.info(returnString);
    Assert.assertTrue(!returnString.contains("transaction"));
    Assert.assertTrue(returnString.contains("type.googleapis.com/protocol.VoteWitnessContract"));
  }

  @Test(enabled = true, description = "test Database.GetBlockByNum")
  public void test045GetBlockByNum() {
    String data = "{\"num\":1}";
    String requestUrl = "protocol.Database/GetBlockByNum";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    JSONObject blockData = JSONObject.parseObject(returnString);
    logger.info(blockData.toJSONString());
    Long blockId = blockData.getJSONObject("block_header").getJSONObject("raw_data").getLong("number");
    Assert.assertEquals(blockId.longValue(), 1L);
  }

  @Test(enabled = true, description = "test Database.GetNowBlock")
  public void test046GetNowBlock() {
    String requestUrl = "protocol.Database/GetNowBlock";
    String returnString = PublicMethed.gRPCurlRequest(null, requestUrl, fullnode);
    JSONObject blockData = JSONObject.parseObject(returnString);
    logger.info(blockData.toJSONString());
    Long blockId = blockData.getJSONObject("block_header").getJSONObject("raw_data").getLong("number");
    Assert.assertTrue(blockId.longValue() > 0L);
  }

  @Test(enabled = true, description = "test Database.GetDynamicProperties")
  public void test047GetDynamicProperties() {
    String requestUrl = "protocol.Database/GetDynamicProperties";
    String returnString = PublicMethed.gRPCurlRequest(null, requestUrl, fullnode);
    JSONObject blockData = JSONObject.parseObject(returnString);
    logger.info(blockData.toJSONString());
    Assert.assertTrue(returnString.contains("last_solidity_block_num"));
  }

  @Test(enabled = true, description = "test Database.getBlockReference")
  public void test048getBlockReference() {
    String requestUrl = "protocol.Database/getBlockReference";
    String returnString = PublicMethed.gRPCurlRequest(null, requestUrl, fullnode);
    Assert.assertNotNull(returnString);
    JSONObject blockData = JSONObject.parseObject(returnString);
    logger.info(blockData.toJSONString());
    Long blockId = blockData.getLongValue("block_num");
    Assert.assertTrue(blockId > 0L);
  }

}
