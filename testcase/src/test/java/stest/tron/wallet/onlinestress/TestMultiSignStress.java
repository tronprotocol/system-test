package stest.tron.wallet.onlinestress;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.PublicMethodForMultiSign;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class TestMultiSignStress extends TronBaseTest {

  ByteString assetAccountId1;
  String[] ownerKeyString = new String[1];
  String accountPermissionJson = "";

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {  }

  @Test(enabled = true, threadPoolSize = 30, invocationCount = 30, groups = {"stress"})
  public void testMultiSignForAccount() {
    PublicMethod.printAddress(testKey002);
  ECKey ecKey4 = new ECKey(Utils.getRandom());
  byte[] newAddress = ecKey4.getAddress();
  String newKey = ByteArray.toHexString(ecKey4.getPrivKeyBytes());
  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] ownerAddress = ecKey3.getAddress();

    Assert.assertTrue(PublicMethod.sendcoin(ownerAddress, 9968981537400L, foundationAddress, testKey002,
        blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] manager1Address = ecKey1.getAddress();
  String manager1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    String[] permissionKeyString = new String[3];
    permissionKeyString[0] = manager1Key;
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] manager2Address = ecKey2.getAddress();
  String manager2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
    permissionKeyString[1] = manager2Key;
  String ownerKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
    permissionKeyString[2] = ownerKey;
    String[] ownerKeyString = new String[1];
    ownerKeyString[0] = ownerKey;

    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":3,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(manager2Key) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(manager2Key) + "\",\"weight\":1}"
            + "]}]}";
    logger.info(accountPermissionJson);
    PublicMethodForMultiSign.accountPermissionUpdate(accountPermissionJson, ownerAddress, ownerKey,
        blockingStubFull, ownerKeyString);
  //permissionKeyString[0] = ownerKey;

    String[] ownerKeyString1 = new String[3];
    ownerKeyString1[0] = ownerKey;
    ownerKeyString1[1] = manager1Key;
    ownerKeyString1[2] = manager2Key;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Integer i = 0;
    while (i++ <= 1000) {
      ecKey4 = new ECKey(Utils.getRandom());
      newAddress = ecKey4.getAddress();
      newKey = ByteArray.toHexString(ecKey4.getPrivKeyBytes());
      PublicMethod.printAddress(newKey);

      PublicMethodForMultiSign.sendcoin(
          newAddress, 4000000L, ownerAddress, ownerKey, blockingStubFull, ownerKeyString1);
      PublicMethodForMultiSign.freezeBalance(
          ownerAddress, 1000000L, 0, ownerKey, blockingStubFull, ownerKeyString1);
      PublicMethodForMultiSign.freezeBalanceGetEnergy(
          ownerAddress, 1000000L, 0, 1, ownerKey, blockingStubFull, ownerKeyString1);
      PublicMethodForMultiSign.freezeBalanceForReceiver(
          ownerAddress, 1000000L, 0, 0, ByteString.copyFrom(newAddress),
          ownerKey, blockingStubFull, ownerKeyString1);
      PublicMethodForMultiSign.unFreezeBalance(
          ownerAddress, ownerKey, 0, null, blockingStubFull, ownerKeyString1);
      PublicMethodForMultiSign.unFreezeBalance(
          ownerAddress, ownerKey, 0, newAddress, blockingStubFull, ownerKeyString1);
      PublicMethodForMultiSign.updateAccount(
          ownerAddress, Long.toString(System.currentTimeMillis()).getBytes(), ownerKey,
          blockingStubFull, ownerKeyString1);
    }


  }

  /**
   * constructor.
   */
  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {  }
}


