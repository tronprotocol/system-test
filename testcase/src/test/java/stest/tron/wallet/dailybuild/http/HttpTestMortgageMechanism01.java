package stest.tron.wallet.dailybuild.http;

import com.alibaba.fastjson.JSONObject;
import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.HttpMethod;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;

@Slf4j
public class HttpTestMortgageMechanism01 {

  private static final long now = System.currentTimeMillis();
  private final String testKey002 =
      Configuration.getByPath("testng.conf").getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethod.getFinalAddress(testKey002);
  private final String witnessKey =
      Configuration.getByPath("testng.conf").getString("witness.key1");
  private final byte[] witnessAddress = PublicMethod.getFinalAddress(witnessKey);
  private final String witnessKey2 =
      Configuration.getByPath("testng.conf").getString("witness.key2");
  private final byte[] witnessAddress2 = PublicMethod.getFinalAddress(witnessKey2);
  Long amount = 2048000000L;
  String description =
      Configuration.getByPath("testng.conf").getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf").getString("defaultParameter.assetUrl");
  private JSONObject responseContent;
  private HttpResponse response;
  private String httpnode =
      Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list").get(1);
  private String httpSoliditynode =
      Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list").get(2);
  private String httpPbftNode =
      Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list").get(4);
  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] dev001Address = ecKey1.getAddress();
  private String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  /** constructor. */
  @Test(enabled = true, description = "GetBrokerage by http", groups = {"daily", "serial"})
  public void test01GetBrokerage() {
    response = HttpMethod.getBrokerage(httpnode, witnessAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertEquals("20", responseContent.getString("brokerage"));
    Assert.assertTrue(Integer.parseInt(responseContent.getString("brokerage")) > 0);
    Assert.assertTrue(Integer.parseInt(responseContent.getString("brokerage")) < 100);
    response = HttpMethod.getBrokerageOnVisible(httpnode, witnessAddress2, "true");
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(Integer.parseInt(responseContent.getString("brokerage")) > 0);
    Assert.assertTrue(Integer.parseInt(responseContent.getString("brokerage")) < 100);

    response = HttpMethod.getBrokerageOnVisible(httpnode, fromAddress, "false");
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(Integer.parseInt(responseContent.getString("brokerage")) > 0);
    Assert.assertTrue(Integer.parseInt(responseContent.getString("brokerage")) < 100);
  }

  @Test(enabled = true, description = "GetBrokerage from solidity by http", groups = {"daily", "serial"})
  public void test02GetBrokerageFromSolidity() {
    response = HttpMethod.getBrokerageFromSolidity(httpSoliditynode, witnessAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(Integer.parseInt(responseContent.getString("brokerage")) > 0);
    Assert.assertTrue(Integer.parseInt(responseContent.getString("brokerage")) < 100);


    response =
        HttpMethod.getBrokerageFromSolidityOnVisible(httpSoliditynode, witnessAddress2, "true");
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(Integer.parseInt(responseContent.getString("brokerage")) > 0);
    Assert.assertTrue(Integer.parseInt(responseContent.getString("brokerage")) < 100);

    response = HttpMethod.getBrokerageFromSolidityOnVisible(httpSoliditynode, fromAddress, "false");
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(Integer.parseInt(responseContent.getString("brokerage")) > 0);
    Assert.assertTrue(Integer.parseInt(responseContent.getString("brokerage")) < 100);
  }

  /** constructor. */
  @Test(enabled = true, description = "GetBrokerage from PBFT by http", groups = {"daily", "serial"})
  public void test03GetBrokerageFromPbft() {
    response = HttpMethod.getBrokerageFromPbft(httpPbftNode, witnessAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(Integer.parseInt(responseContent.getString("brokerage")) > 0);
    Assert.assertTrue(Integer.parseInt(responseContent.getString("brokerage")) < 100);
  }

  /** constructor. */
  @Test(enabled = true, description = "UpdateBrokerage by http", groups = {"daily", "serial"})
  public void test04UpdateBrokerage() {
    response = HttpMethod.sendCoin(httpnode, fromAddress, witnessAddress, amount, testKey002);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);

    // update brokerage
    response = HttpMethod.updateBrokerage(httpnode, witnessAddress, 11L, witnessKey);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);

    response = HttpMethod.sendCoin(httpnode, fromAddress, witnessAddress2, amount, testKey002);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);

    // update brokerage onvisible true
    response =
        HttpMethod.updateBrokerageOnVisible(httpnode, witnessAddress2, 24L, witnessKey2, "true");
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);

    // update brokerage onvisible false
    response =
        HttpMethod.updateBrokerageOnVisible(httpnode, witnessAddress, 88L, witnessKey, "false");
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);

    // update brokerage onvisible false for notwitness
    response = HttpMethod.sendCoin(httpnode, fromAddress, dev001Address, amount, testKey002);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);

    response = HttpMethod.updateBrokerageOnVisible(httpnode, dev001Address, 78L, dev001Key, "true");
    Assert.assertFalse(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
  }

  /** constructor. */
  @Test(enabled = true, description = "GetReward by http", groups = {"daily", "serial"})
  public void test05GetReward() {
    response = HttpMethod.getReward(httpnode, witnessAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(
        (new BigInteger(responseContent.getString("reward")).compareTo(new BigInteger("0"))) == 1);

    response = HttpMethod.getRewardOnVisible(httpnode, witnessAddress2, "true");
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(
        (new BigInteger(responseContent.getString("reward")).compareTo(new BigInteger("0"))) == 1);

    response = HttpMethod.getRewardOnVisible(httpnode, witnessAddress, "false");
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(
        (new BigInteger(responseContent.getString("reward")).compareTo(new BigInteger("0"))) == 1);
  }

  /** constructor. */
  @Test(enabled = true, description = "GetReward from solidity by http", groups = {"daily", "serial"})
  public void test06GetRewardFromSolidity() {
    response = HttpMethod.getRewardFromSolidity(httpSoliditynode, witnessAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(
        (new BigInteger(responseContent.getString("reward")).compareTo(new BigInteger("0"))) == 1);

    response = HttpMethod.getRewardFromSolidityOnVisible(httpSoliditynode, witnessAddress, "true");
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(
        (new BigInteger(responseContent.getString("reward")).compareTo(new BigInteger("0"))) == 1);

    response = HttpMethod.getRewardFromSolidityOnVisible(httpSoliditynode, witnessAddress, "false");
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(
        (new BigInteger(responseContent.getString("reward")).compareTo(new BigInteger("0"))) == 1);
  }

  /** constructor. */
  @Test(enabled = true, description = "GetReward from PBFT by http", groups = {"daily", "serial"})
  public void test07GetRewardFromPbft() {
    response = HttpMethod.getRewardFromPbft(httpPbftNode, witnessAddress);
    responseContent = HttpMethod.parseResponseContent(response);
    HttpMethod.printJsonContent(responseContent);
    Assert.assertTrue(
        (new BigInteger(responseContent.getString("reward")).compareTo(new BigInteger("0")) == 0)
            || (new BigInteger(responseContent.getString("reward")).compareTo(new BigInteger("0")))
                == 1);
  }

  /** constructor. */
  @AfterClass
  public void shutdown() throws InterruptedException {
    // update brokerage
    HttpMethod.freeResource(httpnode, witnessAddress, fromAddress, witnessKey);
    HttpMethod.disConnect();
  }
}
