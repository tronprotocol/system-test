package stest.tron.wallet.dailybuild.multisign;

import static org.hamcrest.core.StringContains.containsString;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.Return;
import org.tron.api.GrpcAPI.TransactionSignWeight;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Permission;
import org.tron.protos.Protocol.Transaction;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.PublicMethodForMultiSign;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class MultiSign35 extends TronBaseTest {
  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] test001Address = ecKey1.getAddress();
  private String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
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
  private long multiSignFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.multiSignFee");
  private long updateAccountPermissionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.updateAccountPermissionFee");

  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {  }

  @Test(enabled = true, description = "update active operation,"
      + " use active address meet all requirement", groups = {"daily", "multisig"})
  public void testMultiUpdatepermissions_45() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    test001Address = ecKey.getAddress();
    long amount = 2 * updateAccountPermissionFee;

    Assert.assertTrue(PublicMethod
        .sendcoin(test001Address, amount, foundationAddress, testKey002,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethod.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
  final long balance = test001AddressAccount.getBalance();
    PublicMethodForMultiSign.printPermissionList(permissionsList);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission));
    dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    String[] permissionKeyString = new String[5];
    permissionKeyString[0] = dev001Key;
  String accountPermissionJson1 =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\""
            + "keys\":[{\"address\":\"" + PublicMethod.getAddressString(dev001Key)
            + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey2)
            + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey5)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\","
            + "\"threshold\":1,\"operations\":\""
            + "7fff1fc0037e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey4)
            + "\",\"weight\":1}]}]} ";

    Assert.assertTrue(PublicMethodForMultiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount1 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
  final long balance1 = test001AddressAccount1.getBalance();
    PublicMethodForMultiSign.printPermissionList(permissionsList1);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission1));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission1));
    Assert.assertEquals(balance - balance1, updateAccountPermissionFee);
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
    String[] permissionKeyString1 = new String[5];
    permissionKeyString1[0] = sendAccountKey4;
    Transaction transaction = PublicMethodForMultiSign
        .accountPermissionUpdateWithoutSign(accountPermissionJson2, test001Address, dev001Key,
            blockingStubFull,
            permissionKeyString);
    Transaction transaction1 = PublicMethodForMultiSign
        .addTransactionSignWithPermissionId(transaction, sendAccountKey4, 2, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethodForMultiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transactionSignWeight:" + transactionSignWeight);

    Return returnResult = PublicMethodForMultiSign
        .broadcastTransaction1(transaction1, blockingStubFull);
    logger.info("returnResult:" + returnResult);
    Assert.assertTrue(returnResult.getResult());
    Account test001AddressAccount2 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList2 = test001AddressAccount2.getActivePermissionList();
    Permission ownerPermission2 = test001AddressAccount2.getOwnerPermission();
    Permission witnessPermission2 = test001AddressAccount2.getWitnessPermission();
  final long balance2 = test001AddressAccount2.getBalance();
    PublicMethodForMultiSign.printPermissionList(permissionsList2);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission2));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission2));
    Assert.assertEquals(balance1 - balance2, updateAccountPermissionFee);


  }

  @Test(enabled = true, description = "update active operation,"
      + " use active address no right update sign, broadcast", groups = {"daily", "multisig"})
  public void testMultiUpdatepermissions_46() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    test001Address = ecKey.getAddress();
    long amount = updateAccountPermissionFee;

    Assert.assertTrue(PublicMethod
        .sendcoin(test001Address, amount, foundationAddress, testKey002,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethod.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
  final long balance = test001AddressAccount.getBalance();
    PublicMethodForMultiSign.printPermissionList(permissionsList);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission));
    dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    String[] permissionKeyString = new String[5];
    permissionKeyString[0] = dev001Key;
  String accountPermissionJson1 =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\""
            + "keys\":[{\"address\":\"" + PublicMethod.getAddressString(dev001Key)
            + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey2)
            + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey5)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\","
            + "\"threshold\":1,\"operations\":\""
            + "7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey4)
            + "\",\"weight\":1}]}]} ";

    Assert.assertTrue(PublicMethodForMultiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));

    Account test001AddressAccount1 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
  final long balance1 = test001AddressAccount1.getBalance();
    PublicMethodForMultiSign.printPermissionList(permissionsList1);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission1));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission1));
    Assert.assertEquals(balance - balance1, updateAccountPermissionFee);
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
    String[] permissionKeyString1 = new String[5];
    permissionKeyString1[0] = sendAccountKey4;
    Transaction transaction = PublicMethodForMultiSign
        .accountPermissionUpdateWithoutSign(accountPermissionJson2, test001Address, dev001Key,
            blockingStubFull,
            permissionKeyString);
    Transaction transaction1 = PublicMethodForMultiSign
        .addTransactionSignWithPermissionId(transaction, sendAccountKey4, 2, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethodForMultiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transactionSignWeight:" + transactionSignWeight);
    Assert
        .assertThat(transactionSignWeight.getResult().getCode().toString(),
            containsString("PERMISSION_ERROR"));
    Assert
        .assertThat(transactionSignWeight.getResult().getMessage(),
            containsString("Permission denied"));
    Return returnResult = PublicMethodForMultiSign
        .broadcastTransaction1(transaction1, blockingStubFull);
    logger.info("returnResult:" + returnResult);

    Assert
        .assertThat(returnResult.getCode().toString(), containsString("SIGERROR"));
    Assert
        .assertThat(returnResult.getMessage().toStringUtf8(),
            containsString("Permission denied"));
    Account test001AddressAccount2 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList2 = test001AddressAccount2.getActivePermissionList();
    Permission ownerPermission2 = test001AddressAccount2.getOwnerPermission();
    Permission witnessPermission2 = test001AddressAccount2.getWitnessPermission();
  final long balance2 = test001AddressAccount2.getBalance();
    PublicMethodForMultiSign.printPermissionList(permissionsList2);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission2));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission2));
    Assert.assertEquals(balance1, balance2);


  }

  @AfterMethod
  public void aftertest() {
    PublicMethod.freeResource(test001Address, dev001Key, foundationAddress, blockingStubFull);
  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {  }


}
