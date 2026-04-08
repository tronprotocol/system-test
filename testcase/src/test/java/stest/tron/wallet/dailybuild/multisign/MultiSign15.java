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
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.PublicMethodForMultiSign;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.MultiNode;

@Slf4j
@MultiNode
public class MultiSign15 extends TronBaseTest {
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
  private String fullnodeLocal = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private byte[] transferTokenContractAddress = null;
  private String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  private String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {    PublicMethod.sendcoin(ownerAddress, 1_000_000, foundationAddress, testKey002, blockingStubFull);
  }

  @Test(enabled = true, description = "Witness doesn't have parent_id", groups = {"daily", "multisig"})
  public void testWitnessParent01() {
    ownerKey = witnessKey;
    ownerAddress = new WalletClient(ownerKey).getAddress();
    long needCoin = updateAccountPermissionFee * 2;

    PublicMethod.sendcoin(ownerAddress, needCoin, foundationAddress, testKey002, blockingStubFull);
    Assert.assertTrue(PublicMethod
        .freezeBalanceForReceiver(foundationAddress, 100000000000L, 0, 0,
            ByteString.copyFrom(ownerAddress),
            testKey002, blockingStubFull));
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
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":1,\"permission_name\":\"witness\","
            + "\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
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
    ownerPermissionKeys.add(testKey002);

    Assert.assertEquals(2,
        PublicMethodForMultiSign.getActivePermissionKeyCount(PublicMethod.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1, PublicMethod.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    Assert.assertEquals(1, PublicMethod.queryAccount(ownerAddress,
        blockingStubFull).getWitnessPermission().getKeysCount());

    PublicMethodForMultiSign.printPermissionList(PublicMethod.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out
        .printf(PublicMethodForMultiSign.printPermission(PublicMethod.queryAccount(ownerAddress,
            blockingStubFull).getOwnerPermission()));

    System.out
        .printf(PublicMethodForMultiSign.printPermission(PublicMethod.queryAccount(ownerAddress,
            blockingStubFull).getWitnessPermission()));

    PublicMethodForMultiSign
        .recoverWitnessPermission(ownerKey, ownerPermissionKeys, blockingStubFull);
  Long balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);
    PublicMethod
        .unFreezeBalance(foundationAddress, testKey002, 0, ownerAddress, blockingStubFull);
  }

  @Test(enabled = true, description = "Witness parent_id is exception condition", groups = {"daily", "multisig"})
  public void testWitnessParent02() {
    ownerKey = witnessKey;
    ownerAddress = new WalletClient(ownerKey).getAddress();
    Assert.assertTrue(PublicMethod
        .freezeBalanceForReceiver(foundationAddress, 100000000000L, 0, 0,
            ByteString.copyFrom(ownerAddress),
            testKey002, blockingStubFull));
    PublicMethod.sendcoin(ownerAddress, 1_000000, foundationAddress, testKey002, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethod.printAddress(ownerKey);
    PublicMethod.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);
  // parent_id = "123"
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"parent_id\":\"123\",\"type\":1,"
            + "\"permission_name\":\"witness\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    GrpcAPI.Return response = PublicMethod.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("Contract validate error : permission's parent should be owner",
        response.getMessage().toStringUtf8());
  // parent_id = 123
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"parent_id\":123,\"type\":1,"
            + "\"permission_name\":\"witness\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    response = PublicMethod.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("Contract validate error : permission's parent should be owner",
        response.getMessage().toStringUtf8());
  // parent_id = "abc"
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"parent_id\":\"abc\",\"type\":1,"
            + "\"permission_name\":\"witness\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
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
    } catch (NumberFormatException e) {
      logger.info("Expected NumberFormatException!");
      ret = true;
    }
    Assert.assertTrue(ret);
  // parent_id = ""
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"parent_id\":\"\",\"type\":1,"
            + "\"permission_name\":\"witness\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";
    ret = false;
    try {
      PublicMethod
          .accountPermissionUpdateForResponse(accountPermissionJson,
              ownerAddress, ownerKey, blockingStubFull);
    } catch (NullPointerException e) {
      logger.info("Expected NullPointerException!");
      ret = true;
    }
    Assert.assertTrue(ret);
  // parent_id = null
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"parent_id\":" + null + ",\"type\":1,"
            + "\"permission_name\":\"witness\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";
    ret = false;
    try {
      PublicMethod
          .accountPermissionUpdateForResponse(accountPermissionJson,
              ownerAddress, ownerKey, blockingStubFull);
    } catch (NullPointerException e) {
      logger.info("Expected NullPointerException!");
      ret = true;
    }
    Assert.assertTrue(ret);
  // parent_id = 1
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"parent_id\":1,\"type\":1,"
            + "\"permission_name\":\"witness\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";
    response = PublicMethod.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("Contract validate error : permission's parent should be owner",
        response.getMessage().toStringUtf8());
  // parent_id = 2
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"parent_id\":2,\"type\":1,"
            + "\"permission_name\":\"witness\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    response = PublicMethod.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("Contract validate error : permission's parent should be owner",
        response.getMessage().toStringUtf8());
  // parent_id = 3
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"parent_id\":3,\"type\":1,"
            + "\"permission_name\":\"witness\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";
    response = PublicMethod.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("Contract validate error : permission's parent should be owner",
        response.getMessage().toStringUtf8());
  // parent_id = -1
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"parent_id\":-1,\"type\":1,"
            + "\"permission_name\":\"witness\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    response = PublicMethod.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("Contract validate error : permission's parent should be owner",
        response.getMessage().toStringUtf8());
  // parent_id = Integer.MAX_VALUE
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"parent_id\":2147483647,\"type\":1,"
            + "\"permission_name\":\"witness\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    response = PublicMethod.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("Contract validate error : permission's parent should be owner",
        response.getMessage().toStringUtf8());
  // parent_id = Integer.MAX_VALUE +1

    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"parent_id\":2147483648,\"type\":1,"
            + "\"permission_name\":\"witness\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";
    response = PublicMethod.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("Contract validate error : permission's parent should be owner",
        response.getMessage().toStringUtf8());
  // parent_id = Integer.MIN_VALUE
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"parent_id\":-2147483648,\"type\":1,"
            + "\"permission_name\":\"witness\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";
    response = PublicMethod.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("Contract validate error : permission's parent should be owner",
        response.getMessage().toStringUtf8());
  // parent_id = Integer.MIN_VALUE -1
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"parent_id\":-2147483649,\"type\":1,"
            + "\"permission_name\":\"witness\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    response = PublicMethod.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("Contract validate error : permission's parent should be owner",
        response.getMessage().toStringUtf8());
  Long balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore, balanceAfter);
    PublicMethod
        .unFreezeBalance(foundationAddress, testKey002, 0, ownerAddress, blockingStubFull);

  }

  @Test(enabled = true, description = "Witness parent_id is 0", groups = {"daily", "multisig"})
  public void testWitnessParent03() {
    ownerKey = witnessKey;
    ownerAddress = new WalletClient(ownerKey).getAddress();
    long needCoin = updateAccountPermissionFee * 2;

    PublicMethod.sendcoin(ownerAddress, needCoin, foundationAddress, testKey002, blockingStubFull);
    Assert.assertTrue(PublicMethod
        .freezeBalanceForReceiver(foundationAddress, 100000000000L, 0, 0,
            ByteString.copyFrom(ownerAddress),
            testKey002, blockingStubFull));

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
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"parent_id\":0,\"type\":1,"
            + "\"permission_name\":\"witness\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
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
    ownerPermissionKeys.add(testKey002);

    Assert.assertEquals(2,
        PublicMethodForMultiSign.getActivePermissionKeyCount(PublicMethod.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1, PublicMethod.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    Assert.assertEquals(1, PublicMethod.queryAccount(ownerAddress,
        blockingStubFull).getWitnessPermission().getKeysCount());

    PublicMethodForMultiSign.printPermissionList(PublicMethod.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out
        .printf(PublicMethodForMultiSign.printPermission(PublicMethod.queryAccount(ownerAddress,
            blockingStubFull).getOwnerPermission()));

    System.out
        .printf(PublicMethodForMultiSign.printPermission(PublicMethod.queryAccount(ownerAddress,
            blockingStubFull).getWitnessPermission()));

    PublicMethodForMultiSign
        .recoverWitnessPermission(ownerKey, ownerPermissionKeys, blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);

    PublicMethod
        .unFreezeBalance(foundationAddress, testKey002, 0, ownerAddress, blockingStubFull);

  }

  @AfterMethod
  public void aftertest() {
    PublicMethod.freeResource(ownerAddress, ownerKey, foundationAddress, blockingStubFull);
    PublicMethod.unFreezeBalance(foundationAddress, testKey002, 0, ownerAddress, blockingStubFull);
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {  }

}
