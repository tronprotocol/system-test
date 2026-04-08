package stest.tron.wallet.dailybuild.http;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.HttpMethod;
import stest.tron.wallet.common.client.utils.PublicMethod;


@Slf4j
public class HttpTestProposal001 {

  private static Integer proposalId;
  private final String testKey002 =
      Configuration.getByPath("testng.conf").getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethod.getFinalAddress(testKey002);
  private final String witnessKey001 =
      Configuration.getByPath("testng.conf").getString("witness.key1");
  private final byte[] witness1Address = PublicMethod.getFinalAddress(witnessKey001);
  private final String witnessKey002 =
      Configuration.getByPath("testng.conf").getString("witness.key2");
  private final byte[] witness2Address = PublicMethod.getFinalAddress(witnessKey002);
  private String httpnode =
      Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list").get(0);
  private JSONObject responseContent;
  private HttpResponse response;
  private int energyFee = 0;
  private int bandFee = 0;

  /** constructor. */
  @Test(enabled = true, description = "Create proposal by http", groups = {"daily", "serial"})
  public void test1CreateProposal() {
    response = HttpMethod.createProposal(httpnode, witness1Address, 20L, 1L, witnessKey001);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
  }

  /** * constructor. * */
  @Test(enabled = true, description = "List proposals by http", groups = {"daily", "serial"})
  public void test2ListProposals() {
    response = HttpMethod.listProposals(httpnode);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    JSONArray jsonArray = JSONArray.parseArray(responseContent.getString("proposals"));
    Assert.assertTrue(jsonArray.size() >= 1);
    proposalId = jsonArray.size();
  }

  /** constructor. */
  @Test(enabled = true, description = "GetProposalById by http", groups = {"daily", "serial"})
  public void test3GetExchangeById() {
    response = HttpMethod.getProposalById(httpnode, proposalId);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.getInteger("proposal_id") == proposalId);
    Assert.assertEquals(
        responseContent.getString("proposer_address"), ByteArray.toHexString(witness1Address));
  }

  /** constructor. */
  @Test(enabled = true, description = "Approval proposal by http", groups = {"daily", "serial"})
  public void test4ApprovalProposal() {
    response =
        HttpMethod.approvalProposal(httpnode, witness1Address, proposalId, true, witnessKey001);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    response =
        HttpMethod.approvalProposal(httpnode, witness2Address, proposalId, true, witnessKey002);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    response = HttpMethod.getProposalById(httpnode, proposalId);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    JSONArray jsonArray = JSONArray.parseArray(responseContent.getString("approvals"));
    Assert.assertTrue(jsonArray.size() == 2);
  }

  /** * constructor. * */
  @Test(enabled = true, description = "Get paginated proposal list by http", groups = {"daily", "serial"})
  public void test5GetPaginatedProposalList() {

    response = HttpMethod.getPaginatedProposalList(httpnode, 0, 1);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);

    JSONArray jsonArray = JSONArray.parseArray(responseContent.getString("proposals"));
    Assert.assertTrue(jsonArray.size() == 1);
  }

  /** constructor. */
  @Test(enabled = true, description = "Delete proposal by http", groups = {"daily", "serial"})
  public void test6DeleteProposal() {
    response = HttpMethod.deleteProposal(httpnode, witness1Address, proposalId, witnessKey001);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    response = HttpMethod.getProposalById(httpnode, proposalId);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertEquals(responseContent.getString("state"), "CANCELED");
  }

  /** constructor. */
  @Test(enabled = true, description = "Get chain parameters by http", groups = {"daily", "serial"})
  public void test7GetChainParameters() {
    response = HttpMethod.getChainParameters(httpnode);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertEquals(
        "getMaintenanceTimeInterval",
        responseContent.getJSONArray("chainParameter").getJSONObject(0).get("key"));
    Assert.assertEquals(
        180000, responseContent.getJSONArray("chainParameter").getJSONObject(0).get("value"));
    Assert.assertEquals(
        "getCreateAccountFee",
        responseContent.getJSONArray("chainParameter").getJSONObject(2).get("key"));
    Assert.assertEquals(
        100000, responseContent.getJSONArray("chainParameter").getJSONObject(2).get("value"));
    for (Object ob : responseContent.getJSONArray("chainParameter")) {
      if ("getEnergyFee".equalsIgnoreCase(((JSONObject) ob).getString("key"))) {
        energyFee = ((JSONObject) ob).getIntValue("value");
      } else if ("getTransactionFee".equalsIgnoreCase(((JSONObject) ob).getString("key"))) {
        bandFee = ((JSONObject) ob).getIntValue("value");
      }
      if (energyFee > 0 && bandFee > 0) {
        break;
      }
    }
  }
  /** constructor. */

  @Test(enabled = true, description = "Get energy price by http", groups = {"daily", "serial"})
  public void test8GetEnergyPrice() {
    response = HttpMethod.getEnergyPric(httpnode);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    String prices = responseContent.getString("prices");
    String expectPrices = "0:100.*" + energyFee + "$";
    logger.info("prices:" + prices);
    Assert.assertTrue(Pattern.matches(expectPrices, prices));
  }

  /** constructor. */

  @Test(enabled = true, description = "Get band price by http", groups = {"daily", "serial"})
  public void test8GetBandPrice() {
    response = HttpMethod.getBandPric(httpnode);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    String prices = responseContent.getString("prices");
    String expectPrices = "0:10.*" + bandFee + "$";
    logger.info("prices:" + prices);
    Assert.assertTrue(Pattern.matches(expectPrices, prices));
  }

  /** constructor. */
  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethod.disConnect();
  }
}
