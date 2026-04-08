package stest.tron.wallet.dailybuild.operationupdate;

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
public class MultiSignUpdateBrokerageTest extends TronBaseTest {
  private static final long now = System.currentTimeMillis();
  private final String operations = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.operations");
  String[] permissionKeyString = new String[2];
  String[] ownerKeyString = new String[2];
  String accountPermissionJson = "";
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] manager1Address = ecKey1.getAddress();
  String manager1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] manager2Address = ecKey2.getAddress();
  String manager2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  private long multiSignFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.multiSignFee");
  private long updateAccountPermissionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.updateAccountPermissionFee");
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);

  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {
    initSolidityChannel();  }

  @Test(enabled = true, groups = {"contract", "daily"})
  public void testMultiSignForUpdateBrokerage() {
    long needcoin = updateAccountPermissionFee * 2 + multiSignFee * 5;
    Assert.assertTrue(PublicMethod
        .sendcoin(witnessAddress2, needcoin + 1000000L, foundationAddress, foundationKey,
            blockingStubFull));

    ecKey1 = new ECKey(Utils.getRandom());
    manager1Address = ecKey1.getAddress();
    manager1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    ecKey2 = new ECKey(Utils.getRandom());
    manager2Address = ecKey2.getAddress();
    manager2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(witnessAddress2, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    permissionKeyString[0] = manager1Key;
    permissionKeyString[1] = manager2Key;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    ownerKeyString[0] = witnessKey2;
    ownerKeyString[1] = foundationKey;

    accountPermissionJson = "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\""
        + ",\"threshold\":2,\"keys\":[{\"address\":\"" + PublicMethod
        .getAddressString(witnessKey2) + "\"," + "\"weight\":1},{\"address\":\"" + PublicMethod
        .getAddressString(foundationKey) + "\",\"weight\":1}]},"
        + "\"witness_permission\":{\"type\":1,\"permission_name\":\"owner\",\"threshold\":1,"
        + "\"keys\":[{\"address\":\"" + PublicMethod.getAddressString(witnessKey2)
        + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
        + "\"operations\":\"7fff1fc0033e0300000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethod.getAddressString(manager1Key)
        + "\",\"weight\":1}," + "{\"address\":\"" + PublicMethod.getAddressString(manager2Key)
        + "\",\"weight\":1}]}]} ";
    logger.info(accountPermissionJson);
    PublicMethodForMultiSign
        .accountPermissionUpdate(accountPermissionJson, witnessAddress2, witnessKey2,
            blockingStubFull, ownerKeyString);
  //Update brokerage

//    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertEquals(PublicMethod.getBrokerage(witnessAddress2, blockingStubFull), 20L);
    Assert.assertTrue(PublicMethodForMultiSign
        .updateBrokerage(witnessAddress2, 70, witnessKey2, 2, permissionKeyString,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
//    PublicMethod.waitProduceNextBlock(blockingStubFull);
  // wait a MaintenanceTimeInterval
    accountPermissionJson = "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\""
        + ",\"threshold\":1,\"keys\":[{\"address\":\"" + PublicMethod
        .getAddressString(witnessKey2) + "\"," + "\"weight\":1}]},"
        + "\"witness_permission\":{\"type\":1,\"permission_name\":\"owner\",\"threshold\":1,"
        + "\"keys\":[{\"address\":\"" + PublicMethod.getAddressString(witnessKey2)
        + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
        + "\"operations\":\"7fff1fc0033e0300000000000000000000000000000000000000000000000000\","
        + "\"keys\":[" + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey2)
        + "\",\"weight\":1}]}]} ";
    logger.info(accountPermissionJson);
    PublicMethodForMultiSign
        .accountPermissionUpdate(accountPermissionJson, witnessAddress2, witnessKey2,
            blockingStubFull, ownerKeyString);
  Long balanceAfter = PublicMethod.queryAccount(witnessAddress2, blockingStubFull).getBalance();
    logger.info("balanceAfter: " + balanceAfter);

  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {  }
}

