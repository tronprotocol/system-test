package stest.tron.wallet.dailybuild.multisign;

import static org.tron.api.GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR;

import com.google.protobuf.ByteString;
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

@Slf4j
public class MultiSign07 extends TronBaseTest {
  private static final long now = System.currentTimeMillis();
  private static final long TotalSupply = 1000L;
  private static String tokenName = "testAssetIssue_" + Long.toString(now);
  private static ByteString assetAccountId = null;
  private final byte[] witnessAddress001 = PublicMethod.getFinalAddress(witnessKey);
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
  private byte[] transferTokenContractAddress = null;
  private String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  private String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {  }

  @Test(enabled = true, description = "Owner permission_name is owner", groups = {"daily", "multisig"})
  public void testActiveName01() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    long needCoin = updateAccountPermissionFee + multiSignFee;

    Assert.assertTrue(PublicMethod
        .sendcoin(ownerAddress, needCoin + 1000000, foundationAddress, testKey002, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    PublicMethod.printAddress(ownerKey);
    PublicMethod.printAddress(tmpKey02);

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();
    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(witnessKey);
    activePermissionKeys.add(tmpKey02);
  String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"owner\",\"threshold\":2,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey)
            + "\",\"weight\":1}," + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
            + "\",\"weight\":1}" + "]}]}";

    Assert.assertTrue(PublicMethodForMultiSign
        .accountPermissionUpdate(accountPermissionJson, ownerAddress, ownerKey, blockingStubFull,
            ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, PublicMethodForMultiSign.getActivePermissionKeyCount(
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1,
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getOwnerPermission()
            .getKeysCount());

    PublicMethodForMultiSign.printPermissionList(
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList());

    System.out.printf(PublicMethodForMultiSign.printPermission(
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getOwnerPermission()));

    logger.info("** trigger a normal transaction");
    Assert.assertTrue(PublicMethodForMultiSign
        .sendcoinWithPermissionId(foundationAddress, 1_000000, ownerAddress, 2, ownerKey,
            blockingStubFull,
            activePermissionKeys.toArray(new String[activePermissionKeys.size()])));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    long balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceAfter: " + balanceAfter);

    Assert.assertEquals(balanceBefore - balanceAfter, needCoin + 1000000);

  }

  @Test(enabled = true, description = "Owner permission_name is active", groups = {"daily", "multisig"})
  public void testActiveName02() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long needCoin = updateAccountPermissionFee + multiSignFee;

    Assert.assertTrue(PublicMethod
        .sendcoin(ownerAddress, needCoin + 1_000000, foundationAddress, testKey002, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    PublicMethod.printAddress(ownerKey);
    PublicMethod.printAddress(tmpKey02);

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();
    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(witnessKey);
    activePermissionKeys.add(tmpKey02);
  String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":2,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey)
            + "\",\"weight\":1}," + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
            + "\",\"weight\":1}" + "]}]}";
    Assert.assertTrue(PublicMethodForMultiSign
        .accountPermissionUpdate(accountPermissionJson, ownerAddress, ownerKey, blockingStubFull,
            ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, PublicMethodForMultiSign.getActivePermissionKeyCount(
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1,
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getOwnerPermission()
            .getKeysCount());

    PublicMethodForMultiSign.printPermissionList(
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList());

    System.out.printf(PublicMethodForMultiSign.printPermission(
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getOwnerPermission()));

    logger.info("** trigger a normal transaction");
    Assert.assertTrue(PublicMethodForMultiSign
        .sendcoinWithPermissionId(foundationAddress, 1_000000, ownerAddress, 2, ownerKey,
            blockingStubFull,
            activePermissionKeys.toArray(new String[activePermissionKeys.size()])));
  Long balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin + 1000000);

  }

  @Test(enabled = true, description = "Owner permission_name is activea", groups = {"daily", "multisig"})
  public void testActiveName03() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long needCoin = updateAccountPermissionFee + multiSignFee;

    Assert.assertTrue(PublicMethod
        .sendcoin(ownerAddress, needCoin + 1_000_000, foundationAddress, testKey002, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    PublicMethod.printAddress(ownerKey);
    PublicMethod.printAddress(tmpKey02);

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();
    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(witnessKey);
    activePermissionKeys.add(tmpKey02);
  String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"activea\",\"threshold\":2,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey)
            + "\",\"weight\":1}," + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
            + "\",\"weight\":1}" + "]}]}";
    Assert.assertTrue(PublicMethodForMultiSign
        .accountPermissionUpdate(accountPermissionJson, ownerAddress, ownerKey, blockingStubFull,
            ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, PublicMethodForMultiSign.getActivePermissionKeyCount(
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1,
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getOwnerPermission()
            .getKeysCount());

    PublicMethodForMultiSign.printPermissionList(
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList());

    System.out.printf(PublicMethodForMultiSign.printPermission(
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getOwnerPermission()));

    logger.info("** trigger a normal transaction");
    Assert.assertTrue(PublicMethodForMultiSign
        .sendcoinWithPermissionId(foundationAddress, 1_000000, ownerAddress, 2, ownerKey,
            blockingStubFull,
            activePermissionKeys.toArray(new String[activePermissionKeys.size()])));
  Long balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin + 1000000);
  }

  @Test(enabled = true, description = "Owner permission_name is \"123\"", groups = {"daily", "multisig"})
  public void testActiveName04() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long needCoin = updateAccountPermissionFee + multiSignFee;

    Assert.assertTrue(PublicMethod
        .sendcoin(ownerAddress, needCoin + 1_000_000, foundationAddress, testKey002, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    PublicMethod.printAddress(ownerKey);
    PublicMethod.printAddress(tmpKey02);

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();
    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(witnessKey);
    activePermissionKeys.add(tmpKey02);
  String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"123\",\"threshold\":2,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey)
            + "\",\"weight\":1}," + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
            + "\",\"weight\":1}" + "]}]}";
    Assert.assertTrue(PublicMethodForMultiSign
        .accountPermissionUpdate(accountPermissionJson, ownerAddress, ownerKey, blockingStubFull,
            ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, PublicMethodForMultiSign.getActivePermissionKeyCount(
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1,
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getOwnerPermission()
            .getKeysCount());

    PublicMethodForMultiSign.printPermissionList(
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList());

    System.out.printf(PublicMethodForMultiSign.printPermission(
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getOwnerPermission()));

    logger.info("** trigger a normal transaction");
    Assert.assertTrue(PublicMethodForMultiSign
        .sendcoinWithPermissionId(foundationAddress, 1_000000, ownerAddress, 2, ownerKey,
            blockingStubFull,
            activePermissionKeys.toArray(new String[activePermissionKeys.size()])));
  Long balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin + 1000000);
  }

  @Test(enabled = true, description = "Owner permission_name is \"\"", groups = {"daily", "multisig"})
  public void testActiveName05() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long needCoin = updateAccountPermissionFee + multiSignFee;

    Assert.assertTrue(PublicMethod
        .sendcoin(ownerAddress, needCoin + 1_000_000, foundationAddress, testKey002, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    PublicMethod.printAddress(ownerKey);
    PublicMethod.printAddress(tmpKey02);

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();
    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(witnessKey);
    activePermissionKeys.add(tmpKey02);
  String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"\",\"threshold\":2,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey)
            + "\",\"weight\":1}," + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
            + "\",\"weight\":1}" + "]}]}";

    Assert.assertTrue(PublicMethodForMultiSign
        .accountPermissionUpdate(accountPermissionJson, ownerAddress, ownerKey, blockingStubFull,
            ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, PublicMethodForMultiSign.getActivePermissionKeyCount(
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1,
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getOwnerPermission()
            .getKeysCount());

    PublicMethodForMultiSign.printPermissionList(
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList());

    System.out.printf(PublicMethodForMultiSign.printPermission(
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getOwnerPermission()));

    logger.info("** trigger a normal transaction");
    Assert.assertTrue(PublicMethodForMultiSign
        .sendcoinWithPermissionId(foundationAddress, 1_000000, ownerAddress, 2, ownerKey,
            blockingStubFull,
            activePermissionKeys.toArray(new String[activePermissionKeys.size()])));
  Long balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin + 1000000);
  }

  @Test(enabled = true, description = "Owner permission_name in exception condition", groups = {"daily", "multisig"})
  public void testActiveName06() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    Assert.assertTrue(
        PublicMethod.sendcoin(ownerAddress, 1_000_000, foundationAddress, testKey002, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethod.printAddress(ownerKey);
    PublicMethod.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);
  String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":" + null
            + ",\"threshold\":2,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey)
            + "\",\"weight\":1}," + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
            + "\",\"weight\":1}" + "]}]}";

    boolean ret = false;
    try {
      GrpcAPI.Return response = PublicMethod
          .accountPermissionUpdateForResponse(accountPermissionJson, ownerAddress, ownerKey,
              blockingStubFull);
    } catch (NullPointerException e) {
      logger.info("Expected NullPointerException!");
      ret = true;
    }
    Assert.assertTrue(ret);
  // name =
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":,\"threshold\":2,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey)
            + "\",\"weight\":1}," + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
            + "\",\"weight\":1}" + "]}]}";

    ret = false;
    try {
      PublicMethod.accountPermissionUpdateForResponse(accountPermissionJson, ownerAddress, ownerKey,
          blockingStubFull);
    } catch (com.alibaba.fastjson.JSONException e) {
      logger.info("Expected com.alibaba.fastjson.JSONException!");
      ret = true;
    }
    Assert.assertTrue(ret);
  Long balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceAfter: " + balanceAfter);

    Assert.assertEquals(balanceBefore, balanceAfter);
  }

  @Test(enabled = true, description = "Owner permission_name is 1.1", groups = {"daily", "multisig"})
  public void testActiveName07() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long needCoin = updateAccountPermissionFee + multiSignFee;

    Assert.assertTrue(PublicMethod
        .sendcoin(ownerAddress, needCoin + 1_000_000, foundationAddress, testKey002, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    PublicMethod.printAddress(ownerKey);
    PublicMethod.printAddress(tmpKey02);

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();
    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(witnessKey);
    activePermissionKeys.add(tmpKey02);
  String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":1.1,\"threshold\":2,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey)
            + "\",\"weight\":1}," + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
            + "\",\"weight\":1}" + "]}]}";
    Assert.assertTrue(PublicMethodForMultiSign
        .accountPermissionUpdate(accountPermissionJson, ownerAddress, ownerKey, blockingStubFull,
            ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, PublicMethodForMultiSign.getActivePermissionKeyCount(
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1,
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getOwnerPermission()
            .getKeysCount());

    PublicMethodForMultiSign.printPermissionList(
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList());

    System.out.printf(PublicMethodForMultiSign.printPermission(
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getOwnerPermission()));

    logger.info("** trigger a normal transaction");
    Assert.assertTrue(PublicMethodForMultiSign
        .sendcoinWithPermissionId(foundationAddress, 1_000000, ownerAddress, 2, ownerKey,
            blockingStubFull,
            activePermissionKeys.toArray(new String[activePermissionKeys.size()])));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin + 1000000);
  }

  @Test(enabled = true, description = "Active permission_name length is 32", groups = {"daily", "multisig"})
  public void testActiveName08() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    long needCoin = updateAccountPermissionFee * 2;

    Assert.assertTrue(PublicMethod
        .sendcoin(ownerAddress, needCoin + 1000000, foundationAddress, testKey002, blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethod.printAddress(ownerKey);
    PublicMethod.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);
  String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,"
            + "\"permission_name\":\"abcdefghijklmnopqrstuvwxyzabcdef\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey)
            + "\",\"weight\":1}," + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
            + "\",\"weight\":1}" + "]}]}";

    Assert.assertTrue(PublicMethodForMultiSign
        .accountPermissionUpdate(accountPermissionJson, ownerAddress, ownerKey, blockingStubFull,
            ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, PublicMethodForMultiSign.getActivePermissionKeyCount(
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1,
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getOwnerPermission()
            .getKeysCount());

    PublicMethodForMultiSign.printPermissionList(
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList());

    System.out.printf(PublicMethodForMultiSign.printPermission(
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getOwnerPermission()));

    logger.info("** trigger a permission transaction");
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey)
            + "\",\"weight\":1}" + "]}]}";

    Assert.assertTrue(PublicMethodForMultiSign
        .accountPermissionUpdate(accountPermissionJson, ownerAddress, ownerKey, blockingStubFull,
            ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceAfter: " + balanceAfter);

    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);

  }


  @Test(enabled = true, description = "Active permission_name length is 33", groups = {"daily", "multisig"})
  public void testActiveName09() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    Assert.assertTrue(
        PublicMethod.sendcoin(ownerAddress, 1000000, foundationAddress, testKey002, blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethod.printAddress(ownerKey);
    PublicMethod.printAddress(tmpKey02);
  String accountPermissionJson = "{\"owner_permission\":{\"type\":0,"
        + "\"permission_name\":\"owner001\",\"threshold\":1,\"keys\":[" + "{\"address\":\""
        + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,"
        + "\"permission_name\":\"abcdefghijklmnopqrstuvwxyzabcdefg\",\"threshold\":1,"
        + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[" + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey)
        + "\",\"weight\":1}," + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
        + "\",\"weight\":1}" + "]}]}";

    GrpcAPI.Return response = PublicMethod
        .accountPermissionUpdateForResponse(accountPermissionJson, ownerAddress, ownerKey,
            blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("Contract validate error : permission's name is too long",
        response.getMessage().toStringUtf8());
  Long balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceAfter: " + balanceAfter);

    Assert.assertEquals(balanceBefore, balanceAfter);

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
