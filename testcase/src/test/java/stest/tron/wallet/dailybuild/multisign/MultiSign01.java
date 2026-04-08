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
public class MultiSign01 extends TronBaseTest {  private final byte[] witnessAddress001 = PublicMethod.getFinalAddress(witnessKey);
  private long multiSignFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.multiSignFee");
  private long updateAccountPermissionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.updateAccountPermissionFee");
  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] ownerAddress = ecKey1.getAddress();
  private String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
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
  public void beforeClass() {  }

  @Test(enabled = true, description = "Owner permission_name is owner", groups = {"daily", "multisig"})
  public void testOwnerName01() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long needCoin = updateAccountPermissionFee * 2 + multiSignFee;
    Assert.assertTrue(PublicMethod.sendcoin(ownerAddress, needCoin, foundationAddress,
        foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    PublicMethod.printAddress(ownerKey);
    PublicMethod.printAddress(tmpKey02);

    List<String> ownerPermissionKeys = new ArrayList<>();

    ownerPermissionKeys.add(ownerKey);

    logger.info("** update owner and active permission to two address");
  String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
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

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(tmpKey02);
    ownerPermissionKeys.add(witnessKey);

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
  Long balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);
  }

  @Test(enabled = true, description = "Owner permission_name is owner1", groups = {"daily", "multisig"})
  public void testOwnerName02() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
  final byte[] ownerAddress = ecKey1.getAddress();
  final String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
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

    logger.info("** update owner and active permission to two address");
  String accountPermissionJson = "{\"owner_permission\":{\"type\":0,"
        + "\"permission_name\":\"owner1\",\"threshold\":1,\"keys\":["
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

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, PublicMethodForMultiSign.getActivePermissionKeyCount(
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

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
  Long balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);

    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);

  }

  @Test(enabled = true, description = "Owner permission_name is string \"123\"", groups = {"daily", "multisig"})
  public void testOwnerName03() {
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

    logger.info("** update owner and active permission to two address");
  String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"123\",\"threshold\":1,\"keys\":["
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

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, PublicMethodForMultiSign.getActivePermissionKeyCount(
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

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

  @Test(enabled = true, description = "Owner permission_name is string \"\"", groups = {"daily", "multisig"})
  public void testOwnerName04() {
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

    logger.info("** update owner and active permission to two address");
  String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"\",\"threshold\":1,\"keys\":["
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

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, PublicMethodForMultiSign.getActivePermissionKeyCount(
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

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

  @Test(enabled = true, description = "Owner permission_name is null", groups = {"daily", "multisig"})
  public void testOwnerName05() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    Assert.assertTrue(PublicMethod.sendcoin(ownerAddress, 1_000000, foundationAddress,
        foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethod.printAddress(ownerKey);
    PublicMethod.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);
  // null
    String accountPermissionJson = "{\"owner_permission\":{\"type\":0,\"permission_name\":" + null
        + ",\"threshold\":1,\"keys\":["
        + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
        + "\",\"weight\":1}]},"
        + "\"witness_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\""
        + "keys\":[{\"address\":\"" + PublicMethod.getAddressString(witnessKey)
        + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
        + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
        + "\"keys\":["
        + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}"
        + "]}]}";
    boolean ret = false;
    try {
      PublicMethod
          .accountPermissionUpdateForResponse(accountPermissionJson,
              ownerAddress, ownerKey, blockingStubFull);
    } catch (NullPointerException e) {
      logger.info("Expected NullPointerException!");
      ret = true;
    }
    Assert.assertTrue(ret);
  Long balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);

    Assert.assertEquals(balanceBefore, balanceAfter);

  }

  @Test(enabled = true, description = "Owner without permission_name", groups = {"daily", "multisig"})
  public void testOwnerName06() {
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

    logger.info("** update owner and active permission to two address");
  String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"threshold\":1,\"keys\":["
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

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, PublicMethodForMultiSign.getActivePermissionKeyCount(
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

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
  Long balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);

    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);
  }

  @Test(enabled = true, description = "Owner permission_name is numbers", groups = {"daily", "multisig"})
  public void testOwnerName07() {
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
  String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":123,\"threshold\":1,\"keys\":["
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

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, PublicMethodForMultiSign.getActivePermissionKeyCount(
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

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
  Long balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);

    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);

  }

  @Test(enabled = true, description = "Owner permission_name is 0.1", groups = {"daily", "multisig"})
  public void testOwnerName08() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    long needCoin = updateAccountPermissionFee * 2;

    Assert.assertTrue(PublicMethod.sendcoin(ownerAddress, needCoin + 1000000, foundationAddress,
        foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethod.printAddress(ownerKey);
    PublicMethod.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);
  String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"0.1\",\"threshold\":1,\"keys\":["
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

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, PublicMethodForMultiSign.getActivePermissionKeyCount(
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

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
  Long balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);

    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);

  }

  @Test(enabled = true, description = "Owner permission_name length is 32", groups = {"daily", "multisig"})
  public void testOwnerName09() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    long needCoin = updateAccountPermissionFee * 2;

    Assert.assertTrue(PublicMethod.sendcoin(ownerAddress, needCoin + 1000000, foundationAddress,
        foundationKey, blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethod.printAddress(ownerKey);
    PublicMethod.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);
  String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,"
            + "\"permission_name\":\"abcdefghijklmnopqrstuvwxyzabcdef\",\"threshold\":1,\"keys\":["
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

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, PublicMethodForMultiSign.getActivePermissionKeyCount(
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

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


  @Test(enabled = true, description = "Owner permission_name length is 33", groups = {"daily", "multisig"})
  public void testOwnerName10() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    Assert.assertTrue(PublicMethod.sendcoin(ownerAddress, 1000000, foundationAddress,
        foundationKey, blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethod.printAddress(ownerKey);
    PublicMethod.printAddress(tmpKey02);
  String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,"
            + "\"permission_name\":\"abcdefghijklmnopqrstuvwxyzabcdefg\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
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
    Assert.assertEquals("Contract validate error : permission's name is too long",
        response.getMessage().toStringUtf8());
  Long balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);

    Assert.assertEquals(balanceBefore, balanceAfter);

  }

  @Test(enabled = true, description = "Owner permission_name contains special char", groups = {"daily", "multisig"})
  public void testOwnerName11() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    long needCoin = updateAccountPermissionFee * 2;

    Assert.assertTrue(PublicMethod.sendcoin(ownerAddress, needCoin + 1000000, foundationAddress,
        foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethod.printAddress(ownerKey);
    PublicMethod.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);
  String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"0&^%\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0&^%%09\","
            + "\"threshold\":1,"
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

    Assert.assertEquals(2, PublicMethodForMultiSign.getActivePermissionKeyCount(
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1, PublicMethod.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethodForMultiSign.printPermissionList(PublicMethod.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());
    logger.info("1111___" + PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getOwnerPermission().toString());

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
    logger.info("22222___" + PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getOwnerPermission().toString());
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
