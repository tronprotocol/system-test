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
public class MultiSign27 extends TronBaseTest {
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

  @Test(enabled = true, description = "Sendcoin,use active address sign, meet all requirements."
      + "Then use  permissionID not same in activelist address to sign,"
      + "not meet the requirements,broadcastTransaction.", groups = {"daily", "multisig"})
  public void testMultiUpdatepermissions_3() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    test001Address = ecKey.getAddress();
    dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    long amount = updateAccountPermissionFee + 1;

    Assert.assertTrue(
        PublicMethod.sendcoin(test001Address, amount, foundationAddress, testKey002, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethod.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    PublicMethodForMultiSign.printPermissionList(permissionsList);
  final long balance = test001AddressAccount.getBalance();

    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission));

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;
  String accountPermissionJson1 = "{\"owner_permission\":{\"type\":0,\"permission_name\":"
        + "\"owner\",\"threshold\":1,\"keys\":[{\"address\"" + ":\"" + PublicMethod
        .getAddressString(dev001Key) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":"
        + "\"active0\",\"threshold\":1,\"operations"
        + "\":\"0200000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey2)
        + "\",\"weight\":1}]},"
        + "{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,\"operations"
        + "\":\"0200000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey3)
        + "\",\"weight\":1}]}]}";

    Assert.assertTrue(PublicMethodForMultiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0, permissionKeyString));

    Account test001AddressAccount1 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    PublicMethodForMultiSign.printPermissionList(permissionsList1);
    long balance1 = test001AddressAccount1.getBalance();
    Assert.assertEquals(balance - balance1, updateAccountPermissionFee);
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission1));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission1));

    Transaction transaction = PublicMethodForMultiSign
        .sendcoinWithPermissionIdNotSign(foundationAddress, 1L, test001Address, 2, dev001Key,
            blockingStubFull);
    Transaction transaction1 = PublicMethod
        .addTransactionSign(transaction, sendAccountKey2, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethodForMultiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transaction:" + transactionSignWeight);

    Transaction transaction2 = PublicMethod
        .addTransactionSign(transaction1, sendAccountKey3, blockingStubFull);
    TransactionSignWeight transactionSignWeight1 = PublicMethodForMultiSign
        .getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("transaction1:" + transactionSignWeight1);

    Assert.assertThat(transactionSignWeight1.getResult().getCode().toString(),
        containsString("PERMISSION_ERROR"));
    Assert.assertThat(transactionSignWeight1.getResult().getMessage(),
        containsString("Signature count is 2 more than key counts of permission : 1"));

    Return returnResult1 = PublicMethodForMultiSign
        .broadcastTransaction1(transaction2, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    logger.info("returnResult1:" + returnResult1);
    Assert.assertThat(returnResult1.getCode().toString(), containsString("SIGERROR"));
    Assert.assertThat(returnResult1.getMessage().toStringUtf8(),
        containsString("Signature count is 2 more than key counts of permission : 1"));
    Account test001AddressAccount2 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    long balance2 = test001AddressAccount2.getBalance();
    Assert.assertEquals(balance1, balance2);
  }

  @Test(enabled = true, description = "Sendcoin,use active address sign, not meet the requirements."
      + "Then use  permissionID not same in activelist address to sign,"
      + "not meet the requirements,broadcastTransaction.", groups = {"daily", "multisig"})
  public void testMultiUpdatepermissions_4() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    test001Address = ecKey.getAddress();
    dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    long amount = updateAccountPermissionFee + 1;

    Assert.assertTrue(
        PublicMethod.sendcoin(test001Address, amount, foundationAddress, testKey002, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Account test001AddressAccount = PublicMethod.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    PublicMethodForMultiSign.printPermissionList(permissionsList);
  final long balance = test001AddressAccount.getBalance();

    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission));

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;
  String accountPermissionJson1 = "{\"owner_permission\":{\"type\":0,\"permission_name\":"
        + "\"owner\",\"threshold\":1,\"keys\":[{\"address\"" + ":\"" + PublicMethod
        .getAddressString(dev001Key) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":"
        + "\"active0\",\"threshold\":1,\"operations"
        + "\":\"0100000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey2)
        + "\",\"weight\":1}]},"
        + "{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,\"operations"
        + "\":\"0200000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey3)
        + "\",\"weight\":1}]}]}";

    Assert.assertTrue(PublicMethodForMultiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0, permissionKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount1 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    PublicMethodForMultiSign.printPermissionList(permissionsList1);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission1));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission1));
    long balance1 = test001AddressAccount1.getBalance();
    Assert.assertEquals(balance - balance1, updateAccountPermissionFee);
    Transaction transaction = PublicMethodForMultiSign
        .sendcoinWithPermissionIdNotSign(foundationAddress, 1, test001Address, 2, dev001Key,
            blockingStubFull);
    Transaction transaction1 = PublicMethod
        .addTransactionSign(transaction, sendAccountKey2, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethodForMultiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transaction:" + transactionSignWeight);
    Assert.assertThat(transactionSignWeight.getResult().getCode().toString(),
        containsString("PERMISSION_ERROR"));
    Assert.assertThat(transactionSignWeight.getResult().getMessage(),
        containsString("Permission denied"));

    Transaction transaction2 = PublicMethod
        .addTransactionSign(transaction1, sendAccountKey3, blockingStubFull);
    TransactionSignWeight transactionSignWeight1 = PublicMethodForMultiSign
        .getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("transaction1:" + transactionSignWeight1);

    Assert.assertThat(transactionSignWeight1.getResult().getCode().toString(),
        containsString("PERMISSION_ERROR"));
    Assert.assertThat(transactionSignWeight1.getResult().getMessage(),
        containsString("Permission denied"));

    Return returnResult1 = PublicMethodForMultiSign
        .broadcastTransaction1(transaction2, blockingStubFull);
    logger.info("returnResult1:" + returnResult1);
    Assert.assertThat(returnResult1.getCode().toString(), containsString("SIGERROR"));
    Assert
        .assertThat(returnResult1.getMessage().toStringUtf8(), containsString("Permission denied"));
    Account test001AddressAccount2 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    long balance2 = test001AddressAccount2.getBalance();
    Assert.assertEquals(balance1, balance2);
  }

  @Test(enabled = true, description = "Sendcoin,use active address sign, meet all requirements."
      + "Then use  permissionID not same in activelist address to sign,"
      + "meet all requirements,broadcastTransaction.", groups = {"daily", "multisig"})
  public void testMultiUpdatepermissions_6() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    test001Address = ecKey.getAddress();
    dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    long amount = updateAccountPermissionFee + 1;

    Assert.assertTrue(
        PublicMethod.sendcoin(test001Address, amount, foundationAddress, testKey002, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethod.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    PublicMethodForMultiSign.printPermissionList(permissionsList);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission));
  final long balance = test001AddressAccount.getBalance();

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;
  String accountPermissionJson1 = "{\"owner_permission\":{\"type\":0,\"permission_name\":"
        + "\"owner\",\"threshold\":1,\"keys\":[{\"address\"" + ":\"" + PublicMethod
        .getAddressString(dev001Key) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":"
        + "\"active0\",\"threshold\":1,\"operations"
        + "\":\"0200000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey2)
        + "\",\"weight\":1}]},"
        + "{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,\"operations"
        + "\":\"0200000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey3)
        + "\",\"weight\":1}]}]}";

    Assert.assertTrue(PublicMethodForMultiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0, permissionKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount1 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    PublicMethodForMultiSign.printPermissionList(permissionsList1);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission1));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission1));
    long balance1 = test001AddressAccount1.getBalance();
    Assert.assertEquals(balance - balance1, updateAccountPermissionFee);
    Transaction transaction = PublicMethodForMultiSign
        .sendcoinWithPermissionIdNotSign(foundationAddress, 1, test001Address, 3, dev001Key,
            blockingStubFull);
    Transaction transaction1 = PublicMethod
        .addTransactionSign(transaction, sendAccountKey3, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethodForMultiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transaction:" + transactionSignWeight);

    Transaction transaction2 = PublicMethodForMultiSign
        .addTransactionSignWithPermissionId(transaction1, sendAccountKey2, 2, blockingStubFull);
    TransactionSignWeight transactionSignWeight1 = PublicMethodForMultiSign
        .getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("transaction1:" + transactionSignWeight1);

    Assert.assertThat(transactionSignWeight1.getResult().getCode().toString(),
        containsString("PERMISSION_ERROR"));
    Assert.assertThat(transactionSignWeight1.getResult().getMessage(),
        containsString("Signature count is 2 more than key counts of permission : 1"));
    Return returnResult1 = PublicMethodForMultiSign
        .broadcastTransaction1(transaction2, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    logger.info("returnResult1:" + returnResult1);
    Assert.assertThat(returnResult1.getCode().toString(), containsString("SIGERROR"));
    Assert.assertThat(returnResult1.getMessage().toStringUtf8(),
        containsString("Signature count is 2 more than key counts of permission : 1"));
    Account test001AddressAccount2 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    long balance2 = test001AddressAccount2.getBalance();
    Assert.assertEquals(balance1, balance2);
  }

  @Test(enabled = true, description = "Sendcoin,use owner address sign, meet all requirements."
      + "Then use  not in permissionlist address to sign,"
      + "not meet the requirements,broadcastTransaction.", groups = {"daily", "multisig"})
  public void testMultiUpdatepermissions_7() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    test001Address = ecKey.getAddress();
    dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    long amount = updateAccountPermissionFee + 1;

    Assert.assertTrue(
        PublicMethod.sendcoin(test001Address, amount, foundationAddress, testKey002, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethod.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    PublicMethodForMultiSign.printPermissionList(permissionsList);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission));
  final long balance = test001AddressAccount.getBalance();

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;
  String accountPermissionJson1 = "{\"owner_permission\":{\"type\":0,\"permission_name\""
        + ":\"owner\",\"threshold\":1,\"keys\":[{\"address\"" + ":\"" + PublicMethod
        .getAddressString(dev001Key) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":"
        + "\"active0\",\"threshold\":1,\"operations\""
        + ":\"0200000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey2)
        + "\",\"weight\":1}," + "{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey3)
        + "\",\"weight\":1}]}]} ";

    Assert.assertTrue(PublicMethodForMultiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0, permissionKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount1 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    PublicMethodForMultiSign.printPermissionList(permissionsList1);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission1));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission1));
    long balance1 = test001AddressAccount1.getBalance();
    Assert.assertEquals(balance - balance1, updateAccountPermissionFee);
    Transaction transaction = PublicMethodForMultiSign
        .sendcoinWithPermissionIdNotSign(foundationAddress, 1L, test001Address, 0, dev001Key,
            blockingStubFull);
    Transaction transaction1 = PublicMethod
        .addTransactionSign(transaction, dev001Key, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethodForMultiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transaction:" + transactionSignWeight);

    Transaction transaction2 = PublicMethod
        .addTransactionSign(transaction1, sendAccountKey4, blockingStubFull);
    TransactionSignWeight transactionSignWeight1 = PublicMethodForMultiSign
        .getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("transaction1:" + transactionSignWeight1);

    Assert.assertThat(transactionSignWeight1.getResult().getCode().toString(),
        containsString("PERMISSION_ERROR"));
    Assert.assertThat(transactionSignWeight1.getResult().getMessage(),
        containsString("Signature count is 2 more than key counts of permission : 1"));

    Return returnResult1 = PublicMethodForMultiSign
        .broadcastTransaction1(transaction2, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    logger.info("returnResult1:" + returnResult1);
    Assert.assertThat(returnResult1.getCode().toString(), containsString("SIGERROR"));
    Assert.assertThat(returnResult1.getMessage().toStringUtf8(),
        containsString("Signature count is 2 more than key counts of permission : 1"));
    Account test001AddressAccount2 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    long balance2 = test001AddressAccount2.getBalance();
    Assert.assertEquals(balance1, balance2);
  }

  @Test(enabled = true, description = "Sendcoin,use owner address sign, meet all requirements."
      + "Then use in active address to sign,not meet the requirements,broadcastTransaction.", groups = {"daily", "multisig"})
  public void testMultiUpdatepermissions_8() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    test001Address = ecKey.getAddress();
    dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    long amount = updateAccountPermissionFee + 1;

    Assert.assertTrue(
        PublicMethod.sendcoin(test001Address, amount, foundationAddress, testKey002, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethod.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    PublicMethodForMultiSign.printPermissionList(permissionsList);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission));
  final long balance = test001AddressAccount.getBalance();

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;
  String accountPermissionJson1 = "{\"owner_permission\":{\"type\":0,\"permission_name\""
        + ":\"owner\",\"threshold\":1,\"keys\":[{\"address\"" + ":\"" + PublicMethod
        .getAddressString(dev001Key) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":"
        + "\"active0\",\"threshold\":1,\"operations\""
        + ":\"0100000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey2)
        + "\",\"weight\":1}," + "{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey3)
        + "\",\"weight\":1}]}]} ";

    Assert.assertTrue(PublicMethodForMultiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0, permissionKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount1 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    PublicMethodForMultiSign.printPermissionList(permissionsList1);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission1));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission1));
    long balance1 = test001AddressAccount1.getBalance();
    Assert.assertEquals(balance - balance1, updateAccountPermissionFee);
    Transaction transaction = PublicMethodForMultiSign
        .sendcoinWithPermissionIdNotSign(foundationAddress, 1L, test001Address, 0, dev001Key,
            blockingStubFull);
    Transaction transaction1 = PublicMethod
        .addTransactionSign(transaction, dev001Key, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethodForMultiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transaction:" + transactionSignWeight);

    Transaction transaction2 = PublicMethodForMultiSign
        .addTransactionSignWithPermissionId(transaction1, sendAccountKey2, 2, blockingStubFull);
    TransactionSignWeight transactionSignWeight1 = PublicMethodForMultiSign
        .getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("transaction1:" + transactionSignWeight1);

    Assert.assertThat(transactionSignWeight1.getResult().getCode().toString(),
        containsString("PERMISSION_ERROR"));
    Assert.assertThat(transactionSignWeight1.getResult().getMessage(),
        containsString("Permission denied"));

    Return returnResult1 = PublicMethodForMultiSign
        .broadcastTransaction1(transaction2, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    logger.info("returnResult1:" + returnResult1);
    Assert.assertThat(returnResult1.getCode().toString(), containsString("SIGERROR"));
    Assert
        .assertThat(returnResult1.getMessage().toStringUtf8(), containsString("Permission denied"));
    Account test001AddressAccount2 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    long balance2 = test001AddressAccount2.getBalance();
    Assert.assertEquals(balance1, balance2);
  }

  @Test(enabled = true, description = "Sendcoin,use owner address sign, meet all requirements."
      + "Then use in active address to sign, meet all requirements,broadcastTransaction.", groups = {"daily", "multisig"})
  public void testMultiUpdatepermissions_9() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    test001Address = ecKey.getAddress();
    dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    long amount = updateAccountPermissionFee + 1;

    Assert.assertTrue(
        PublicMethod.sendcoin(test001Address, amount, foundationAddress, testKey002, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethod.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    PublicMethodForMultiSign.printPermissionList(permissionsList);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission));
  final long balance = test001AddressAccount.getBalance();

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;
  String accountPermissionJson1 = "{\"owner_permission\":{\"type\":0,\"permission_name\""
        + ":\"owner\",\"threshold\":1,\"keys\":[{\"address\"" + ":\"" + PublicMethod
        .getAddressString(dev001Key) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":"
        + "\"active0\",\"threshold\":1,\"operations\""
        + ":\"0200000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey2)
        + "\",\"weight\":1}," + "{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey3)
        + "\",\"weight\":1}]}]} ";

    Assert.assertTrue(PublicMethodForMultiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0, permissionKeyString));

    Account test001AddressAccount1 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    PublicMethodForMultiSign.printPermissionList(permissionsList1);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission1));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission1));
    long balance1 = test001AddressAccount1.getBalance();
    Assert.assertEquals(balance - balance1, updateAccountPermissionFee);
    Transaction transaction = PublicMethodForMultiSign
        .sendcoinWithPermissionIdNotSign(foundationAddress, 1, test001Address, 0, dev001Key,
            blockingStubFull);
    Transaction transaction1 = PublicMethod
        .addTransactionSign(transaction, dev001Key, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethodForMultiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transaction:" + transactionSignWeight);

    Transaction transaction2 = PublicMethodForMultiSign
        .addTransactionSignWithPermissionId(transaction1, sendAccountKey2, 2, blockingStubFull);
    TransactionSignWeight transactionSignWeight1 = PublicMethodForMultiSign
        .getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("transaction1:" + transactionSignWeight1);

    Assert.assertThat(transactionSignWeight1.getResult().getCode().toString(),
        containsString("PERMISSION_ERROR"));
    Assert.assertThat(transactionSignWeight1.getResult().getMessage(),
        containsString("but it is not contained of permission"));

    Return returnResult1 = PublicMethodForMultiSign
        .broadcastTransaction1(transaction2, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    logger.info("returnResult1:" + returnResult1);
    Assert.assertThat(returnResult1.getCode().toString(), containsString("SIGERROR"));
    Assert.assertThat(returnResult1.getMessage().toStringUtf8(),
        containsString("but it is not contained of permission"));
    Account test001AddressAccount2 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    long balance2 = test001AddressAccount2.getBalance();
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
