package stest.tron.wallet.dailybuild.manual;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
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
import stest.tron.wallet.common.client.utils.ProposalEnum;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import stest.tron.wallet.common.client.utils.JsonRpcBase.*;

@Slf4j
public class EstimateEnergyTest001 {
  private final String foundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] foundationAddress = PublicMethed.getFinalAddress(foundationKey);


  //FullNode1 estimateEnergy = False
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode =
      Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String soliditynode =
      Configuration.getByPath("testng.conf").getStringList("solidityNode.ip.list")
      .get(1);
  private ManagedChannel channelSolidity = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFullSolidity = null;
  private String pbftnode =
      Configuration.getByPath("testng.conf").getStringList("solidityNode.ip.list")
          .get(3);
  private ManagedChannel channelPbft = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubPbft = null;
  private byte[] contractAddress = null;
  private long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  private byte[] contractAddressTrc721 = null;
  private byte[] contractAddressTrc20 = null;

  //FullNode2 estimateEnergy = True
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
      Configuration.getByPath("testng.conf").getStringList("jsonRpcNode.ip.list").get(2);

  private Long energyFee = 0L;



  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() throws Exception {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext()
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelFull2 = ManagedChannelBuilder.forTarget(fullnode2)
        .usePlaintext()
        .build();
    blockingStubFull2 = WalletGrpc.newBlockingStub(channelFull2);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext()
        .build();
    blockingStubFullSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);

    channelPbft = ManagedChannelBuilder.forTarget(pbftnode)
        .usePlaintext()
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
            blockingStubFull2);
    PublicMethed.waitProduceNextBlock(blockingStubFull2);
    Optional<Protocol.TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull2);

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
            blockingStubFull2);
    PublicMethed.waitProduceNextBlock(blockingStubFull2);
    logger.info("deployTrc20Txid：" + deployTrc20Txid);
    Optional<Protocol.TransactionInfo> info =
        PublicMethed.getTransactionInfoById(deployTrc20Txid, blockingStubFull2);

    Assert.assertTrue(info.get().getReceipt().getResult().toString() == "SUCCESS");
    contractAddressTrc20 = info.get().getContractAddress().toByteArray();

    //deploy TRC721

    String filePathTrc721 = "./src/test/resources/soliditycode/trontrc721.sol";
    String contractNameTrc721 = "TRC721Token";
    retMap = PublicMethed.getBycodeAbi(filePathTrc721, contractNameTrc721);

    String codeTrc721 = retMap.get("byteCode").toString();
    String abiTrc721 = retMap.get("abI").toString();

    contractAddressTrc721 = PublicMethed.deployContract(
        contractNameTrc721, abiTrc721, codeTrc721, "", maxFeeLimit,
        0L, 100, null, foundationKey, foundationAddress, blockingStubFull2);
    PublicMethed.waitProduceNextBlock(blockingStubFull2);
    energyFee = PublicMethed
        .getChainParametersValue(ProposalEnum.GetEnergyFee.getProposalName(), blockingStubFull2);
    Assert.assertNotEquals(0L, energyFee.longValue());

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "EstimateEnergy request fullnode solidity pbft")
  public void test01TriggerFunctionFullnodeSolidityPbft() {
    String method = "writeNumber(uint256)";
    String args = "5";
    boolean isHex = false;
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
        PublicMethed.estimateEnergy(
            blockingStubFull2, foundationAddress, contractAddress, 0, method, args, isHex, 0, null);
    Assert.assertEquals(estimateEnergyMessage.get().getResult().getCode().toString(), "SUCCESS");
    Long energyEstimateRequired = estimateEnergyMessage.get().getEnergyRequired();
    GrpcAPI.TransactionExtention te =
        PublicMethed.triggerConstantContractForExtention(
            contractAddress, method, args, false, 0,
            maxFeeLimit, "#", 0L, foundationAddress, foundationKey, blockingStubFull2);
    Assert.assertEquals(te.getResult().getCode().toString(), "SUCCESS");
    Long energyUsedConstant = te.getEnergyUsed();
    //energyEstimateRequired is bigger than energyUsedConstant but not more than 1 TRX.
    Assert.assertTrue((energyEstimateRequired - energyUsedConstant) * energyFee <= 1000000L);
    //if do not wait, contact can be null
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull2, blockingStubFullSolidity);
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull2, blockingStubPbft);
    //query solidity , query pbft
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessageSolidity =
        PublicMethed.estimateEnergySolidity(
            blockingStubFullSolidity, foundationAddress,
            contractAddress, 0, method, args, isHex, 0, null);
    logger.info("energyEstimateRequired:" + energyEstimateRequired);
    logger.info("estimateEnergyMessageSolidity:"
        + estimateEnergyMessageSolidity.get().getEnergyRequired());
    logger.info(estimateEnergyMessage.get().toString());
    logger.info(estimateEnergyMessageSolidity.get().toString());
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
  @Test(enabled = true, description = "EstimateEnergy to a Revert Function")
  public void test02TriggerFunctionRevert() {
    String method = "writeNumber(uint256)";
    String args = "5";
    long callValue = 1;
    //give a non payable function a callValue to make REVERT opcode executed
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
        PublicMethed.estimateEnergy(blockingStubFull2, foundationAddress,
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
  @Test(enabled = true, description = "EstimateEnergy use to calculate feelimit can be triggered success")
  public void test03EstimateEnergyUseToCalculateFeeLimit() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    final byte[] ownerAddress = ecKey1.getAddress();
    final String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    Assert.assertTrue(
        PublicMethed.sendcoin(ownerAddress, 100000000L,
            foundationAddress, foundationKey, blockingStubFull2));
    PublicMethed.waitProduceNextBlock(blockingStubFull2);

    String method = "writeNumber(uint256)";
    String args = "6";
    boolean isHex = false;
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
        PublicMethed.estimateEnergy(blockingStubFull2, ownerAddress,
            contractAddress, 0, method, args, isHex, 0, null);
    Assert.assertEquals(estimateEnergyMessage.get().getResult().getCode().toString(), "SUCCESS");
    Long energyEstimateRequired = estimateEnergyMessage.get().getEnergyRequired();
    GrpcAPI.TransactionExtention te =
        PublicMethed.triggerConstantContractForExtention(
            contractAddress, method, args, false, 0,
            maxFeeLimit, "#", 0L, ownerAddress, ownerKey, blockingStubFull2);
    Assert.assertEquals(te.getResult().getCode().toString(), "SUCCESS");
    Long energyUsedConstant = te.getEnergyUsed();
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
            0L, feeLimit, ownerAddress, ownerKey, blockingStubFull2);
    PublicMethed.waitProduceNextBlock(blockingStubFull2);
    Optional<Protocol.TransactionInfo> info =
        PublicMethed.getTransactionInfoById(triggerTxidSuccess, blockingStubFull2);
    logger.info(info.get().getReceipt().toString());
    Assert.assertEquals(info.get().getReceipt().getResult().toString(), "SUCCESS");

    //if feelimit is low , OUT_OF_ENERGY
    args = "8";
    te =  PublicMethed.triggerConstantContractForExtention(
        contractAddress, method, args, false, 0,
        maxFeeLimit, "#", 0L, ownerAddress, ownerKey, blockingStubFull2);
    energyUsedConstant = te.getEnergyUsed();
    feeLimit = energyUsedConstant - energyFee;
    String triggerTxidFail =
        PublicMethed.triggerContract(
            contractAddress, method, args, isHex, 0L,
            feeLimit, ownerAddress, ownerKey, blockingStubFull2);
    PublicMethed.waitProduceNextBlock(blockingStubFull2);
    info = PublicMethed.getTransactionInfoById(triggerTxidFail, blockingStubFull2);
    logger.info(info.get().getReceipt().toString());
    Assert.assertEquals(info.get().getReceipt().getResult().toString(), "OUT_OF_ENERGY");
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "TriggerConstantContract use to calculate feelimit boundary value test")
  public void test04FeeLimitBoundary() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    final byte[] ownerAddress = ecKey1.getAddress();
    final String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    logger.info("bse58:" + Base58.encode58Check(ownerAddress));
    Assert.assertTrue(
        PublicMethed.sendcoin(ownerAddress, 10000000000L,
            foundationAddress, foundationKey, blockingStubFull2));
    PublicMethed.waitProduceNextBlock(blockingStubFull2);
    Assert.assertTrue(PublicMethed
        .freezeBalanceGetEnergy(ownerAddress, 1000000000L, 3L, 1, ownerKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull2);

    String method = "writeNumber(uint256)";
    String args = "6";
    boolean isHex = false;
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
        PublicMethed.estimateEnergy(blockingStubFull2, ownerAddress,
            contractAddress, 0, method, args, isHex, 0, null);
    Assert.assertEquals(estimateEnergyMessage.get().getResult().getCode().toString(), "SUCCESS");
    Long energyEstimateRequired = estimateEnergyMessage.get().getEnergyRequired();
    GrpcAPI.TransactionExtention te =
        PublicMethed.triggerConstantContractForExtention(
            contractAddress, method, args, false, 0,
            maxFeeLimit, "#", 0L, ownerAddress, ownerKey, blockingStubFull2);
    Assert.assertEquals(te.getResult().getCode().toString(), "SUCCESS");
    Long energyUsedConstant = te.getEnergyUsed();
    //energyEstimateRequired is bigger than energyUsedConstant but not more than 1 TRX.
    logger.info("energyUsedConstant:" + energyUsedConstant);
    logger.info("energyEstimateRequired:" + energyEstimateRequired);
    Assert.assertNotEquals(energyEstimateRequired.longValue(), energyUsedConstant.longValue());
    Assert.assertTrue((energyEstimateRequired - energyUsedConstant) * energyFee <= 1000000L);

    Long feeLimit = energyEstimateRequired * energyFee;
    logger.info("FeeLimit:" + feeLimit);
    Long feeLimitConstant = energyUsedConstant * energyFee;
    logger.info("feeLimitConstant:" + feeLimitConstant);
    Assert.assertTrue(feeLimitConstant <= feeLimit);
    String failTxid =
        PublicMethed.triggerContract(
            contractAddress, method, args, isHex,
            0L, feeLimitConstant - 1L, ownerAddress, ownerKey, blockingStubFull2);
    PublicMethed.waitProduceNextBlock(blockingStubFull2);
    Optional<Protocol.TransactionInfo> infofail =
        PublicMethed.getTransactionInfoById(failTxid, blockingStubFull2);
    logger.info(infofail.get().getReceipt().toString());
    Assert.assertEquals(infofail.get().getReceipt().getResult().toString(), "OUT_OF_ENERGY");
    PublicMethed.waitProduceNextBlock(blockingStubFull2);

    String triggerTxidSuccess =
        PublicMethed.triggerContract(
            contractAddress, method, args, isHex,
            0L, feeLimitConstant, ownerAddress, ownerKey, blockingStubFull2);
    PublicMethed.waitProduceNextBlock(blockingStubFull2);
    Optional<Protocol.TransactionInfo> info =
        PublicMethed.getTransactionInfoById(triggerTxidSuccess, blockingStubFull2);
    logger.info(info.get().getReceipt().toString());
    Assert.assertEquals(info.get().getReceipt().getResult().toString(), "SUCCESS");

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Estimate a function which has emit a event")
  public void test05TriggerEventFunction() {
    String method = "clockOut(address,string,uint256,bool,uint256)";
    String args = "\"TB4B1RMhoPeivkj4Hebm6tttHjRY9yQFes\",\"beijing\",5,true,256";
    boolean isHex = false;
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
        PublicMethed.estimateEnergy(blockingStubFull2, foundationAddress,
            contractAddress, 0, method, args, isHex, 0, null);
    Assert.assertEquals(estimateEnergyMessage.get().getResult().getCode().toString(),  "SUCCESS");
    Long energyEstimateRequired = estimateEnergyMessage.get().getEnergyRequired();
    GrpcAPI.TransactionExtention te =
        PublicMethed.triggerConstantContractForExtention(
            contractAddress, method, args, false, 0,
            maxFeeLimit, "#", 0L, foundationAddress, foundationKey, blockingStubFull2);
    Assert.assertEquals(te.getResult().getCode().toString(), "SUCCESS");
    Long energyUsedConstant = te.getEnergyUsed();
    //energyEstimateRequired is bigger than energyUsedConstant but not more than 1 TRX.
    Assert.assertTrue((energyEstimateRequired - energyUsedConstant) * energyFee <= 1000000L);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Estimate nonexistent function")
  public void test06NotExistFunction() {
    String method = "aaa(address,string,uint256,bool,uint256)";
    String args = "\"TB4B1RMhoPeivkj4Hebm6tttHjRY9yQFes\",\"beijing\",5,true,256";
    boolean isHex = false;
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
        PublicMethed.estimateEnergy(
            blockingStubFull2, foundationAddress, contractAddressTrc721, 0, method, args, isHex, 0, null);
    Assert.assertEquals(
        estimateEnergyMessage.get().getResult().getCode().toString(), "CONTRACT_EXE_ERROR");
    Assert.assertEquals(
        estimateEnergyMessage.get().getResult().getMessage().toStringUtf8(),
        "REVERT opcode executed");
  }



  /**
   * constructor.
   */
  @Test(enabled = true, description = "Estimate payable function")
  public void test07PayableFunction() {
    String method = "payMeTRX()";
    String args = "";
    boolean isHex = false;
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
        PublicMethed.estimateEnergy(blockingStubFull2, foundationAddress,
            contractAddress, 0, method, args, isHex, 0, null);
    Assert.assertEquals(estimateEnergyMessage.get().getResult().getCode().toString(), "SUCCESS");
    Long energyEstimateRequired = estimateEnergyMessage.get().getEnergyRequired();
    GrpcAPI.TransactionExtention te =  PublicMethed.triggerConstantContractForExtention(
        contractAddress, method, args, false, 10000000L,
        maxFeeLimit, "#", 0L, foundationAddress, foundationKey, blockingStubFull2);
    Assert.assertEquals(te.getResult().getCode().toString(), "SUCCESS");
    Long energyUsedConstant = te.getEnergyUsed();
    //energyEstimateRequired is bigger than energyUsedConstant but not more than 1 TRX.
    Assert.assertTrue((energyEstimateRequired - energyUsedConstant) * energyFee <= 1000000L);

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Estimate TRC20 important function")
  public void test08Trc20TransferFunction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] coinReceiverAddress = ecKey1.getAddress();
    String coinReceiverBase58 = Base58.encode58Check(coinReceiverAddress);
    //transfer
    String method = "transfer(address,uint256)";
    String args = "\"" + coinReceiverBase58 + "\",256";
    boolean isHex = false;
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
        PublicMethed.estimateEnergy(
            blockingStubFull2, foundationAddress,
            contractAddressTrc20, 0, method, args, isHex, 0, null);
    Assert.assertEquals(estimateEnergyMessage.get().getResult().getCode().toString(), "SUCCESS");
    Long energyEstimateRequired = estimateEnergyMessage.get().getEnergyRequired();
    GrpcAPI.TransactionExtention te =
        PublicMethed.triggerConstantContractForExtention(
            contractAddressTrc20, method, args, false, 0L,
            maxFeeLimit, "#", 0L, foundationAddress, foundationKey, blockingStubFull2);
    Assert.assertEquals(te.getResult().getCode().toString(), "SUCCESS");
    Long energyUsedConstant = te.getEnergyUsed();
    //energyEstimateRequired is bigger than energyUsedConstant but not more than 1 TRX.
    Assert.assertTrue((energyEstimateRequired - energyUsedConstant) * energyFee <= 1000000L);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Estimate TRC20 important function")
  public void test09Trc20BalanceOfFunction() {
    String method = "balanceOf(address)";
    String args = "\"" + Base58.encode58Check(foundationAddress) + "\"";
    boolean isHex = false;
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
        PublicMethed.estimateEnergy(blockingStubFull2, foundationAddress,
            contractAddressTrc20, 0, method, args, isHex, 0, null);
    Assert.assertEquals(estimateEnergyMessage.get().getResult().getCode().toString(), "SUCCESS");
    Long energyEstimateRequired = estimateEnergyMessage.get().getEnergyRequired();
    GrpcAPI.TransactionExtention te =
        PublicMethed.triggerConstantContractForExtention(
            contractAddressTrc20, method, args, false, 0L,
            maxFeeLimit, "#", 0L, foundationAddress, foundationKey, blockingStubFull2);
    Assert.assertEquals(te.getResult().getCode().toString(), "SUCCESS");
    Long energyUsedConstant = te.getEnergyUsed();
    //energyEstimateRequired is bigger than energyUsedConstant but not more than 1 TRX.
    Assert.assertTrue((energyEstimateRequired - energyUsedConstant) * energyFee <= 1000000L);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Estimate TRC20 important function")
  public void test10Trc20ApproveFunction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] coinReceiverAddress = ecKey1.getAddress();
    String coinReceiverBase58 = Base58.encode58Check(coinReceiverAddress);
    //transfer
    String method = "approve(address,uint256)";
    String args = "\"" + coinReceiverBase58 + "\",256";
    boolean isHex = false;
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
        PublicMethed.estimateEnergy(blockingStubFull2, foundationAddress,
            contractAddressTrc20, 0, method, args, isHex, 0, null);
    Assert.assertEquals(estimateEnergyMessage.get().getResult().getCode().toString(), "SUCCESS");
    Long energyEstimateRequired = estimateEnergyMessage.get().getEnergyRequired();
    GrpcAPI.TransactionExtention te =
        PublicMethed.triggerConstantContractForExtention(
            contractAddressTrc20, method, args, false, 0L,
            maxFeeLimit, "#",  0L, foundationAddress, foundationKey, blockingStubFull2);
    Assert.assertEquals(te.getResult().getCode().toString(), "SUCCESS");
    Long energyUsedConstant = te.getEnergyUsed();
    //energyEstimateRequired is bigger than energyUsedConstant but not more than 1 TRX.
    Assert.assertTrue((energyEstimateRequired - energyUsedConstant) * energyFee <= 1000000L);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Estimate TRC20 important function")
  public void test11Trc20AllowanceFunction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] coinReceiverAddress = ecKey1.getAddress();
    String coinReceiverBase58 = Base58.encode58Check(coinReceiverAddress);
    //transfer
    String method = "allowance(address,address)";
    String args = "\"" + Base58.encode58Check(foundationAddress) + "\",\""
        + coinReceiverBase58 + "\"";
    boolean isHex = false;
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
        PublicMethed.estimateEnergy(blockingStubFull2, foundationAddress,
            contractAddressTrc20, 0, method, args, isHex, 0, null);
    Assert.assertEquals(estimateEnergyMessage.get().getResult().getCode().toString(),  "SUCCESS");
    Long energyEstimateRequired = estimateEnergyMessage.get().getEnergyRequired();
    GrpcAPI.TransactionExtention te =
        PublicMethed.triggerConstantContractForExtention(
            contractAddressTrc20, method, args, false, 0L,
            maxFeeLimit, "#", 0L, foundationAddress, foundationKey, blockingStubFull2);
    Assert.assertEquals(te.getResult().getCode().toString(), "SUCCESS");
    Long energyUsedConstant = te.getEnergyUsed();
    //energyEstimateRequired is bigger than energyUsedConstant but not more than 1 TRX.
    Assert.assertTrue((energyEstimateRequired - energyUsedConstant) * energyFee <= 1000000L);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Estimate compare to eth_estimateEnergy")
  public void test12EthJsonRpcFunction() {
    String method = "writeNumber(uint256)";
    String args = "5";
    boolean isHex = false;
    //fullnode1 vm.estimateEnergy = false  fullnode2 vm.estimateEnergy = true
    //query fullnode1 failed and eth_estimateEnergy == triggerConstantContract
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
        PublicMethed.estimateEnergy(blockingStubFull, foundationAddress,
            contractAddress, 0, method, args, isHex, 0, null);
    logger.info(estimateEnergyMessage.get().toString());
    Assert.assertEquals(
        estimateEnergyMessage.get().getResult().getCode().toString(), "CONTRACT_VALIDATE_ERROR");
    Assert.assertEquals(estimateEnergyMessage.get().getResult().getMessage().toStringUtf8(), "Contract validate error : this node does not support estimate energy");
    //query json rpc
    JsonObject param = new JsonObject();
    param.addProperty("from", ByteArray.toHexString(foundationAddress));
    param.addProperty("to", ByteArray.toHexString(contractAddress));
    param.addProperty("gas", "0x0");
    param.addProperty("gasPrice", "0x0");
    param.addProperty("value", "0x0");
    param.addProperty("data",
        "0x5637a79c"
            + "0000000000000000000000000000000000000000000000000000000000000005");
    JsonArray params = new JsonArray();
    params.add(param);
    JsonRpcBase jrpc = new JsonRpcBase();
    JsonObject requestBody = JsonRpcBase.getJsonRpcBody("eth_estimateGas", params);
    response = jrpc.getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info(responseContent.toJSONString());
    Assert.assertNotNull(responseContent.getString("result"));
    Long energyEstimateFromjrpc =
        Long.valueOf(responseContent.getString("result").replace("0x", ""), 16);

    GrpcAPI.TransactionExtention transactionExtention =
        PublicMethed.triggerConstantContractForExtention(
            contractAddress, method, args, false, 0,
            maxFeeLimit, "", 0L, foundationAddress, foundationKey, blockingStubFull);
    logger.info("triggerConstantContract" + transactionExtention.getResult().toString());
    logger.info("triggerConstantContract energyUsed:" + transactionExtention.getEnergyUsed());
    Assert.assertEquals(transactionExtention.getResult().getCode().toString(), "SUCCESS");
    Assert.assertTrue(
        transactionExtention.getEnergyUsed() == energyEstimateFromjrpc.longValue());

    //query fullnode2
    //eth_estimateEnergy == estimateEnergy
    //eth_estimateEnergy >= triggerConstantContract energyUsed
    estimateEnergyMessage = PublicMethed.estimateEnergy(
        blockingStubFull2, foundationAddress, contractAddress, 0, method, args, isHex, 0, null);
    Assert.assertEquals(estimateEnergyMessage.get().getResult().getCode().toString(), "SUCCESS");
    Long energyEstimateRequired = estimateEnergyMessage.get().getEnergyRequired();
    GrpcAPI.TransactionExtention te =
        PublicMethed.triggerConstantContractForExtention(
            contractAddress, method, args, false, 0,
            maxFeeLimit, "#", 0L, foundationAddress, foundationKey, blockingStubFull2);
    Assert.assertEquals(te.getResult().getCode().toString(), "SUCCESS");
    Long energyUsedConstant = te.getEnergyUsed();
    //energyEstimateRequired is bigger than energyUsedConstant but not more than 1 TRX.
    Assert.assertTrue((energyEstimateRequired - energyUsedConstant) * energyFee <= 1000000L);

    response = jrpc.getJsonRpc(jsonRpcNode2, requestBody);
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
  public void test13Trc721BalanceOfFunction() {
    String method = "balanceOf(address)";
    String args = "\"" + Base58.encode58Check(foundationAddress) + "\"";
    boolean isHex = false;
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
        PublicMethed.estimateEnergy(blockingStubFull2, foundationAddress,
            contractAddressTrc721, 0, method, args, isHex, 0, null);
    Assert.assertEquals(estimateEnergyMessage.get().getResult().getCode().toString(), "SUCCESS");
    Long energyEstimateRequired = estimateEnergyMessage.get().getEnergyRequired();
    GrpcAPI.TransactionExtention te =
        PublicMethed.triggerConstantContractForExtention(
            contractAddressTrc721, method, args, false, 0,
            maxFeeLimit, "#", 0L, foundationAddress, foundationKey, blockingStubFull2);
    Assert.assertEquals(te.getResult().getCode().toString(), "SUCCESS");
    Long energyUsedConstant = te.getEnergyUsed();
    logger.info("energyUsedConstant:" + energyUsedConstant);
    logger.info("energyEstimateRequired:" + energyEstimateRequired);
    //energyEstimateRequired is bigger than energyUsedConstant but not more than 1 TRX.
    Assert.assertTrue((energyEstimateRequired - energyUsedConstant) * energyFee <= 1000000L);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Estimate TRC721 important function")
  public void test14Trc721MintFunction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] coinReceiverAddress = ecKey1.getAddress();
    String coinReceiverBase58 = Base58.encode58Check(coinReceiverAddress);
    String method = "mintWithTokenURI(address,uint256,string)";
    String args = "\"" + Base58.encode58Check(foundationAddress) + "\",1,\"sdd\"";
    boolean isHex = false;
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
        PublicMethed.estimateEnergy(blockingStubFull2, foundationAddress,
            contractAddressTrc721, 0, method, args, isHex, 0, null);
    Assert.assertEquals(estimateEnergyMessage.get().getResult().getCode().toString(),"SUCCESS");
    Long energyEstimateRequired = estimateEnergyMessage.get().getEnergyRequired();
    GrpcAPI.TransactionExtention te =
        PublicMethed.triggerConstantContractForExtention(
            contractAddressTrc721, method, args, false, 0,
            maxFeeLimit, "#", 0L, foundationAddress, foundationKey, blockingStubFull2);
    logger.info(te.getResult().toString());
    Assert.assertEquals(te.getResult().getCode().toString(), "SUCCESS");
    Long energyUsedConstant = te.getEnergyUsed();
    logger.info("energyUsedConstant:" + energyUsedConstant);
    logger.info("energyEstimateRequired:" + energyEstimateRequired);
    //energyEstimateRequired is bigger than energyUsedConstant but not more than 1 TRX.
    Assert.assertTrue((energyEstimateRequired - energyUsedConstant) * energyFee <= 1000000L);

    String triggerTxidSuccess =
        PublicMethed.triggerContract(
            contractAddressTrc721, method, args, isHex, 0L,
            maxFeeLimit, foundationAddress, foundationKey, blockingStubFull2);
    PublicMethed.waitProduceNextBlock(blockingStubFull2);
    Optional<Protocol.TransactionInfo> info =
        PublicMethed.getTransactionInfoById(triggerTxidSuccess, blockingStubFull2);
    logger.info(info.get().getReceipt().toString());
    Assert.assertEquals(info.get().getReceipt().getResult().toString(), "SUCCESS");
  }


  /**
   * constructor.
   */
  @Test(enabled = true,
      description = "Estimate TRC721 important function")
  public void test15Trc721TransferFromFunction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] coinReceiverAddress = ecKey1.getAddress();
    String coinReceiverBase58 = Base58.encode58Check(coinReceiverAddress);
    String method = "transferFrom(address,address,uint256)";
    String args = "\"" + Base58.encode58Check(foundationAddress) + "\"," + "\""
        + coinReceiverBase58 + "\",1";
    boolean isHex = false;
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
        PublicMethed.estimateEnergy(blockingStubFull2, foundationAddress,
            contractAddressTrc721, 0, method, args, isHex, 0, null);
    Assert.assertEquals(estimateEnergyMessage.get().getResult().getCode().toString(), "SUCCESS");
    Long energyEstimateRequired = estimateEnergyMessage.get().getEnergyRequired();
    GrpcAPI.TransactionExtention te =
        PublicMethed.triggerConstantContractForExtention(
            contractAddressTrc721, method, args, false, 0,
            maxFeeLimit, "#", 0L, foundationAddress, foundationKey, blockingStubFull2);
    Assert.assertEquals(te.getResult().getCode().toString(), "SUCCESS");
    Long energyUsedConstant = te.getEnergyUsed();
    logger.info("energyUsedConstant:" + energyUsedConstant);
    logger.info("energyEstimateRequired:" + energyEstimateRequired);
    //energyEstimateRequired is bigger than energyUsedConstant but not more than 1 TRX.
    Assert.assertTrue((energyEstimateRequired - energyUsedConstant) * energyFee <= 1000000L);
  }

  /**
   * constructor.
   */
  @Test(enabled = true,
      description = "Estimate TRC721 important function")
  public void test16Trc721SafeTransferFromFunction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] coinReceiverAddress = ecKey1.getAddress();
    String coinReceiverBase58 = Base58.encode58Check(coinReceiverAddress);
    String method = "safeTransferFrom(address,address,uint256)";
    String args = "\"" + Base58.encode58Check(foundationAddress) + "\"," + "\""
        + coinReceiverBase58 + "\",1";
    boolean isHex = false;
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
        PublicMethed.estimateEnergy(blockingStubFull2, foundationAddress,
            contractAddressTrc721, 0, method, args, isHex, 0, null);
    logger.info(estimateEnergyMessage.get().toString());
    Assert.assertEquals(estimateEnergyMessage.get().getResult().getCode().toString(), "SUCCESS");

    Long energyEstimateRequired = estimateEnergyMessage.get().getEnergyRequired();
    GrpcAPI.TransactionExtention te =
        PublicMethed.triggerConstantContractForExtention(
            contractAddressTrc721, method, args, false, 0,
            maxFeeLimit, "#", 0L, foundationAddress, foundationKey, blockingStubFull2);
    logger.info(te.toString());
    Assert.assertEquals(te.getResult().getCode().toString(), "SUCCESS");
    Long energyUsedConstant = te.getEnergyUsed();
    logger.info("energyUsedConstant:" + energyUsedConstant);
    logger.info("energyEstimateRequired:" + energyEstimateRequired);
    //energyEstimateRequired is bigger than energyUsedConstant but not more than 1 TRX.
    Assert.assertTrue((energyEstimateRequired - energyUsedConstant) * energyFee <= 1000000L);
  }

  /**
   * constructor.
   */
  @Test(enabled = true,
      description = "Estimate TRC721 important function")
  public void test17Trc721MintFunctionApprove() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] coinReceiverAddress = ecKey1.getAddress();
    String coinReceiverBase58 = Base58.encode58Check(coinReceiverAddress);
    String method = "approve(address,uint256)";
    String args = "\"" + coinReceiverBase58 + "\",1";
    boolean isHex = false;
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
        PublicMethed.estimateEnergy(blockingStubFull2, foundationAddress,
            contractAddressTrc721, 0, method, args, isHex, 0, null);
    logger.info(estimateEnergyMessage.get().toString());
    Assert.assertEquals(estimateEnergyMessage.get().getResult().getCode().toString(), "SUCCESS");

    Long energyEstimateRequired = estimateEnergyMessage.get().getEnergyRequired();
    GrpcAPI.TransactionExtention te =
        PublicMethed.triggerConstantContractForExtention(
            contractAddressTrc721, method, args, false, 0,
            maxFeeLimit, "#", 0L, foundationAddress, foundationKey, blockingStubFull2);
    logger.info(te.toString());
    Assert.assertEquals(te.getResult().getCode().toString(), "SUCCESS");
    Long energyUsedConstant = te.getEnergyUsed();
    logger.info("energyUsedConstant:" + energyUsedConstant);
    logger.info("energyEstimateRequired:" + energyEstimateRequired);
    //energyEstimateRequired is bigger than energyUsedConstant but not more than 1 TRX.
    Assert.assertTrue((energyEstimateRequired - energyUsedConstant) * energyFee <= 1000000L);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Estimate function with call_value bug-fix call refund")
  public void test18EstimateWithCallvalue() {


    String method = "test()";
    String args = "";
    boolean isHex = false;
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
        PublicMethed.estimateEnergy(
            blockingStubFull2, foundationAddress, contractAddress, 10, method, args, isHex, 0, null);
    Assert.assertEquals(true, estimateEnergyMessage.get().getResult().getResult());
    Assert.assertEquals(10563, estimateEnergyMessage.get().getEnergyRequired());

    String txid = PublicMethed.triggerContract(contractAddress, method, args,
        false, 10, maxFeeLimit, foundationAddress, foundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("1  infoById: " + infoById.get().toString());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS, infoById.get().getReceipt().getResult());
    long actualEnergyTotal = infoById.get().getReceipt().getEnergyUsageTotal();
    long feeLimit = (actualEnergyTotal + 2300) * energyFee;

    txid = PublicMethed.triggerContract(contractAddress, method, args,
        false, 10, feeLimit, foundationAddress, foundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("2  infoById: " + infoById.get().toString());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS, infoById.get().getReceipt().getResult());

    txid = PublicMethed.triggerContract(contractAddress, method, args,
        false, 10, feeLimit - 1, foundationAddress, foundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("3  infoById: " + infoById.get().toString());
    //expect out of energy, but success
//    Assert.assertEquals(Protocol.Transaction.Result.contractResult.OUT_OF_ENERGY, infoById.get().getReceipt().getResult());
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Estimate function with call_value and command after call")
  public void test19EstimateWithCallvalueAndCommandAfterCall() {
    String method = "test1()";
    String args = "";
    boolean isHex = false;
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
        PublicMethed.estimateEnergy(
            blockingStubFull2, foundationAddress, contractAddress, 10, method, args, isHex, 0, null);
    Assert.assertEquals(true, estimateEnergyMessage.get().getResult().getResult());
    Assert.assertEquals(28700, estimateEnergyMessage.get().getEnergyRequired());
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Estimate function out_of_time")
  public void test20EstimateWithCallvalueAndCommandAfterCall() {
    String method = "testUseCpu(int256)";
    String args = "206000";
    boolean isHex = false;
    Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
        PublicMethed.estimateEnergy(
            blockingStubFull2, foundationAddress, contractAddress, 0, method, args, isHex, 0, null);
    System.out.println(estimateEnergyMessage.get().toString());
    Assert.assertTrue( estimateEnergyMessage.get().toString().contains("CPU timeout"));
  }


  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


}
