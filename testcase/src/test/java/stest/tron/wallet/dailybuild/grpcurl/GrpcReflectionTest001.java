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
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Utils;

import java.util.Base64;

@Slf4j
public class GrpcReflectionTest001 {

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
    String data = String
        .format(
            "{\"owner_address\":\"%s\",\"contract_address\":\"%s\",\"data\":\"aH2qOgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB\"}",
            ownerAddressBase64, contractAddressBase64);
    String requestUrl = "protocol.Wallet/TriggerConstantContract";
    String returnString = PublicMethed.gRPCurlRequest(data, requestUrl, fullnode);
    logger.info(returnString);
    JSONObject triggerResult = JSONObject.parseObject(returnString).getJSONObject("result");
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
    String requestUrlSolidity = "protocol.WalletSolidity/GetTransactionById";
    String returnStringSolidity = PublicMethed.gRPCurlRequest(data, requestUrlSolidity, soliditynode);
    JSONObject txDataSolidity = JSONObject.parseObject(returnStringSolidity);
    logger.info(txDataSolidity.toJSONString());
    Assert.assertEquals(
        txDataSolidity.getJSONArray("ret")
            .getJSONObject(0)
            .getString("contractRet"),
        "SUCCESS");
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
    Assert.assertEquals(txData.getLongValue("fee"), 100000L);
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
}
