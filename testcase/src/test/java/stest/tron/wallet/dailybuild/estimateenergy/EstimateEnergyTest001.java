package stest.tron.wallet.dailybuild.estimateenergy;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import javassist.bytecode.BadBytecode;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.junit.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.*;
import zmq.socket.pubsub.Pub;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import stest.tron.wallet.common.client.utils.JsonRpcBase.*;

@Slf4j
public class EstimateEnergyTest001 {
  private final String foundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] foundationAddress = PublicMethed.getFinalAddress(foundationKey);


  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode =
      Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String soliditynode =
      Configuration.getByPath("testng.conf").getStringList("solidityNode.ip.list")
      .get(0);
  private ManagedChannel channelSolidity = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFullSolidity = null;
  private String pbftnode =
      Configuration.getByPath("testng.conf").getStringList("solidityNode.ip.list")
          .get(2);
  private ManagedChannel channelPbft = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubPbft = null;
  private byte[] contractAddress = null;
  private long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  private byte[] contractAddressTrc721 = null;
  private byte[] contractAddressTrc20 = null;

  private ManagedChannel channelFull2 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull2 = null;
  private String fullnode2 =
      Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(1);
  static HttpResponse response;
  static JSONObject responseContent;

  public static String jsonRpcNode =
      Configuration.getByPath("testng.conf").getStringList("jsonRpcNode.ip.list").get(0);
  public static String jsonRpcNode2 =
      Configuration.getByPath("testng.conf").getStringList("jsonRpcNode.ip.list").get(1);





  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() throws Exception {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelFull2 = ManagedChannelBuilder.forTarget(fullnode2)
        .usePlaintext(true)
        .build();
    blockingStubFull2 = WalletGrpc.newBlockingStub(channelFull2);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubFullSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);

    channelPbft = ManagedChannelBuilder.forTarget(pbftnode)
        .usePlaintext(true)
        .build();
    blockingStubPbft = WalletSolidityGrpc.newBlockingStub(channelPbft);
    String filePath = "src/test/resources/soliditycode/estimateenergy.sol";
    String contractName = "TCtoken";
    HashMap retMap = PublicMethed.getBycodeAbiNoOptimize(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    final String txid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            0, 100, 10000, "0", 0, null, foundationKey, foundationAddress,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);

    if (txid == null || infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage()
          .toStringUtf8());
    }
    contractAddress = infoById.get().getContractAddress().toByteArray();




    //deploy TRC20
    String contractNameTrc20 = "shieldTrc20Token";
    String abiTrc20 = Configuration.getByPath("testng.conf").getString("abi.abi_shieldTrc20Token");
    String codeTrc20 =
        Configuration.getByPath("testng.conf").getString("code.code_shieldTrc20Token");
    String constructorStr = "constructor(uint256,string,string)";
    Long totalSupply = 10000000000L;
    String data = totalSupply.toString() + "," + "\"TokenTRC20\"" + "," + "\"zen20\"";
    logger.info("data:" + data);
    String deployTrc20Txid =
        PublicMethed.deployContractWithConstantParame(
            contractNameTrc20,
            abiTrc20,
            codeTrc20,
            constructorStr,
            data,
            "",
            maxFeeLimit,
            0L,
            100,
            null,
            foundationKey,
            foundationAddress,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("deployTrc20Txid：" + deployTrc20Txid);
    Optional<Protocol.TransactionInfo> info =
        PublicMethed.getTransactionInfoById(deployTrc20Txid, blockingStubFull);

    Assert.assertTrue(info.get().getReceipt().getResult().toString() == "SUCCESS");
    contractAddressTrc20 = info.get().getContractAddress().toByteArray();

    //deploy TRC721

    String filePathTrc721 = "./src/test/resources/soliditycode/contractScenario010.sol";
    String contractNameTrc721 = "TRON_ERC721";
    retMap = PublicMethed.getBycodeAbi(filePathTrc721, contractNameTrc721);

    String codeTrc721 = retMap.get("byteCode").toString();
    String abiTrc721 = retMap.get("abI").toString();

    contractAddressTrc721 = PublicMethed.deployContract(
        contractNameTrc721, abiTrc721, codeTrc721, "", maxFeeLimit,
        0L, 100, null, foundationKey, foundationAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "EstimateEnergy by grpc Pure Function")
  public void testTriggerPureFunction() {
    String method = "getMax(uint256,uint256)";
    String args = "5,6";
    boolean isHex = false;
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
        PublicMethed.estimateEnergy(
            blockingStubFull, foundationAddress, contractAddress, 0, method, args, isHex, 0, null);
    Assert.assertEquals(estimateEnergyMessage.get().getResult().getCode().toString(), "SUCCESS");
    Long energyEstimateRequired = estimateEnergyMessage.get().getEnergyRequired();
    GrpcAPI.TransactionExtention te =
        PublicMethed.triggerConstantContractForExtention(
            contractAddress, method, args, false, 0,
            maxFeeLimit, "#", 0L, foundationAddress, foundationKey, blockingStubFull);
    Assert.assertEquals(te.getResult().getCode().toString(), "SUCCESS");
    Long energyUsedConstant = te.getEnergyUsed();
    Long energyFee = PublicMethed.getChainParametersValue("getEnergyFee", blockingStubFull);
    //energyEstimateRequired is bigger than energyUsedConstant but not more than 1 TRX.
    Assert.assertTrue((energyEstimateRequired - energyUsedConstant) * energyFee <= 1000000L);
    //if do not wait, contact can be null
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubFullSolidity);
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubPbft);
    //query solidity , query pbft
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessageSolidity =
        PublicMethed.estimateEnergySolidity(
            blockingStubFullSolidity, foundationAddress,
            contractAddress, 0, method, args, isHex, 0, null);
    Assert.assertTrue(
        energyEstimateRequired == estimateEnergyMessageSolidity.get().getEnergyRequired());
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessagePbft =
        PublicMethed.estimateEnergySolidity(blockingStubPbft,
            foundationAddress, contractAddress, 0, method, args, isHex, 0, null);
    Assert.assertEquals(
        energyEstimateRequired.longValue(), estimateEnergyMessagePbft.get().getEnergyRequired());
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "EstimateEnergy by grpc, Revert")
  public void testTriggerPureFunctionRevert() {
    String method = "getMax(uint256,uint256)";
    String args = "5,6";
    long callValue = 1;
    //give a non payable function a callValue to make REVERT opcode executed
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
        PublicMethed.estimateEnergy(blockingStubFull, foundationAddress,
            contractAddress, callValue, method, args, false, 0, null);
    Assert.assertEquals(
        estimateEnergyMessage.get().getResult().getCode().toString(), "CONTRACT_EXE_ERROR");
    Assert.assertEquals(
        estimateEnergyMessage.get().getResult().getMessage().toStringUtf8(),
        "REVERT opcode executed");

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "EstimateEnergy as a feelimit can be trigger")
  public void testEstimateEnergyUseAsFeeLimit() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    final byte[] ownerAddress = ecKey1.getAddress();
    final String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    Assert.assertTrue(
        PublicMethed.sendcoin(ownerAddress, 100000000L,
            foundationAddress, foundationKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String method = "writeNumber(uint256)";
    String args = "6";
    boolean isHex = false;
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
        PublicMethed.estimateEnergy(blockingStubFull, ownerAddress,
            contractAddress, 0, method, args, isHex, 0, null);
    Assert.assertEquals(estimateEnergyMessage.get().getResult().getCode().toString(), "SUCCESS");
    Long energyEstimateRequired = estimateEnergyMessage.get().getEnergyRequired();
    GrpcAPI.TransactionExtention te =
        PublicMethed.triggerConstantContractForExtention(
            contractAddress, method, args, false, 0,
            maxFeeLimit, "#", 0L, ownerAddress, ownerKey, blockingStubFull);
    Assert.assertEquals(te.getResult().getCode().toString(), "SUCCESS");
    Long energyUsedConstant = te.getEnergyUsed();
    final Long energyFee = PublicMethed.getChainParametersValue("getEnergyFee", blockingStubFull);
    //energyEstimateRequired is bigger than energyUsedConstant but not more than 1 TRX.
    logger.info("energyUsedConstant:" + energyUsedConstant);
    logger.info("energyEstimateRequired:" + energyEstimateRequired);
    Assert.assertNotEquals(energyEstimateRequired.longValue(), energyUsedConstant.longValue());
    Assert.assertTrue((energyEstimateRequired - energyUsedConstant) * energyFee <= 1000000L);

    Long feeLimit = energyEstimateRequired * energyFee;
    logger.info("FeeLimit:" + feeLimit);
    String triggerTxidSuccess =
        PublicMethed.triggerContract(
            contractAddress, method, args, isHex,
            0L, feeLimit, ownerAddress, ownerKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethed.getTransactionInfoById(triggerTxidSuccess, blockingStubFull);
    logger.info(info.get().getReceipt().toString());
    Assert.assertEquals(info.get().getReceipt().getResult().toString(), "SUCCESS");

    //if feelimit is low , OUT_OF_ENERGY
    args = "8";
    te =  PublicMethed.triggerConstantContractForExtention(
        contractAddress, method, args, false, 0,
        maxFeeLimit, "#", 0L, ownerAddress, ownerKey, blockingStubFull);
    energyUsedConstant = te.getEnergyUsed();
    feeLimit = energyUsedConstant - energyFee;
    String triggerTxidFail =
        PublicMethed.triggerContract(
            contractAddress, method, args, isHex, 0L,
            feeLimit, ownerAddress, ownerKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    info = PublicMethed.getTransactionInfoById(triggerTxidFail, blockingStubFull);
    logger.info(info.get().getReceipt().toString());
    Assert.assertEquals(info.get().getReceipt().getResult().toString(), "OUT_OF_ENERGY");
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Estimate a function has emit a Event by grpc")
  public void testTriggerEventFunction() {
    String method = "clockOut(address,string,uint256,bool,uint256)";
    String args = "\"TB4B1RMhoPeivkj4Hebm6tttHjRY9yQFes\",\"beijing\",5,true,256";
    boolean isHex = false;
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
        PublicMethed.estimateEnergy(blockingStubFull, foundationAddress,
            contractAddress, 0, method, args, isHex, 0, null);
    Assert.assertEquals(estimateEnergyMessage.get().getResult().getCode().toString(),  "SUCCESS");
    Long energyEstimateRequired = estimateEnergyMessage.get().getEnergyRequired();
    GrpcAPI.TransactionExtention te =
        PublicMethed.triggerConstantContractForExtention(
            contractAddress, method, args, false, 0,
            maxFeeLimit, "#", 0L, foundationAddress, foundationKey, blockingStubFull);
    Assert.assertEquals(te.getResult().getCode().toString(), "SUCCESS");
    Long energyUsedConstant = te.getEnergyUsed();
    Long energyFee = PublicMethed.getChainParametersValue("getEnergyFee", blockingStubFull);
    //energyEstimateRequired is bigger than energyUsedConstant but not more than 1 TRX.
    Assert.assertTrue((energyEstimateRequired - energyUsedConstant) * energyFee <= 1000000L);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Estimate nonexistent function by grpc")
  public void testNotExistFunction() {
    String method = "aaa(address,string,uint256,bool,uint256)";
    String args = "\"TB4B1RMhoPeivkj4Hebm6tttHjRY9yQFes\",\"beijing\",5,true,256";
    boolean isHex = false;
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
        PublicMethed.estimateEnergy(
            blockingStubFull, foundationAddress, contractAddress, 0, method, args, isHex, 0, null);
    Assert.assertEquals(
        estimateEnergyMessage.get().getResult().getCode().toString(), "CONTRACT_EXE_ERROR");
    Assert.assertEquals(
        estimateEnergyMessage.get().getResult().getMessage().toStringUtf8(),
        "REVERT opcode executed");
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Estimate a Write function by grpc")
  public void testWriteFunction() {
    String method = "writeNumber(uint256)";
    String args = "256";
    boolean isHex = false;
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
        PublicMethed.estimateEnergy(
            blockingStubFull, foundationAddress, contractAddress, 0, method, args, isHex, 0, null);
    Assert.assertEquals(estimateEnergyMessage.get().getResult().getCode().toString(), "SUCCESS");
    Long energyEstimateRequired = estimateEnergyMessage.get().getEnergyRequired();
    GrpcAPI.TransactionExtention te =
        PublicMethed.triggerConstantContractForExtention(
            contractAddress, method, args, false, 0,
            maxFeeLimit, "#", 0L, foundationAddress, foundationKey, blockingStubFull);
    Assert.assertEquals(te.getResult().getCode().toString(), "SUCCESS");
    Long energyUsedConstant = te.getEnergyUsed();
    Long energyFee = PublicMethed.getChainParametersValue("getEnergyFee", blockingStubFull);
    //energyEstimateRequired is bigger than energyUsedConstant but not more than 1 TRX.
    Assert.assertTrue((energyEstimateRequired - energyUsedConstant) * energyFee <= 1000000L);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Estimate payable function by grpc")
  public void testPayableFunction() {
    String method = "payMeTRX()";
    String args = "";
    boolean isHex = false;
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
        PublicMethed.estimateEnergy(blockingStubFull, foundationAddress,
            contractAddress, 0, method, args, isHex, 0, null);
    Assert.assertEquals(estimateEnergyMessage.get().getResult().getCode().toString(), "SUCCESS");
    Long energyEstimateRequired = estimateEnergyMessage.get().getEnergyRequired();
    GrpcAPI.TransactionExtention te =  PublicMethed.triggerConstantContractForExtention(
        contractAddress, method, args, false, 10000000L,
        maxFeeLimit, "#", 0L, foundationAddress, foundationKey, blockingStubFull);
    Assert.assertEquals(te.getResult().getCode().toString(), "SUCCESS");
    Long energyUsedConstant = te.getEnergyUsed();
    Long energyFee = PublicMethed.getChainParametersValue("getEnergyFee", blockingStubFull);
    //energyEstimateRequired is bigger than energyUsedConstant but not more than 1 TRX.
    Assert.assertTrue((energyEstimateRequired - energyUsedConstant) * energyFee <= 1000000L);

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Estimate a view function by grpc")
  public void testViewFunction() {
    String method = "getBlockChainId()";
    String args = "";
    boolean isHex = false;
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
        PublicMethed.estimateEnergy(blockingStubFull, foundationAddress,
            contractAddress, 0, method, args, isHex, 0, null);
    Assert.assertEquals(estimateEnergyMessage.get().getResult().getCode().toString(), "SUCCESS");
    Long energyEstimateRequired = estimateEnergyMessage.get().getEnergyRequired();
    GrpcAPI.TransactionExtention te =
        PublicMethed.triggerConstantContractForExtention(
            contractAddress, method, args, false, 0,
            maxFeeLimit, "#", 0L, foundationAddress, foundationKey, blockingStubFull);
    Assert.assertEquals(te.getResult().getCode().toString(), "SUCCESS");
    Long energyUsedConstant = te.getEnergyUsed();
    Long energyFee = PublicMethed.getChainParametersValue("getEnergyFee", blockingStubFull);
    //energyEstimateRequired is bigger than energyUsedConstant but not more than 1 TRX.
    Assert.assertTrue((energyEstimateRequired - energyUsedConstant) * energyFee <= 1000000L);
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Estimate TRC20 important function")
  public void testTrc20TransferFunction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] coinReceiverAddress = ecKey1.getAddress();
    String coinReceiverBase58 = Base58.encode58Check(coinReceiverAddress);
    //transfer
    String method = "transfer(address,uint256)";
    String args = "\"" + coinReceiverBase58 + "\",256";
    boolean isHex = false;
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
        PublicMethed.estimateEnergy(
            blockingStubFull, foundationAddress,
            contractAddressTrc20, 0, method, args, isHex, 0, null);
    Assert.assertEquals(estimateEnergyMessage.get().getResult().getCode().toString(), "SUCCESS");
    Long energyEstimateRequired = estimateEnergyMessage.get().getEnergyRequired();
    GrpcAPI.TransactionExtention te =
        PublicMethed.triggerConstantContractForExtention(
            contractAddressTrc20, method, args, false, 0L,
            maxFeeLimit, "#", 0L, foundationAddress, foundationKey, blockingStubFull);
    Assert.assertEquals(te.getResult().getCode().toString(), "SUCCESS");
    Long energyUsedConstant = te.getEnergyUsed();
    Long energyFee = PublicMethed.getChainParametersValue("getEnergyFee", blockingStubFull);
    //energyEstimateRequired is bigger than energyUsedConstant but not more than 1 TRX.
    Assert.assertTrue((energyEstimateRequired - energyUsedConstant) * energyFee <= 1000000L);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Estimate TRC20 important function")
  public void testTrc20BalanceOfFunction() {
    String method = "balanceOf(address)";
    String args = "\"" + Base58.encode58Check(foundationAddress) + "\"";
    boolean isHex = false;
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
        PublicMethed.estimateEnergy(blockingStubFull, foundationAddress,
            contractAddressTrc20, 0, method, args, isHex, 0, null);
    Assert.assertEquals(estimateEnergyMessage.get().getResult().getCode().toString(), "SUCCESS");
    Long energyEstimateRequired = estimateEnergyMessage.get().getEnergyRequired();
    GrpcAPI.TransactionExtention te =
        PublicMethed.triggerConstantContractForExtention(
            contractAddressTrc20, method, args, false, 0L,
            maxFeeLimit, "#", 0L, foundationAddress, foundationKey, blockingStubFull);
    Assert.assertEquals(te.getResult().getCode().toString(), "SUCCESS");
    Long energyUsedConstant = te.getEnergyUsed();
    Long energyFee = PublicMethed.getChainParametersValue("getEnergyFee", blockingStubFull);
    //energyEstimateRequired is bigger than energyUsedConstant but not more than 1 TRX.
    Assert.assertTrue((energyEstimateRequired - energyUsedConstant) * energyFee <= 1000000L);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Estimate TRC20 important function")
  public void testTrc20ApproveFunction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] coinReceiverAddress = ecKey1.getAddress();
    String coinReceiverBase58 = Base58.encode58Check(coinReceiverAddress);
    //transfer
    String method = "approve(address,uint256)";
    String args = "\"" + coinReceiverBase58 + "\",256";
    boolean isHex = false;
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
        PublicMethed.estimateEnergy(blockingStubFull, foundationAddress,
            contractAddressTrc20, 0, method, args, isHex, 0, null);
    Assert.assertEquals(estimateEnergyMessage.get().getResult().getCode().toString(), "SUCCESS");
    Long energyEstimateRequired = estimateEnergyMessage.get().getEnergyRequired();
    GrpcAPI.TransactionExtention te =
        PublicMethed.triggerConstantContractForExtention(
            contractAddressTrc20, method, args, false, 0L,
            maxFeeLimit, "#",  0L, foundationAddress, foundationKey, blockingStubFull);
    Assert.assertEquals(te.getResult().getCode().toString(), "SUCCESS");
    Long energyUsedConstant = te.getEnergyUsed();
    Long energyFee = PublicMethed.getChainParametersValue("getEnergyFee", blockingStubFull);
    //energyEstimateRequired is bigger than energyUsedConstant but not more than 1 TRX.
    Assert.assertTrue((energyEstimateRequired - energyUsedConstant) * energyFee <= 1000000L);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Estimate TRC20 important function")
  public void testTrc20AllowanceFunction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] coinReceiverAddress = ecKey1.getAddress();
    String coinReceiverBase58 = Base58.encode58Check(coinReceiverAddress);
    //transfer
    String method = "allowance(address,address)";
    String args = "\"" + Base58.encode58Check(foundationAddress) + "\",\""
        + coinReceiverBase58 + "\"";
    boolean isHex = false;
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
        PublicMethed.estimateEnergy(blockingStubFull, foundationAddress,
            contractAddressTrc20, 0, method, args, isHex, 0, null);
    Assert.assertEquals(estimateEnergyMessage.get().getResult().getCode().toString(),  "SUCCESS");
    Long energyEstimateRequired = estimateEnergyMessage.get().getEnergyRequired();
    GrpcAPI.TransactionExtention te =
        PublicMethed.triggerConstantContractForExtention(
            contractAddressTrc20, method, args, false, 0L,
            maxFeeLimit, "#", 0L, foundationAddress, foundationKey, blockingStubFull);
    Assert.assertEquals(te.getResult().getCode().toString(), "SUCCESS");
    Long energyUsedConstant = te.getEnergyUsed();
    Long energyFee = PublicMethed.getChainParametersValue("getEnergyFee", blockingStubFull);
    //energyEstimateRequired is bigger than energyUsedConstant but not more than 1 TRX.
    Assert.assertTrue((energyEstimateRequired - energyUsedConstant) * energyFee <= 1000000L);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Estimate Test eth_estimateEnergy")
  public void testEthJsonRpcFunction() {
    String method = "getMax(uint256,uint256)";
    String args = "1,256";
    boolean isHex = false;
    //fullnode vm.estimateEnergy = true  fullnode2 vm.estimateEnergy = false
    //query fullnode2 failed and eth_estimateEnergy == triggerConstantContract
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
        PublicMethed.estimateEnergy(blockingStubFull2, foundationAddress,
            contractAddress, 0, method, args, isHex, 0, null);
    logger.info(estimateEnergyMessage.get().toString());
    Assert.assertEquals(
        estimateEnergyMessage.get().getResult().getCode().toString(), "CONTRACT_VALIDATE_ERROR");
    //query json rpc
    JsonObject param = new JsonObject();
    param.addProperty("from", ByteArray.toHexString(foundationAddress));
    param.addProperty("to", ByteArray.toHexString(contractAddress));
    param.addProperty("gas", "0x0");
    param.addProperty("gasPrice", "0x0");
    param.addProperty("value", "0x0");
    param.addProperty("data",
        "0x26b23020"
            + "0000000000000000000000000000000000000000000000000000000000000001"
            + "0000000000000000000000000000000000000000000000000000000000000100");
    JsonArray params = new JsonArray();
    params.add(param);
    JsonRpcBase jrpc = new JsonRpcBase();
    JsonObject requestBody = JsonRpcBase.getJsonRpcBody("eth_estimateGas", params);
    response = jrpc.getJsonRpc(jsonRpcNode2, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    Assert.assertNotNull(responseContent.getString("result"));
    Long energyEstimateFromjrpc =
        Long.valueOf(responseContent.getString("result").replace("0x", ""), 16);

    GrpcAPI.TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtention(
            contractAddress, method, args, false, 0,
            maxFeeLimit, "", 0L, foundationAddress, foundationKey, blockingStubFull2);
    logger.info("triggerConstantContract" + transactionExtention.getResult().toString());
    logger.info("triggerConstantContract energyUsed:" + transactionExtention.getEnergyUsed());
    Assert.assertEquals(transactionExtention.getResult().getCode().toString(), "SUCCESS");
    Assert.assertTrue(
        transactionExtention.getEnergyUsed() == energyEstimateFromjrpc.longValue());

    //query fullnode1
    //eth_estimateEnergy == estimateEnergy
    //eth_estimateEnergy >= triggerConstantContract energyUsed
    estimateEnergyMessage = PublicMethed.estimateEnergy(
        blockingStubFull, foundationAddress, contractAddress, 0, method, args, isHex, 0, null);
    Assert.assertEquals(estimateEnergyMessage.get().getResult().getCode().toString(), "SUCCESS");
    Long energyEstimateRequired = estimateEnergyMessage.get().getEnergyRequired();
    GrpcAPI.TransactionExtention te =
        PublicMethed.triggerConstantContractForExtention(
            contractAddress, method, args, false, 0,
            maxFeeLimit, "#", 0L, foundationAddress, foundationKey, blockingStubFull);
    Assert.assertEquals(te.getResult().getCode().toString(), "SUCCESS");
    Long energyUsedConstant = te.getEnergyUsed();
    Long energyFee = PublicMethed.getChainParametersValue("getEnergyFee", blockingStubFull);
    //energyEstimateRequired is bigger than energyUsedConstant but not more than 1 TRX.
    Assert.assertTrue((energyEstimateRequired - energyUsedConstant) * energyFee <= 1000000L);

    response = jrpc.getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    Assert.assertNotNull(responseContent.getString("result"));
    energyEstimateFromjrpc =
        Long.valueOf(responseContent.getString("result").replace("0x", ""), 16);
    logger.info("energyEstimateFromjrpc:" + energyEstimateFromjrpc);
    logger.info("energyUsedConstant:" + energyUsedConstant);
    logger.info("energyEstimateRequired" + energyEstimateRequired);
    Assert.assertTrue(energyEstimateFromjrpc.longValue() >= energyUsedConstant.longValue());
    Assert.assertTrue(energyEstimateFromjrpc.longValue() == energyEstimateRequired.longValue());
  }



  /**
   * constructor.
   */
  @Test(enabled = true, description = "Estimate TRC721 important function")
  public void testTrc721BalanceOfFunction() {
    String method = "balanceOf(address)";
    String args = "\"" + Base58.encode58Check(foundationAddress) + "\"";
    boolean isHex = false;
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
        PublicMethed.estimateEnergy(blockingStubFull, foundationAddress,
            contractAddressTrc721, 0, method, args, isHex, 0, null);
    Assert.assertEquals(estimateEnergyMessage.get().getResult().getCode().toString(), "SUCCESS");
    Long energyEstimateRequired = estimateEnergyMessage.get().getEnergyRequired();
    GrpcAPI.TransactionExtention te =
        PublicMethed.triggerConstantContractForExtention(
            contractAddressTrc721, method, args, false, 0,
            maxFeeLimit, "#", 0L, foundationAddress, foundationKey, blockingStubFull);
    Assert.assertEquals(te.getResult().getCode().toString(), "SUCCESS");
    Long energyUsedConstant = te.getEnergyUsed();
    Long energyFee = PublicMethed.getChainParametersValue("getEnergyFee", blockingStubFull);
    logger.info("energyUsedConstant:" + energyUsedConstant);
    logger.info("energyEstimateRequired:" + energyEstimateRequired);
    //energyEstimateRequired is bigger than energyUsedConstant but not more than 1 TRX.
    Assert.assertTrue((energyEstimateRequired - energyUsedConstant) * energyFee <= 1000000L);
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "Estimate TRC721 important function")
  public void testTrc721TakeOwnershipFunction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] coinReceiverAddress = ecKey1.getAddress();
    String coinReceiverBase58 = Base58.encode58Check(coinReceiverAddress);
    String method = "takeOwnership(uint256)";
    String args = "0";
    boolean isHex = false;
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
        PublicMethed.estimateEnergy(blockingStubFull, foundationAddress,
            contractAddressTrc721, 0, method, args, isHex, 0, null);
    //Assert.assertEquals(estimateEnergyMessage.get().getResult().getCode().toString(),"SUCCESS");
    Long energyEstimateRequired = estimateEnergyMessage.get().getEnergyRequired();
    GrpcAPI.TransactionExtention te =
        PublicMethed.triggerConstantContractForExtention(
            contractAddressTrc721, method, args, false, 0,
            maxFeeLimit, "#", 0L, foundationAddress, foundationKey, blockingStubFull);
    logger.info(te.getResult().toString());
    Assert.assertEquals(te.getResult().getCode().toString(), "SUCCESS");
    Long energyUsedConstant = te.getEnergyUsed();
    Long energyFee = PublicMethed.getChainParametersValue("getEnergyFee", blockingStubFull);
    logger.info("energyUsedConstant:" + energyUsedConstant);
    logger.info("energyEstimateRequired:" + energyEstimateRequired);
    //energyEstimateRequired is bigger than energyUsedConstant but not more than 1 TRX.
    Assert.assertTrue((energyEstimateRequired - energyUsedConstant) * energyFee <= 1000000L);


    String triggerTxidSuccess =
        PublicMethed.triggerContract(
            contractAddressTrc721, method, args, isHex, 0L,
            maxFeeLimit, foundationAddress, foundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethed.getTransactionInfoById(triggerTxidSuccess, blockingStubFull);
    logger.info(info.get().getReceipt().toString());
    Assert.assertEquals(info.get().getReceipt().getResult().toString(), "SUCCESS");
  }


  /**
   * constructor.
   */
  @Test(enabled = false,
      description = "Estimate TRC721 important function, testTRC721TakeOwnershipFunction must pass")
  public void testTrc721TransferFromFunction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] coinReceiverAddress = ecKey1.getAddress();
    String coinReceiverBase58 = Base58.encode58Check(coinReceiverAddress);
    String method = "transferFrom(address,address,uint256)";
    String args = "\"" + Base58.encode58Check(foundationAddress) + "\"," + "\""
        + coinReceiverBase58 + "\",1";
    boolean isHex = false;
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
        PublicMethed.estimateEnergy(blockingStubFull, foundationAddress,
            contractAddressTrc721, 0, method, args, isHex, 0, null);
    Assert.assertEquals(estimateEnergyMessage.get().getResult().getCode().toString(), "SUCCESS");
    Long energyEstimateRequired = estimateEnergyMessage.get().getEnergyRequired();
    GrpcAPI.TransactionExtention te =
        PublicMethed.triggerConstantContractForExtention(
            contractAddressTrc721, method, args, false, 0,
            maxFeeLimit, "#", 0L, foundationAddress, foundationKey, blockingStubFull);
    Assert.assertEquals(te.getResult().getCode().toString(), "SUCCESS");
    Long energyUsedConstant = te.getEnergyUsed();
    Long energyFee = PublicMethed.getChainParametersValue("getEnergyFee", blockingStubFull);
    logger.info("energyUsedConstant:" + energyUsedConstant);
    logger.info("energyEstimateRequired:" + energyEstimateRequired);
    //energyEstimateRequired is bigger than energyUsedConstant but not more than 1 TRX.
    Assert.assertTrue((energyEstimateRequired - energyUsedConstant) * energyFee <= 1000000L);
  }

  /**
   * constructor.
   */
  @Test(enabled = false,
      description = "Estimate TRC721 important function, testTRC721TakeOwnershipFunction must pass")
  public void testTrc721TransferFunction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] coinReceiverAddress = ecKey1.getAddress();
    String coinReceiverBase58 = Base58.encode58Check(coinReceiverAddress);
    String method = "transfer(address,uint256)";
    String args = "\"" + coinReceiverBase58 + "\",1";
    boolean isHex = false;
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
        PublicMethed.estimateEnergy(blockingStubFull, foundationAddress,
            contractAddressTrc721, 0, method, args, isHex, 0, null);
    Assert.assertEquals(estimateEnergyMessage.get().getResult().getCode().toString(), "SUCCESS");
    Long energyEstimateRequired = estimateEnergyMessage.get().getEnergyRequired();
    GrpcAPI.TransactionExtention te =
        PublicMethed.triggerConstantContractForExtention(
            contractAddressTrc721, method, args, false, 0,
            maxFeeLimit, "#", 0L, foundationAddress, foundationKey, blockingStubFull);
    Assert.assertEquals(te.getResult().getCode().toString(), "SUCCESS");
    Long energyUsedConstant = te.getEnergyUsed();
    Long energyFee = PublicMethed.getChainParametersValue("getEnergyFee", blockingStubFull);
    logger.info("energyUsedConstant:" + energyUsedConstant);
    logger.info("energyEstimateRequired:" + energyEstimateRequired);
    //energyEstimateRequired is bigger than energyUsedConstant but not more than 1 TRX.
    Assert.assertTrue((energyEstimateRequired - energyUsedConstant) * energyFee <= 1000000L);
  }

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


}
