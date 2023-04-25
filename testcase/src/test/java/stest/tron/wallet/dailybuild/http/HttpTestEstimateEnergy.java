package stest.tron.wallet.dailybuild.http;

import java.util.HashMap;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.Utils;



@Slf4j
public class HttpTestEstimateEnergy {
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private JSONObject responseContent;
  private HttpResponse response;
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(1);
  private String httpSoliditynode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(5);
  private String httpPbftNode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(6);
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private byte[] contractAddress = null;
  long deployContractEnergy = 0;
  long energyFee;
  String code;
  String abi;
  ECKey triggerECKey = new ECKey(Utils.getRandom());
  byte[] triggerAddress = triggerECKey.getAddress();
  String triggerKey = ByteArray.toHexString(triggerECKey.getPrivKeyBytes());


  @BeforeClass
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    Assert.assertTrue(PublicMethed.sendcoin(triggerAddress, 10000000000L,
    fromAddress, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode/estimateenergy.sol";
    String contractName = "TCtoken";
    HashMap retMap = PublicMethed.getBycodeAbiNoOptimize(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    final String txid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", 1000000000L,
            0, 100, 10000, "0", 0, null, triggerKey, triggerAddress,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);

    if (txid == null || infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage()
          .toStringUtf8());
    }
    contractAddress = infoById.get().getContractAddress().toByteArray();
    deployContractEnergy = infoById.get().getReceipt().getEnergyUsageTotal();
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    energyFee = PublicMethed.getChainParametersValue("getEnergyFee", blockingStubFull);

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "EstimateEnergy request fullnode,solidity,pbft")
  public void test01EstimateCanGetValue() {
    String method = "writeNumber(uint256)";
    String param = "0000000000000000000000000000000000000000000000000000000000000006";
    response = HttpMethed
        .getEstimateEnergy(httpnode, fromAddress, contractAddress, method, param, null,false, 0, 0, 0);
    Long energyRequired = HttpMethed.parseResponseContent(response).getLong("energy_required");
    Assert.assertTrue(energyRequired >= 0);
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed
        .getEstimateEnergySolidity(
            httpSoliditynode, fromAddress, contractAddress, method, param, false);
    Long energyRequiredSolidity =
        HttpMethed.parseResponseContent(response).getLong("energy_required");
    Assert.assertTrue(energyRequiredSolidity >= 0);

    response = HttpMethed
        .getEstimateEnergyPBFT(
            httpPbftNode, fromAddress, contractAddress, method, param, false);
    Long energyRequiredPbft = HttpMethed.parseResponseContent(response).getLong("energy_required");
    Assert.assertTrue(energyRequiredPbft >= 0);

    Assert.assertEquals(energyRequired.longValue(), energyRequiredSolidity.longValue());
    Assert.assertEquals(energyRequired.longValue(), energyRequiredPbft.longValue());
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "EstimateEnergy value compare to TriggerConstantContract")
  public void test02CompareToTriggerConstantContract() {
    String method = "writeNumber(uint256)";
    String param = "0000000000000000000000000000000000000000000000000000000000000006";
    response = HttpMethed
        .getEstimateEnergy(httpnode, fromAddress, contractAddress, method, param, null,true, 0, 0, 0);
    Long energyRequired = HttpMethed.parseResponseContent(response).getLong("energy_required");
    Assert.assertTrue(energyRequired >= 0);
    response = HttpMethed
        .triggerConstantContract(
            httpnode, fromAddress, ByteArray.toHexString(contractAddress), method, param);
    Long energyRequiredConstant =
        HttpMethed.parseResponseContent(response).getLong("energy_used");
    final Long energyFee = PublicMethed.getChainParametersValue("getEnergyFee", blockingStubFull);
    logger.info("energyRequired: " + energyRequired);
    logger.info("energyRequiredConstant: " + energyRequiredConstant);
    Assert.assertTrue(energyRequired >= energyRequiredConstant);
    Assert.assertTrue((energyRequired - energyRequiredConstant) * energyFee <= 1000000L);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Estimate energy deploy contract")
  public void test03EstimateDeployContract() {
    String method = null;
    String param = null;
    response = HttpMethed
        .getEstimateEnergy(httpnode, fromAddress, null, method, param, code,true, 0, 0, 0);
    Long energyRequired = HttpMethed.parseResponseContent(response).getLong("energy_required");
    Assert.assertTrue(energyRequired >= 0);
    response = HttpMethed
        .triggerConstantContractWithData(
            httpnode, fromAddress, null, method, param, code, 0, 0, 0);
    Long energyRequiredConstant =
        HttpMethed.parseResponseContent(response).getLong("energy_used");
    logger.info("energyRequired: " + energyRequired);
    logger.info("energyRequiredConstant: " + energyRequiredConstant);
    logger.info("deployEnergyCost: " + deployContractEnergy);
    Assert.assertTrue(energyRequired >= energyRequiredConstant);
    Assert.assertTrue((energyRequired - energyRequiredConstant) * energyFee <= 1000000L);
    Assert.assertTrue((energyRequired - deployContractEnergy) * energyFee <= 1000000L);

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Estimate function out_of_time by estimateEnergy and triggerConstant")
  public void test04EstimateWithCallvalueAndCommandAfterCall() {
    String method = "testUseCpu(int256)";
    String args = "00000000000000000000000000000000000000000000000000000000001324b0";
    response = HttpMethed
        .getEstimateEnergy(httpnode, fromAddress, contractAddress, method, args, null,true, 0, 0, 0);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("EstimateEnergy result: " + responseContent.toJSONString());
    Assert.assertTrue( responseContent.containsKey("result"));
    JSONObject res = responseContent.getJSONObject("result");
    Assert.assertEquals(2, res.keySet().size());
    Assert.assertEquals("OTHER_ERROR".toLowerCase(), res.getString("code").toLowerCase());
    Assert.assertTrue( res.getString("message").contains("CPU timeout"));

    response = HttpMethed
        .triggerConstantContractWithData(
            httpnode, fromAddress, ByteArray.toHexString(contractAddress), method, args,null,0,0,0);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("triggerconstant result: " + responseContent);
    Assert.assertTrue( responseContent.containsKey("result"));
    res = responseContent.getJSONObject("result");
    Assert.assertEquals(2, res.keySet().size());
    Assert.assertEquals("OTHER_ERROR".toLowerCase(), res.getString("code").toLowerCase());
    Assert.assertTrue(ByteString.copyFrom(ByteArray.fromHexString(res.getString("message"))).toStringUtf8().contains("CPU timeout"));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "estimateEnergy and triggerconstantcontract "
      + "without function_selector but with contract address and data ")
  public void test05EstimateOnlyHasCalldata() {
    // function is  writeNumber(uint256);
    String data = "5637a79c0000000000000000000000000000000000000000000000000000000000000006";
    response = HttpMethed
        .getEstimateEnergy(httpnode, fromAddress, contractAddress, null, null, data,true, 0, 0, 0);
    Long energyRequired = HttpMethed.parseResponseContent(response).getLong("energy_required");
    Assert.assertTrue(energyRequired >= 0);
    response = HttpMethed
        .triggerConstantContractWithData(
            httpnode, fromAddress, ByteArray.toHexString(contractAddress), null, null,data,0,0,0);
    Long energyRequiredConstant =
        HttpMethed.parseResponseContent(response).getLong("energy_used");
    logger.info("energyRequired: " + energyRequired);
    logger.info("energyRequiredConstant" + energyRequiredConstant);
    Assert.assertTrue(energyRequired >= energyRequiredConstant);
    Assert.assertTrue((energyRequired - energyRequiredConstant) * energyFee <= 1000000L);
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "estimateEnergy and triggerconstantcontract "
      + "only with contract address. and it will trigger fallback function ")
  public void test06EstimateOnlyContractAddress() {
    response = HttpMethed
        .getEstimateEnergy(httpnode, fromAddress, contractAddress, null, null, null,true, 0, 0, 0);
    Long energyRequired = HttpMethed.parseResponseContent(response).getLong("energy_required");
    Assert.assertTrue(energyRequired >= 0);
    response = HttpMethed
        .triggerConstantContractWithData(
            httpnode, fromAddress, ByteArray.toHexString(contractAddress), null, null,null,0,0,0);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info(responseContent.toJSONString());
    Assert.assertTrue(responseContent.getJSONObject("result").getBoolean("result"));
    Assert.assertEquals(1, responseContent.getJSONArray("logs").size());
    Long energyRequiredConstant = responseContent.getLong("energy_used");
    logger.info("energyRequired: " + energyRequired);
    logger.info("energyRequiredConstant" + energyRequiredConstant);
    Assert.assertTrue(energyRequired >= energyRequiredConstant);
    Assert.assertTrue((energyRequired - energyRequiredConstant) * energyFee <= 1000000L);
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "estimateEnergy and triggerconstantcontract "
      + "with contract address, function_selector and data. and it will triggerContract use function_selector ")
  public void test07EstimatePreferFunctionSelector() {
    String method = "writeNumber(uint256)";
    String param = "0000000000000000000000000000000000000000000000000000000000000006";
    //testUseCpu(int256)
    String data = "56d14afe00000000000000000000000000000000000000000000000000000000001324b0";
    response = HttpMethed
        .getEstimateEnergy(httpnode, fromAddress, contractAddress, method, param, data,true, 0, 0, 0);
    Long energyRequired = HttpMethed.parseResponseContent(response).getLong("energy_required");
    Assert.assertTrue(energyRequired >= 0);
    response = HttpMethed
        .triggerConstantContractWithData(
            httpnode, fromAddress, ByteArray.toHexString(contractAddress), method, param, data,0,0,0);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info(responseContent.toJSONString());
    Long energyRequiredConstant = responseContent.getLong("energy_used");
    logger.info("energyRequired: " + energyRequired);
    logger.info("energyRequiredConstant" + energyRequiredConstant);
    Assert.assertTrue(energyRequired >= energyRequiredConstant);
    Assert.assertTrue((energyRequired - energyRequiredConstant) * energyFee <= 1000000L);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "triggerSmartContract using data")
  public void test08triggerSmartContractWithData() {
    //testUseCpu(int256)
    String data = "56d14afe00000000000000000000000000000000000000000000000000000000001324b0";
    String contractHex = ByteArray.toHexString(contractAddress);
    String txid =
        HttpMethed.triggerContractGetTxid(
            httpnode,
            triggerAddress,
            contractHex,
            null,
            null,
            1000000000L,
            0L,
            0,
            0L,
            data,
            triggerKey);
    HttpMethed.waitToProduceOneBlock(httpnode);
    logger.info(txid);
    response = HttpMethed.getTransactionById(httpnode, txid);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    JSONObject res = responseContent.getJSONArray("ret").getJSONObject(0);
    Assert.assertEquals(1, res.keySet().size());
    Assert.assertEquals("OUT_OF_TIME".toLowerCase(), res.getString("contractRet").toLowerCase());
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethed.disConnect();
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

}
