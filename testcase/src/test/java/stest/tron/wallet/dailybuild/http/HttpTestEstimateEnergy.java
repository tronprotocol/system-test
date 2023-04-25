package stest.tron.wallet.dailybuild.http;

import java.util.HashMap;
import com.alibaba.fastjson.JSONObject;
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

import static java.lang.System.exit;


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
  String code;
  String abi;


  @BeforeClass
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    String filePath = "src/test/resources/soliditycode/estimateenergy.sol";
    String contractName = "TCtoken";
    HashMap retMap = PublicMethed.getBycodeAbiNoOptimize(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    final String txid = HttpMethed.deployContractGetTxid(httpnode, contractName, abi, code, 1000000L,
            1000000000L, 100, 10000L, 0L,
            0, 0L, fromAddress, testKey002);
    HttpMethed.waitToProduceOneBlock(httpnode);
    logger.info(txid);
    response =  HttpMethed.getTransactionInfoById(httpnode, txid);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertFalse(responseContent.getString("contract_address").isEmpty());
    contractAddress = ByteArray.fromHexString(responseContent.getString("contract_address"));
    deployContractEnergy = responseContent.getJSONObject("receipt").getLong("energy_usage_total");

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "EstimateEnergy request fullnode,solidity,pbft")
  public void test01EstimateCanGetValue() {
    String method = "writeNumber(uint256)";
    String param = "0000000000000000000000000000000000000000000000000000000000000006";
    response = HttpMethed
        .getEstimateEnergy(httpnode, fromAddress, contractAddress, method, param, null,false);
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
        .getEstimateEnergy(httpnode, fromAddress, contractAddress, method, param, null,true);
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
        .getEstimateEnergy(httpnode, fromAddress, null, method, param, code,true);
    Long energyRequired = HttpMethed.parseResponseContent(response).getLong("energy_required");
    Assert.assertTrue(energyRequired >= 0);
    response = HttpMethed
        .triggerConstantContractWithData(
            httpnode, fromAddress, null, method, param, code, 0, 0, 0);
    Long energyRequiredConstant =
        HttpMethed.parseResponseContent(response).getLong("energy_used");
    final Long energyFee = PublicMethed.getChainParametersValue("getEnergyFee", blockingStubFull);
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
  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethed.disConnect();
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

}
