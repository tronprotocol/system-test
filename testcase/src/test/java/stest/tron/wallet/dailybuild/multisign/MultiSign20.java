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

@Slf4j
public class MultiSign20 extends TronBaseTest {  private final byte[] witnessAddress001 = PublicMethod.getFinalAddress(witnessKey);
  private final String contractTronDiceAddr = "TMYcx6eoRXnePKT1jVn25ZNeMNJ6828HWk";
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
  private String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  private String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {  }

  @Test(enabled = true, description = "Owner address is witness", groups = {"daily", "multisig"})
  public void testOwnerAddress01() {
    // address = witness
    ownerKey = witnessKey;
    ownerAddress = new WalletClient(ownerKey).getAddress();
    long needCoin = updateAccountPermissionFee * 2;

    PublicMethod
        .freezeBalanceForReceiver(foundationAddress, 50000000L, 0, 1, ByteString.copyFrom(ownerAddress),
            foundationKey, blockingStubFull);
    PublicMethod
        .freezeBalanceForReceiver(foundationAddress, 50000000L, 0, 0, ByteString.copyFrom(ownerAddress),
            foundationKey, blockingStubFull);
    PublicMethod.sendcoin(ownerAddress, needCoin, foundationAddress, foundationKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethod.printAddress(ownerKey);
    ownerPermissionKeys.add(ownerKey);

    logger.info("** update owner and active permission to two address");
  String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(foundationKey) + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":1,\"permission_name\":\"owner\","
            + "\"threshold\":1,\"keys\":[" + "{\"address\":\"" + PublicMethod
            .getAddressString(foundationKey) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey)
            + "\",\"weight\":1}" + "]}]}";

    Assert.assertTrue(PublicMethodForMultiSign
        .accountPermissionUpdate(accountPermissionJson, ownerAddress, ownerKey, blockingStubFull,
            ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(foundationKey);

    Assert.assertEquals(1, PublicMethodForMultiSign.getActivePermissionKeyCount(
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1,
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getOwnerPermission()
            .getKeysCount());

    Assert.assertEquals(1,
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getWitnessPermission()
            .getKeysCount());

    PublicMethodForMultiSign.printPermissionList(
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList());

    System.out.printf(PublicMethodForMultiSign.printPermission(
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getOwnerPermission()));

    System.out.printf(PublicMethodForMultiSign.printPermission(
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getWitnessPermission()));

    logger.info("** trigger a permission transaction");
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey) + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":1,\"permission_name\":\"witness\","
            + "\"threshold\":1,\"keys\":[" + "{\"address\":\"" + PublicMethod
            .getAddressString(ownerKey) + "\",\"weight\":1}]},"
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

  @Test(enabled = true, description = "Owner address is witness with exception condition", groups = {"daily", "multisig"})
  public void testOwnerAddress02() {
    // address = witness, without witness permission
    ownerKey = witnessKey;
    ownerAddress = new WalletClient(ownerKey).getAddress();
    PublicMethod.sendcoin(ownerAddress, 1_000000, foundationAddress, foundationKey, blockingStubFull);
    Assert.assertTrue(PublicMethod.freezeBalanceForReceiver(foundationAddress, 100000000000L, 0, 0,
        ByteString.copyFrom(ownerAddress), foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethod.printAddress(ownerKey);

    ownerPermissionKeys.add(ownerKey);
  String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey)
            + "\",\"weight\":1}" + "]}]}";

    GrpcAPI.Return response = PublicMethod
        .accountPermissionUpdateForResponse(accountPermissionJson, ownerAddress, ownerKey,
            blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("Contract validate error : witness permission is missed",
        response.getMessage().toStringUtf8());
  // address = witness, without active permission
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey) + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":1,\"permission_name\":\"witness\","
            + "\"threshold\":1,\"keys\":[" + "{\"address\":\"" + PublicMethod
            .getAddressString(ownerKey) + "\",\"weight\":1}]}}";
    response = PublicMethod
        .accountPermissionUpdateForResponse(accountPermissionJson, ownerAddress, ownerKey,
            blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("Contract validate error : active permission is missed",
        response.getMessage().toStringUtf8());
  // address = witness, without owner permission
    accountPermissionJson = "{\"witness_permission\":{\"type\":1,\"permission_name\":\"witness\","
        + "\"threshold\":1,\"keys\":[" + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey)
        + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
        + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[" + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey)
        + "\",\"weight\":1}" + "]}]}";
    response = PublicMethod
        .accountPermissionUpdateForResponse(accountPermissionJson, ownerAddress, ownerKey,
            blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("Contract validate error : owner permission is missed",
        response.getMessage().toStringUtf8());
  Long balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore, balanceAfter);
    PublicMethod.unFreezeBalance(foundationAddress, foundationKey, 0, ownerAddress, blockingStubFull);
  }

  @Test(enabled = true, description = "Owner address is normal address with exception condition", groups = {"daily", "multisig"})
  public void testOwnerAddress03() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    Assert.assertTrue(
        PublicMethod.sendcoin(ownerAddress, 1_000_000, foundationAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    PublicMethod.printAddress(ownerKey);
    PublicMethod.printAddress(tmpKey02);
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethod.printAddress(ownerKey);

    ownerPermissionKeys.add(ownerKey);
  // address = normal address, with witness permission
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey) + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":1,\"permission_name\":\"witness\","
            + "\"threshold\":1,\"keys\":[" + "{\"address\":\"" + PublicMethod
            .getAddressString(ownerKey) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey)
            + "\",\"weight\":1}" + "]}]}";

    GrpcAPI.Return response = PublicMethod
        .accountPermissionUpdateForResponse(accountPermissionJson, ownerAddress, ownerKey,
            blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals(
        "Contract validate error : account isn't witness can't set" + " witness permission",
        response.getMessage().toStringUtf8());
  // address = normal address, without owner permission
    accountPermissionJson =
        "{\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey)
            + "\",\"weight\":1}" + "]}]}";

    response = PublicMethod
        .accountPermissionUpdateForResponse(accountPermissionJson, ownerAddress, ownerKey,
            blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("Contract validate error : owner permission is missed",
        response.getMessage().toStringUtf8());
  // address = normal address, without active permission
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey) + "\",\"weight\":1}]}}";

    response = PublicMethod
        .accountPermissionUpdateForResponse(accountPermissionJson, ownerAddress, ownerKey,
            blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("Contract validate error : active permission is missed",
        response.getMessage().toStringUtf8());
  // address = contract address
    byte[] ownerAddress02 = contractTronDiceAddr.getBytes();
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey)
            + "\",\"weight\":1}," + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
            + "\",\"weight\":1}" + "]}]}";
    response = PublicMethod
        .accountPermissionUpdateForResponse(accountPermissionJson, ownerAddress02, ownerKey,
            blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("Contract validate error : invalidate ownerAddress",
        response.getMessage().toStringUtf8());
  // address = not active address
    ECKey ecKeyTmp = new ECKey(Utils.getRandom());
  final byte[] ownerAddressTmp = ecKeyTmp.getAddress();
  final String ownerKeyTmp = ByteArray.toHexString(ecKeyTmp.getPrivKeyBytes());
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    PublicMethod.printAddress(ownerKey);
    PublicMethod.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey)
            + "\",\"weight\":1}," + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
            + "\",\"weight\":1}" + "]}]}";

    response = PublicMethod
        .accountPermissionUpdateForResponse(accountPermissionJson, ownerAddressTmp, ownerKeyTmp,
            blockingStubFull);
    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("Contract validate error : ownerAddress account does not exist",
        response.getMessage().toStringUtf8());
  // address = not exist
    String fakeAddress = "THph9K2M2nLvkianrMGswRhz5hjSA9fuH1";
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey)
            + "\",\"weight\":1}," + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
            + "\",\"weight\":1}" + "]}]}";

    response = PublicMethod
        .accountPermissionUpdateForResponse(accountPermissionJson, fakeAddress.getBytes(), ownerKey,
            blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("Contract validate error : invalidate ownerAddress",
        response.getMessage().toStringUtf8());
  // address = long address
    fakeAddress = "TR3FAbhiSeP7kSh39RjGYpwCqfMDHPMhX4d121";

    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey)
            + "\",\"weight\":1}," + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
            + "\",\"weight\":1}" + "]}]}";

    response = PublicMethod
        .accountPermissionUpdateForResponse(accountPermissionJson, fakeAddress.getBytes(), ownerKey,
            blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("Contract validate error : invalidate ownerAddress",
        response.getMessage().toStringUtf8());
  // address = short address

    fakeAddress = "THph9K2M2nLvkianrMGswRhz5hj";

    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey)
            + "\",\"weight\":1}," + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
            + "\",\"weight\":1}" + "]}]}";

    response = PublicMethod
        .accountPermissionUpdateForResponse(accountPermissionJson, fakeAddress.getBytes(), ownerKey,
            blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("Contract validate error : invalidate ownerAddress",
        response.getMessage().toStringUtf8());
  // address =
    fakeAddress = "";
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey)
            + "\",\"weight\":1}," + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
            + "\",\"weight\":1}" + "]}]}";

    response = PublicMethod
        .accountPermissionUpdateForResponse(accountPermissionJson, fakeAddress.getBytes(), ownerKey,
            blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("Contract validate error : invalidate ownerAddress",
        response.getMessage().toStringUtf8());
  // address = null
    fakeAddress = null;

    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey)
            + "\",\"weight\":1}," + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
            + "\",\"weight\":1}" + "]}]}";

    boolean ret = false;
    try {
      PublicMethod.accountPermissionUpdateForResponse(accountPermissionJson, fakeAddress.getBytes(),
          ownerKey, blockingStubFull);
    } catch (NullPointerException e) {
      logger.info("NullPointerException !");
      ret = true;
    }
    Assert.assertTrue(ret);
  // address = "1ab(*c"
    fakeAddress = "1ab(*c";
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey)
            + "\",\"weight\":1}," + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02)
            + "\",\"weight\":1}" + "]}]}";

    response = PublicMethod
        .accountPermissionUpdateForResponse(accountPermissionJson, fakeAddress.getBytes(), ownerKey,
            blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("Contract validate error : invalidate ownerAddress",
        response.getMessage().toStringUtf8());
  Long balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore, balanceAfter);
  }

  @AfterMethod
  public void aftertest() {
    PublicMethod.unFreezeBalance(foundationAddress, foundationKey, 0, ownerAddress, blockingStubFull);
    PublicMethod.unFreezeBalance(foundationAddress, foundationKey, 1, ownerAddress, blockingStubFull);
    PublicMethod.freeResource(ownerAddress, ownerKey, foundationAddress, blockingStubFull);
  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {  }

}
