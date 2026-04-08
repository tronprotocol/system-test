package stest.tron.wallet.dailybuild.http;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.HttpMethod;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.PublicMethodForMultiSign;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class HttpTestMultiSign001 extends TronBaseTest {

  private final String manager2Key =
      Configuration.getByPath("testng.conf").getString("foundationAccount.key2");
  private final byte[] manager2Address = PublicMethod.getFinalAddress(manager2Key);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] ownerAddress = ecKey1.getAddress();
  String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] receiverAddress = ecKey1.getAddress();
  String receiverKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  byte[] hexTestAddress = ecKey2.getAddress();
  String hexTestKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  String[] permissionKeyString;
  Long amount = 1000000000L;
  JsonArray keys = new JsonArray();
  JsonArray activeKeys = new JsonArray();
  JsonObject manager1Wight = new JsonObject();
  JsonObject manager2Wight = new JsonObject();
  JsonObject manager3Wight = new JsonObject();
  JsonObject manager4Wight = new JsonObject();
  JsonObject ownerObject = new JsonObject();
  JsonObject witnessObject = new JsonObject();
  JsonObject activeObject = new JsonObject();
  private JSONObject responseContent;
  private HttpResponse response;
  private String httpnode =
      Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list").get(1);
  private List<String> list;

  /** constructor. */
  @BeforeClass
  public void beforeClass() {  }

  /** constructor. */
  @Test(enabled = true, description = "Account Permission Up Date by http", groups = {"daily", "serial"})
  public void test1AccountPermissionUpDate() {
    PublicMethod.printAddress(ownerKey);
    response = HttpMethod.sendCoin(httpnode, foundationAddress, ownerAddress, amount, foundationKey);
    Assert.assertTrue(HttpMethod.verificationResult(response));
    HttpMethod.waitToProduceOneBlock(httpnode);
    manager1Wight.addProperty("address", ByteArray.toHexString(foundationAddress));
    manager1Wight.addProperty("weight", 1);

    logger.info(manager1Wight.toString());
    manager2Wight.addProperty("address", ByteArray.toHexString(manager2Address));
    manager2Wight.addProperty("weight", 1);

    logger.info(manager2Wight.toString());

    keys.add(manager1Wight);
    keys.add(manager2Wight);

    ownerObject.addProperty("type", 0);
    ownerObject.addProperty("permission_name", "owner");
    ownerObject.addProperty("threshold", 2);
    ownerObject.add("keys", keys);

    manager3Wight.addProperty("address", ByteArray.toHexString(witnessAddress));
    manager3Wight.addProperty("weight", 1);

    logger.info(manager3Wight.toString());
    manager4Wight.addProperty("address", ByteArray.toHexString(witnessAddress2));
    manager4Wight.addProperty("weight", 1);

    logger.info(manager4Wight.toString());

    activeKeys.add(manager3Wight);
    activeKeys.add(manager4Wight);

    activeObject.addProperty("type", 2);
    activeObject.addProperty("permission_name", "active0");
    activeObject.addProperty("threshold", 2);
    activeObject.addProperty(
        "operations", "7fff1fc0037e0000000000000000000000000000000000000000000000000000");
    activeObject.add("keys", activeKeys);

    response =
        HttpMethod.accountPermissionUpdate(
            httpnode, ownerAddress, ownerObject, witnessObject, activeObject, ownerKey);
    Assert.assertTrue(HttpMethod.verificationResult(response));
  }

  /** constructor. */
  @Test(enabled = true, description = "Add transaction sign by http with permission id", groups = {"daily", "serial"})
  public void test2AddTransactionSign() {

    HttpMethod.waitToProduceOneBlock(httpnode);
    permissionKeyString = new String[2];
    permissionKeyString[0] = foundationKey;
    permissionKeyString[1] = manager2Key;

    String[] permissionKeyActive = new String[2];
    permissionKeyActive[0] = witnessKey;
    permissionKeyActive[1] = witnessKey2;

    response =
        HttpMethod.sendCoin(httpnode, ownerAddress, foundationAddress, 10L, 0, permissionKeyString);
    Assert.assertTrue(HttpMethod.verificationResult(response));

    response =
        HttpMethod.sendCoin(httpnode, ownerAddress, foundationAddress, 10L, 2, permissionKeyString);
    Assert.assertFalse(HttpMethod.verificationResult(response));

    logger.info("start permission id 2");
    response =
        HttpMethod.sendCoin(httpnode, ownerAddress, foundationAddress, 12L, 2, permissionKeyActive);
    Assert.assertTrue(HttpMethod.verificationResult(response));

    response =
        HttpMethod.sendCoin(httpnode, ownerAddress, foundationAddress, 12L, 0, permissionKeyActive);
    Assert.assertFalse(HttpMethod.verificationResult(response));

    response =
        HttpMethod.sendCoin(httpnode, ownerAddress, foundationAddress, 11L, 1, permissionKeyActive);
    Assert.assertFalse(HttpMethod.verificationResult(response));

    response =
        HttpMethod.sendCoin(httpnode, ownerAddress, foundationAddress, 11L, 3, permissionKeyString);
    Assert.assertFalse(HttpMethod.verificationResult(response));
  }

  /** constructor. */
  @Test(
      enabled = true,
      description = "Add broadcasthex http interface to " + "broadcast hex transaction string", groups = {"daily", "serial"})
  public void test3Broadcasthex() {
    PublicMethod.printAddress(hexTestKey);
  String transactionHex =
        PublicMethod.sendcoinGetTransactionHex(
            hexTestAddress, 1000L, foundationAddress, foundationKey, blockingStubFull);
  // Wrong type of hex
    response = HttpMethod.broadcasthex(httpnode, transactionHex);
    Assert.assertTrue(HttpMethod.verificationResult(response));
  String wrongTransactionHex = transactionHex + "wrong";
    response = HttpMethod.broadcasthex(httpnode, wrongTransactionHex);
    logger.info("transaction wrong:");
    Assert.assertFalse(HttpMethod.verificationResult(response));
  // SingleSign for broadcastHex
    response = HttpMethod.broadcasthex(httpnode, transactionHex);
    Assert.assertFalse(HttpMethod.verificationResult(response));
  // Multisign for broadcastHex
    String multiSignTransactionHex =
        PublicMethodForMultiSign.sendcoinGetTransactionHex(
            hexTestAddress, 999L, ownerAddress, ownerKey, blockingStubFull, permissionKeyString);
    response = HttpMethod.broadcasthex(httpnode, multiSignTransactionHex);
    Assert.assertTrue(HttpMethod.verificationResult(response));
  // Hex is null
    response = HttpMethod.broadcasthex(httpnode, "");
    Assert.assertFalse(HttpMethod.verificationResult(response));
  }

  @Test(enabled = true, description = "GetApprovedList  type is wrong by http", groups = {"daily", "serial"})
  public void test4getApprovedListTypeWrong() {

    permissionKeyString = new String[2];
    permissionKeyString[0] = foundationKey;
    permissionKeyString[1] = manager2Key;
  String originType = "\"type\":\"TransferContract\"";
  String type = "\"type\": \"TransferContract1111111\"";
    response =
        HttpMethod.sendCoinReplaceTransactionType(
            httpnode, foundationAddress, receiverAddress, 10L, 0, permissionKeyString, originType, type);
    Assert.assertTrue(!HttpMethod.verificationResult(response));
    responseContent = HttpMethod.parseResponseContent(response);
    Assert.assertEquals(responseContent.getJSONObject("result").getString("code"), "OTHER_ERROR");
    Assert.assertEquals(
        responseContent.getJSONObject("result").getString("message"),
        "Invalid transaction: no valid contract");
  }

  /** constructor. */
  @Test(enabled = true, description = "GetApprovedList without type  by http", groups = {"daily", "serial"})
  public void test5getApprovedListWithoutType() {
    permissionKeyString = new String[2];
    permissionKeyString[0] = foundationKey;
    permissionKeyString[1] = manager2Key;
  String originType = "," + "\"type\":\"TransferContract\"";
  String type = " ";
    response =
        HttpMethod.sendCoinReplaceTransactionType(
            httpnode, foundationAddress, receiverAddress, 10L, 0, permissionKeyString, originType, type);
    Assert.assertTrue(!HttpMethod.verificationResult(response));
    responseContent = HttpMethod.parseResponseContent(response);
    Assert.assertEquals("OTHER_ERROR", responseContent.getJSONObject("result").getString("code"));
    Assert.assertEquals(
        "Invalid transaction: no valid contract",
        responseContent.getJSONObject("result").getString("message"));
  }

  /** constructor. */
  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethod.disConnect();  }
}
