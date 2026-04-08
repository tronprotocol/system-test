package stest.tron.wallet.dailybuild.http;

import java.util.HashMap;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.protos.Protocol;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.HttpMethod;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;


@Slf4j
public class HttpTestEstimateEnergy extends TronBaseTest {  private JSONObject responseContent;
  private HttpResponse response;
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(1);
  private String httpSoliditynode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(5);
  private String httpPbftNode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(6);
  private byte[] contractAddress = null;
  long deployContractEnergy = 0;
  long energyFee;
  String code;
  String abi;
  ECKey triggerECKey = new ECKey(Utils.getRandom());
  byte[] triggerAddress = triggerECKey.getAddress();
  String triggerKey = ByteArray.toHexString(triggerECKey.getPrivKeyBytes());


  @BeforeClass
  public void beforeClass() {    Assert.assertTrue(PublicMethod.sendcoin(triggerAddress, 10000000000L,
    foundationAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "src/test/resources/soliditycode/estimateenergy.sol";
  String contractName = "TCtoken";
    HashMap retMap = PublicMethod.getBycodeAbiNoOptimize(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
  String txid = HttpMethod.deployContractGetTxid(httpnode, contractName, abi, code, 10000L,
            1000000000L, 100, 10000L,
            0L, null, 0L, triggerAddress, triggerKey);
    HttpMethod.waitToProduceOneBlock(httpnode);
    response = HttpMethod.getTransactionInfoById(httpnode, txid);
    responseContent = HttpMethod.parseResponseContent(response);
    Assert.assertTrue(!responseContent.getString("contract_address").isEmpty());
    contractAddress = ByteArray.fromHexString(responseContent.getString("contract_address"));
    deployContractEnergy = responseContent.getJSONObject("receipt").getLong("energy_usage_total");
    HttpMethod.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    energyFee = PublicMethod.getChainParametersValue("getEnergyFee", blockingStubFull);

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "EstimateEnergy request fullnode,solidity,pbft", groups = {"daily", "serial"})
  public void test01EstimateCanGetValue() {
    String method = "writeNumber(uint256)";
  String param = "0000000000000000000000000000000000000000000000000000000000000006";
    response = HttpMethod
        .getEstimateEnergy(httpnode, foundationAddress, contractAddress, method, param, null,false, 0, 0, 0);
  Long energyRequired = HttpMethod.parseResponseContent(response).getLong("energy_required");
    Assert.assertTrue(energyRequired >= 0);
    HttpMethod.waitToProduceOneBlock(httpnode);
    response = HttpMethod
        .getEstimateEnergySolidity(
            httpSoliditynode, foundationAddress, contractAddress, method, param, false);
  Long energyRequiredSolidity =
        HttpMethod.parseResponseContent(response).getLong("energy_required");
    Assert.assertTrue(energyRequiredSolidity >= 0);

    response = HttpMethod
        .getEstimateEnergyPBFT(
            httpPbftNode, foundationAddress, contractAddress, method, param, false);
  Long energyRequiredPbft = HttpMethod.parseResponseContent(response).getLong("energy_required");
    Assert.assertTrue(energyRequiredPbft >= 0);

    Assert.assertEquals(energyRequired.longValue(), energyRequiredSolidity.longValue());
    Assert.assertEquals(energyRequired.longValue(), energyRequiredPbft.longValue());
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "EstimateEnergy value compare to TriggerConstantContract", groups = {"daily", "serial"})
  public void test02CompareToTriggerConstantContract() {
    String method = "writeNumber(uint256)";
  String param = "0000000000000000000000000000000000000000000000000000000000000006";
    response = HttpMethod
        .getEstimateEnergy(httpnode, foundationAddress, contractAddress, method, param, null,true, 0, 0, 0);
  Long energyRequired = HttpMethod.parseResponseContent(response).getLong("energy_required");
    Assert.assertTrue(energyRequired >= 0);
    response = HttpMethod
        .triggerConstantContract(
            httpnode, foundationAddress, ByteArray.toHexString(contractAddress), method, param);
  Long energyRequiredConstant =
        HttpMethod.parseResponseContent(response).getLong("energy_used");
  final Long energyFee = PublicMethod.getChainParametersValue("getEnergyFee", blockingStubFull);
    logger.info("energyRequired: " + energyRequired);
    logger.info("energyRequiredConstant: " + energyRequiredConstant);
    Assert.assertTrue(energyRequired >= energyRequiredConstant);
    Assert.assertTrue((energyRequired - energyRequiredConstant) * energyFee <= 1000000L);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Estimate energy deploy contract", groups = {"daily", "serial"})
  public void test03EstimateDeployContract() {
    String method = null;
  String param = null;
    response = HttpMethod
        .getEstimateEnergy(httpnode, foundationAddress, null, method, param, code,true, 0, 0, 0);
  Long energyRequired = HttpMethod.parseResponseContent(response).getLong("energy_required");
    Assert.assertTrue(energyRequired >= 0);
    response = HttpMethod
        .triggerConstantContractWithData(
            httpnode, foundationAddress, null, method, param, code, 0, 0, 0);
  Long energyRequiredConstant =
        HttpMethod.parseResponseContent(response).getLong("energy_used");
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
  @Test(enabled = true, description = "Estimate function out_of_time by estimateEnergy and triggerConstant", groups = {"daily", "serial"})
  public void test04EstimateWithCallvalueAndCommandAfterCall() {
    String method = "testUseCpu(int256)";
  String args = "00000000000000000000000000000000000000000000000000000000001324b0";
    response = HttpMethod
        .getEstimateEnergy(httpnode, foundationAddress, contractAddress, method, args, null,true, 0, 0, 0);
    responseContent = HttpMethod.parseResponseContent(response);
    logger.info("EstimateEnergy result: " + responseContent.toJSONString());
    Assert.assertTrue( responseContent.containsKey("result"));
    JSONObject res = responseContent.getJSONObject("result");
    Assert.assertEquals(2, res.keySet().size());
    Assert.assertEquals("OTHER_ERROR".toLowerCase(), res.getString("code").toLowerCase());
    Assert.assertTrue( res.getString("message").contains("CPU timeout"));

    response = HttpMethod
        .triggerConstantContractWithData(
            httpnode, foundationAddress, ByteArray.toHexString(contractAddress), method, args,null,0,0,0);
    responseContent = HttpMethod.parseResponseContent(response);
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
      + "without function_selector but with contract address and data ", groups = {"daily", "serial"})
  public void test05EstimateOnlyHasCalldata() {
    // function is  writeNumber(uint256);
  String data = "5637a79c0000000000000000000000000000000000000000000000000000000000000006";
    response = HttpMethod
        .getEstimateEnergy(httpnode, foundationAddress, contractAddress, null, null, data,true, 0, 0, 0);
  Long energyRequired = HttpMethod.parseResponseContent(response).getLong("energy_required");
    Assert.assertTrue(energyRequired >= 0);
    response = HttpMethod
        .triggerConstantContractWithData(
            httpnode, foundationAddress, ByteArray.toHexString(contractAddress), null, null,data,0,0,0);
  Long energyRequiredConstant =
        HttpMethod.parseResponseContent(response).getLong("energy_used");
    logger.info("energyRequired: " + energyRequired);
    logger.info("energyRequiredConstant" + energyRequiredConstant);
    Assert.assertTrue(energyRequired >= energyRequiredConstant);
    Assert.assertTrue((energyRequired - energyRequiredConstant) * energyFee <= 1000000L);
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "estimateEnergy and triggerconstantcontract "
      + "only with contract address. and it will trigger fallback function ", groups = {"daily", "serial"})
  public void test06EstimateOnlyContractAddress() {
    response = HttpMethod
        .getEstimateEnergy(httpnode, foundationAddress, contractAddress, null, null, null,true, 0, 0, 0);
  Long energyRequired = HttpMethod.parseResponseContent(response).getLong("energy_required");
    Assert.assertTrue(energyRequired >= 0);
    response = HttpMethod
        .triggerConstantContractWithData(
            httpnode, foundationAddress, ByteArray.toHexString(contractAddress), null, null,null,0,0,0);
    responseContent = HttpMethod.parseResponseContent(response);
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
      + "with contract address, function_selector and data. and it will triggerContract use function_selector ", groups = {"daily", "serial"})
  public void test07EstimatePreferFunctionSelector() {
    String method = "writeNumber(uint256)";
  String param = "0000000000000000000000000000000000000000000000000000000000000006";
  //testUseCpu(int256)
    String data = "56d14afe00000000000000000000000000000000000000000000000000000000001324b0";
    response = HttpMethod
        .getEstimateEnergy(httpnode, foundationAddress, contractAddress, method, param, data,true, 0, 0, 0);
  Long energyRequired = HttpMethod.parseResponseContent(response).getLong("energy_required");
    Assert.assertTrue(energyRequired >= 0);
    response = HttpMethod
        .triggerConstantContractWithData(
            httpnode, foundationAddress, ByteArray.toHexString(contractAddress), method, param, data,0,0,0);
    responseContent = HttpMethod.parseResponseContent(response);
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
  @Test(enabled = true, description = "triggerSmartContract using data", groups = {"daily", "serial"})
  public void test08triggerSmartContractWithData() {
    //testUseCpu(int256)
    String data = "56d14afe00000000000000000000000000000000000000000000000000000000001324b0";
  String contractHex = ByteArray.toHexString(contractAddress);
  String txid =
        HttpMethod.triggerContractGetTxid(
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
    HttpMethod.waitToProduceOneBlock(httpnode);
    logger.info(txid);
    response = HttpMethod.getTransactionById(httpnode, txid);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    JSONObject res = responseContent.getJSONArray("ret").getJSONObject(0);
    Assert.assertEquals(1, res.keySet().size());
    Assert.assertEquals("OUT_OF_TIME".toLowerCase(), res.getString("contractRet").toLowerCase());
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethod.disConnect();  }

}
