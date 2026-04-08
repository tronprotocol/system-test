package stest.tron.wallet.onlinestress;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Permission;
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
public class CycleMultiSign extends TronBaseTest {

  private ManagedChannel searchChannelFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidityInFullnode = null;
  private WalletGrpc.WalletBlockingStub searchBlockingStubFull = null;
  private String searchFullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private ManagedChannel channelSolidityInFullnode = null;
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);
  private ECKey ecKey = new ECKey(Utils.getRandom());
  private byte[] test001Address = ecKey.getAddress();
  private String dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());
  private ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] test002Address = ecKey2.getAddress();
  private String sendAccountKey2 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  private ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] test003Address = ecKey3.getAddress();
  String sendAccountKey3 = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
  private ECKey ecKey4 = new ECKey(Utils.getRandom());
  byte[] test004Address = ecKey4.getAddress();
  String sendAccountKey4 = ByteArray.toHexString(ecKey4.getPrivKeyBytes());
  private ECKey ecKey5 = new ECKey(Utils.getRandom());
  byte[] test005Address = ecKey5.getAddress();
  String sendAccountKey5 = ByteArray.toHexString(ecKey5.getPrivKeyBytes());

  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {    searchChannelFull = ManagedChannelBuilder.forTarget(searchFullnode)
        .usePlaintext()
        .build();
    searchBlockingStubFull = WalletGrpc.newBlockingStub(searchChannelFull);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext()
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);

  }

  //(use no id)
  @Test(enabled = true, groups = {"stress"})
  public void testMultiSignActiveAddress() {
    Assert.assertTrue(PublicMethod
        .sendcoin(test001Address, 10000000000000L, foundationAddress, testKey002,
            blockingStubFull));

    Account test001AddressAccount = PublicMethod.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    PublicMethodForMultiSign.printPermissionList(permissionsList);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission));
    logger.info("wei-----------------------");

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;
  String accountPermissionJson1 =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\""
            + "keys\":[{\"address\":\"" + PublicMethod.getAddressString(dev001Key)
            + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey2)
            + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey3)
            + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey4)
            + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey5)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\","
            + "\"threshold\":1,\"operations\":\""
            + "0200000000000000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[{\"address\":\"" + PublicMethod.getAddressString(dev001Key)
            + "\",\"weight\":1}]}]} ";

    Account test001AddressAccount1 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    PublicMethodForMultiSign.printPermissionList(permissionsList1);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission1));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission1));
    logger.info("1-----------------------");
  String accountPermissionJson2 =
        "{\"owner_permission\":{\"type\":0,\"permission_name\""
            + ":\"owner\",\"threshold\":1,\"keys\":[{\"address\""
            + ":\"" + PublicMethod.getAddressString(dev001Key) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":"
            + "\"active0\",\"threshold\":1,\"operations\""
            + ":\"0200000000000000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey4)
            + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey3)
            + "\",\"weight\":1}]}]} ";
  String accountPermissionJson3 = "{\"owner_permission\":{\"type\":0,\"permission_name\":"
        + "\"owner\",\"threshold\":1,\"keys\":[{\"address\""
        + ":\"" + PublicMethod.getAddressString(dev001Key) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":"
        + "\"active0\",\"threshold\":1,\"operations"
        + "\":\"0100000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethod.getAddressString(dev001Key)
        + "\",\"weight\":1}]},"
        + "{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,\"operations"
        + "\":\"0100000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey3)
        + "\",\"weight\":1}]}]}";
  String accountPermissionJson4 = "{\"owner_permission\":{\"type\":0,\""
        + "permission_name\":\"owner\",\"threshold\":1,\"keys\":"
        + "[{\"address\":\"" + PublicMethod.getAddressString(dev001Key) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
        + "\"operations\":\"0200000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey3)
        + "\",\"weight\":1}]},"
        + "{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,\"operations\":"
        + "\"0200000000000000000000000000000000000000000000000000000000000000\",\"keys\":"
        + "[{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey4)
        + "\",\"weight\":1}]},{\"type\":2,"
        + "\"permission_name\":\"active0\",\"threshold\":1,\"operations\""
        + ":\"0200000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey2)
        + "\",\"weight\":1}]},"
        + "{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,\"operations\""
        + ":\"0100000000000000000000000000000000000000000000000000000000000000\",\"keys\""
        + ":[{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey3)
        + "\",\"weight\":1}]},"
        + "{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,\"operations\":"
        + "\"0200000000000000000000000000000000000000000000000000000000000000\",\"keys\""
        + ":[{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey2)
        + "\",\"weight\":1}]},{\"type\":2,"
        + "\"permission_name\":\"active0\",\"threshold\":1,\"operations\":\""
        + "0200000000000000000000000000000000000000000000000000000000000000\",\"keys\":"
        + "[{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey3)
        + "\",\"weight\":1}]},"
        + "{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,\"operations\""
        + ":\"0200000000000000000000000000000000000000000000000000000000000000\",\""
        + "keys\":[{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey5)
        + "\",\"weight\":1}]},"
        + "{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,\"operations\":"
        + "\"0020000000000000000000000000000000000000000000000000000000000000\",\"keys\""
        + ":[{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey3)
        + "\",\"weight\":1}]}]}";
    while (true) {
      PublicMethodForMultiSign
          .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address,
              dev001Key,
              blockingStubFull, 0,
              permissionKeyString);
      addressPermission(dev001Key, accountPermissionJson2);
      addressPermission(dev001Key, accountPermissionJson3);
      addressPermission(dev001Key, accountPermissionJson4);
      Account test001AddressAccount2 = PublicMethod.queryAccount(test001Address, blockingStubFull);
      List<Permission> permissionsList2 = test001AddressAccount2.getActivePermissionList();
      Permission ownerPermission2 = test001AddressAccount2.getOwnerPermission();
      Permission witnessPermission2 = test001AddressAccount2.getWitnessPermission();
      PublicMethodForMultiSign.printPermissionList(permissionsList2);
      logger.info(PublicMethodForMultiSign.printPermission(ownerPermission2));
      logger.info(PublicMethodForMultiSign.printPermission(witnessPermission2));
    }
  }

  /**
   * constructor.
   */
  public void addressPermission(String addKey, String accountPermissionJson) {
    PublicMethod.freezeBalanceForReceiver(test001Address,
        10000000L, 0, 0,
        com.google.protobuf.ByteString.copyFrom(foundationAddress), testKey002, blockingStubFull);

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = addKey;
    PublicMethodForMultiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString);
    PublicMethod.unFreezeBalance(foundationAddress, testKey002, 0,
        test001Address, blockingStubFull);
  }


  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {    if (searchChannelFull != null) {
      searchChannelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


}
