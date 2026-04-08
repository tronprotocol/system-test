package stest.tron.wallet.onlinestress;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.PublicMethodForMultiSign;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class MultiSignStress extends TronBaseTest {  ByteString assetAccountId1;
  String[] permissionKeyString = new String[2];
  String[] ownerKeyString = new String[1];
  String accountPermissionJson = "";
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] manager1Address = ecKey1.getAddress();
  String manager1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] manager2Address = ecKey2.getAddress();
  String manager2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] ownerAddress = ecKey3.getAddress();
  String ownerKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
  ECKey ecKey4 = new ECKey(Utils.getRandom());
  byte[] newAddress = ecKey4.getAddress();
  String newKey = ByteArray.toHexString(ecKey4.getPrivKeyBytes());

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {  }

  @Test(enabled = true, threadPoolSize = 20, invocationCount = 20, groups = {"stress"})
  public void testMultiSignForAccount() {
    Integer i = 0;
    while (i < 20) {
      ecKey1 = new ECKey(Utils.getRandom());
      manager1Address = ecKey1.getAddress();
      manager1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

      ecKey2 = new ECKey(Utils.getRandom());
      manager2Address = ecKey2.getAddress();
      manager2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

      ecKey3 = new ECKey(Utils.getRandom());
      ownerAddress = ecKey3.getAddress();
      ownerKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
      PublicMethod.printAddress(ownerKey);

      ecKey4 = new ECKey(Utils.getRandom());
      newAddress = ecKey4.getAddress();
      newKey = ByteArray.toHexString(ecKey4.getPrivKeyBytes());

      PublicMethod.sendcoin(ownerAddress, 4000000L, foundationAddress, foundationKey,
          blockingStubFull);
      PublicMethod.sendcoin(ownerAddress, 4000000L, foundationAddress, foundationKey,
          blockingStubFull);
      PublicMethod.sendcoin(ownerAddress, 4000000L, foundationAddress, foundationKey,
          blockingStubFull);
      permissionKeyString[0] = manager1Key;
      permissionKeyString[1] = manager2Key;
      ownerKeyString[0] = ownerKey;
      accountPermissionJson = "[{\"keys\":[{\"address\":\""
          + PublicMethod.getAddressString(ownerKey)
          + "\",\"weight\":2}],\"name\":\"owner\",\"threshold\":2,\"parent\":\"owner\"},"
          + "{\"parent\":\"owner\",\"keys\":[{\"address\":\""
          + PublicMethod.getAddressString(manager1Key) + "\",\"weight\":1},{\"address\":\""
          + PublicMethod.getAddressString(manager2Key) + "\",\"weight\":1}],\"name\":\"active\","
          + "\"threshold\":2}]";
  //logger.info(accountPermissionJson);
      PublicMethodForMultiSign.accountPermissionUpdate(accountPermissionJson, ownerAddress, ownerKey,
          blockingStubFull, ownerKeyString);
  String updateName = Long.toString(System.currentTimeMillis());

      PublicMethodForMultiSign.sendcoin(newAddress, 1000000L, ownerAddress, ownerKey,
          blockingStubFull, permissionKeyString);
      PublicMethodForMultiSign.sendcoin(newAddress, 1000000L, ownerAddress, ownerKey,
          blockingStubFull, permissionKeyString);
      PublicMethodForMultiSign.sendcoin(newAddress, 1000000L, ownerAddress, ownerKey,
          blockingStubFull, permissionKeyString);
      PublicMethodForMultiSign.freezeBalance(ownerAddress, 1000000L, 0,
          ownerKey, blockingStubFull, permissionKeyString);
      PublicMethodForMultiSign.freezeBalance(ownerAddress, 1000000L, 0,
          ownerKey, blockingStubFull, permissionKeyString);
      PublicMethodForMultiSign.freezeBalance(ownerAddress, 1000000L, 0,
          ownerKey, blockingStubFull, permissionKeyString);
      PublicMethodForMultiSign.unFreezeBalance(ownerAddress, ownerKey, 0, null,
          blockingStubFull, permissionKeyString);
      PublicMethodForMultiSign.unFreezeBalance(ownerAddress, ownerKey, 0, null,
          blockingStubFull, permissionKeyString);
      PublicMethodForMultiSign.unFreezeBalance(ownerAddress, ownerKey, 0, null,
          blockingStubFull, permissionKeyString);
    }


  }

  /**
   * constructor.
   */
  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {  }
}


