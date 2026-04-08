package stest.tron.wallet.dailybuild.multisign;

import static org.tron.api.GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.PublicMethodForMultiSign;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.MultiNode;

@Slf4j
@MultiNode
public class MultiSign04 extends TronBaseTest {  private final byte[] witnessAddress001 = PublicMethod.getFinalAddress(witnessKey);
  private long multiSignFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.multiSignFee");
  private long updateAccountPermissionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.updateAccountPermissionFee");
  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] ownerAddress = ecKey1.getAddress();
  private String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ECKey ecKey2 = new ECKey(Utils.getRandom());
  private byte[] normalAddr001 = ecKey2.getAddress();
  private String normalKey001 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  private ECKey tmpEcKey01 = new ECKey(Utils.getRandom());
  private byte[] tmpAddr01 = tmpEcKey01.getAddress();
  private String tmpKey01 = ByteArray.toHexString(tmpEcKey01.getPrivKeyBytes());
  private ECKey tmpEcKey02 = new ECKey(Utils.getRandom());
  private byte[] tmpAddr02 = tmpEcKey02.getAddress();
  private String tmpKey02 = ByteArray.toHexString(tmpEcKey02.getPrivKeyBytes());
  private String fullnodeLocal = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  private String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {    PublicMethod.sendcoin(ownerAddress, 1_000_000, foundationAddress, foundationKey, blockingStubFull);
  }

  @Test(enabled = true, description = "Owner weight in exception condition", groups = {"daily", "multisig"})
  public void testOwnerWeight01() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    Assert.assertTrue(PublicMethod.sendcoin(ownerAddress, 1_000_000, foundationAddress,
        foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    PublicMethod.printAddress(ownerKey);
    PublicMethod.printAddress(tmpKey02);

    List<String> ownerPermissionKeys = new ArrayList<>();
    ownerPermissionKeys.add(ownerKey);
  // weight = Integer.MIN_VALUE
    String accountPermissionJson = "{\"owner_permission\":{\"type\":0,"
        + ",\"threshold\":1,\"keys\":["
        + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey)
        + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey)
        + "\",\"weight\":-2147483647},"
        + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
        + "\",\"weight\":2147483647}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
        + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
        + "\"keys\":["
        + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}"
        + "]}]}";

    GrpcAPI.Return response = PublicMethod.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("Contract validate error : key's weight"
            + " should be greater than 0",
        response.getMessage().toStringUtf8());
  // weight = 0
    accountPermissionJson = "{\"owner_permission\":{\"type\":0,"
        + ",\"threshold\":1,\"keys\":["
        + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey)
        + "\",\"weight\":3},"
        + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey)
        + "\",\"weight\":0},"
        + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
        + "\",\"weight\":0}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
        + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
        + "\"keys\":["
        + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}"
        + "]}]}";
    response = PublicMethod.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("Contract validate error : key's weight"
            + " should be greater than 0",
        response.getMessage().toStringUtf8());
  // weight = -1
    accountPermissionJson = "{\"owner_permission\":{\"type\":0,"
        + ",\"threshold\":1,\"keys\":["
        + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey)
        + "\",\"weight\":3},"
        + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey)
        + "\",\"weight\":-1},"
        + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
        + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
        + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
        + "\"keys\":["
        + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}"
        + "]}]}";
    response = PublicMethod.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("Contract validate error : key's weight"
            + " should be greater than 0",
        response.getMessage().toStringUtf8());
  // weight = long.min
    accountPermissionJson = "{\"owner_permission\":{\"type\":0,"
        + ",\"threshold\":1,\"keys\":["
        + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey)
        + "\",\"weight\":3},"
        + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey)
        + "\",\"weight\":-9223372036854775808},"
        + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
        + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
        + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
        + "\"keys\":["
        + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}"
        + "]}]}";
    response = PublicMethod.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("Contract validate error : key's weight"
            + " should be greater than 0",
        response.getMessage().toStringUtf8());
  // weight = long.min - 1000020
    accountPermissionJson = "{\"owner_permission\":{\"type\":0,"
        + ",\"threshold\":1,\"keys\":["
        + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey)
        + "\",\"weight\":3},"
        + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey)
        + "\",\"weight\":-9223372036855775828},"
        + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
        + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
        + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
        + "\"keys\":["
        + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}"
        + "]}]}";
    boolean ret = false;
    try {
      PublicMethod.accountPermissionUpdateForResponse(
          accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    } catch (NumberFormatException e) {
      logger.info("NumberFormatException !");
      ret = true;
    }
    Assert.assertTrue(ret);
  // weight = "12a"
    accountPermissionJson = "{\"owner_permission\":{\"type\":0,"
        + ",\"threshold\":1,\"keys\":["
        + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey)
        + "\",\"weight\":3},"
        + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey)
        + "\",\"weight\":\"12a\"},"
        + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
        + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
        + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
        + "\"keys\":["
        + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}"
        + "]}]}";
    ret = false;
    try {
      PublicMethod.accountPermissionUpdateForResponse(
          accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    } catch (NumberFormatException e) {
      logger.info("NumberFormatException !");
      ret = true;
    }
    Assert.assertTrue(ret);
  // weight = ""
    accountPermissionJson = "{\"owner_permission\":{\"type\":0,"
        + ",\"threshold\":1,\"keys\":["
        + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey)
        + "\",\"weight\":3},"
        + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey)
        + "\",\"weight\":\"\"},"
        + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
        + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
        + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
        + "\"keys\":["
        + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}"
        + "]}]}";
    ret = false;
    try {
      PublicMethod.accountPermissionUpdateForResponse(
          accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    } catch (NumberFormatException e) {
      logger.info("NumberFormatException !");
      ret = true;
    }
    Assert.assertTrue(ret);
  // weight =
    accountPermissionJson = "{\"owner_permission\":{\"type\":0,"
        + ",\"threshold\":1,\"keys\":["
        + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey)
        + "\",\"weight\":3},"
        + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey)
        + "\",\"weight\":},"
        + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
        + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
        + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
        + "\"keys\":["
        + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}"
        + "]}]}";

    ret = false;
    try {
      PublicMethod.accountPermissionUpdateForResponse(
          accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    } catch (com.alibaba.fastjson.JSONException e) {
      logger.info("JSONException !");
      ret = true;
    }
    Assert.assertTrue(ret);
  // weight = null
    accountPermissionJson = "{\"owner_permission\":{\"type\":0,"
        + ",\"threshold\":1,\"keys\":["
        + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey)
        + "\",\"weight\":3},"
        + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey)
        + "\",\"weight\":" + null + "},"
        + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
        + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
        + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
        + "\"keys\":["
        + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}"
        + "]}]}";
    ret = false;
    try {
      PublicMethod.accountPermissionUpdateForResponse(
          accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    } catch (NumberFormatException e) {
      logger.info("NumberFormatException !");
      ret = true;
    }
    Assert.assertTrue(ret);
  // sum(weight) > Long.MAX_VALUE
    accountPermissionJson = "{\"owner_permission\":{\"type\":0,"
        + ",\"threshold\":1,\"keys\":["
        + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey)
        + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
        + "\",\"weight\":9223372036854775807}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
        + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
        + "\"keys\":["
        + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}"
        + "]}]}";
    response = PublicMethod.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("Contract validate error : long overflow",
        response.getMessage().toStringUtf8());
  // weight = 1.1
    accountPermissionJson = "{\"owner_permission\":{\"type\":0,"
        + ",\"threshold\":1,\"keys\":["
        + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
        + "\",\"weight\":1.1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
        + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
        + "\"keys\":["
        + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}"
        + "]}]}";

    ret = false;
    try {
      PublicMethod.accountPermissionUpdateForResponse(
          accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    } catch (NumberFormatException e) {
      logger.info("NumberFormatException !");
      ret = true;
    }
    Assert.assertTrue(ret);
  Long balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);

    Assert.assertEquals(balanceBefore, balanceAfter);

  }

  @Test(enabled = true, description = "Owner weight is 1", groups = {"daily", "multisig"})
  public void testOwnerWeight02() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long needCoin = updateAccountPermissionFee * 2;

    Assert.assertTrue(PublicMethod.sendcoin(ownerAddress, needCoin, foundationAddress,
        foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethod.printAddress(ownerKey);
    PublicMethod.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);
  String accountPermissionJson = "{\"owner_permission\":{\"type\":0,"
        + ",\"threshold\":1,\"keys\":["
        + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey)
        + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
        + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
        + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
        + "\"keys\":["
        + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}"
        + "]}]}";

    Assert.assertTrue(PublicMethodForMultiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.add(tmpKey02);

    Assert.assertEquals(2,
        PublicMethodForMultiSign.getActivePermissionKeyCount(PublicMethod.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethod.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethodForMultiSign.printPermissionList(PublicMethod.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out
        .printf(PublicMethodForMultiSign.printPermission(PublicMethod.queryAccount(ownerAddress,
            blockingStubFull).getOwnerPermission()));

    logger.info("** trigger a permission transaction");
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethodForMultiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);

    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);
  }


  @Test(enabled = true, description = "Owner weight is Long.MAX_VALUE", groups = {"daily", "multisig"})
  public void testOwnerWeight04() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long needCoin = updateAccountPermissionFee * 2;

    Assert.assertTrue(PublicMethod.sendcoin(ownerAddress, needCoin, foundationAddress,
        foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethod.printAddress(ownerKey);
    PublicMethod.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);
  String accountPermissionJson = "{\"owner_permission\":{\"type\":0,"
        + ",\"threshold\":1,\"keys\":["
        + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
        + "\",\"weight\":9223372036854775807}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
        + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
        + "\"keys\":["
        + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}"
        + "]}]}";

    Assert.assertTrue(PublicMethodForMultiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(tmpKey02);

    Assert.assertEquals(2,
        PublicMethodForMultiSign.getActivePermissionKeyCount(PublicMethod.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1, PublicMethod.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethodForMultiSign.printPermissionList(PublicMethod.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out
        .printf(PublicMethodForMultiSign.printPermission(PublicMethod.queryAccount(ownerAddress,
            blockingStubFull).getOwnerPermission()));

    logger.info("** trigger a permission transaction");
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethodForMultiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);

    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);
  }

  @AfterMethod
  public void aftertest() {
    PublicMethod.freeResource(ownerAddress, ownerKey, foundationAddress, blockingStubFull);
  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {  }

}
