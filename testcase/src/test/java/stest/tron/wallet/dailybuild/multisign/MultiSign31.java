package stest.tron.wallet.dailybuild.multisign;

import static org.hamcrest.core.StringContains.containsString;

import io.grpc.ManagedChannel;
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
import stest.tron.wallet.common.client.utils.MultiNode;

@Slf4j
@MultiNode
public class MultiSign31 extends TronBaseTest {
  private ManagedChannel searchChannelFull = null;
  private String searchFullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);
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

  @Test(enabled = true, description =
      "Sendcoin,use active address sign,  meet all requirements，broadcast，Then use the same"
          + " permissionID active address to sign, meet all requirements,broadcast.", groups = {"daily", "multisig"})
  public void testMultiUpdatepermissions_34() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    test001Address = ecKey.getAddress();
    long amount = updateAccountPermissionFee + 1000000;
    Assert.assertTrue(PublicMethod
        .sendcoin(test001Address, amount, foundationAddress, testKey002,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethod.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    long balance = test001AddressAccount.getBalance();
    logger.info("balance:" + balance);
    PublicMethodForMultiSign.printPermissionList(permissionsList);
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission));
    dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;
  String accountPermissionJson1 = "{\"owner_permission\":{\"type\":0,\"permission_name\":\""
        + "owner\",\"threshold\":1,\"keys\":[{\"address\":\""
        + "" + PublicMethod.getAddressString(dev001Key) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name"
        + "\":\"active0\",\"threshold\":1,\"operations\":\""
        + "0200000000000000000000000000000000000000000000000000000000000000\""
        + ",\"keys\":[{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey2) + "\","
        + "\"weight\":1},{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey3)
        + "\",\"weight\":1}]}]} ";
    Assert.assertTrue(PublicMethodForMultiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));

    Account test001AddressAccount1 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    long balance1 = test001AddressAccount1.getBalance();
    logger.info("balance1:" + balance1);

    PublicMethodForMultiSign.printPermissionList(permissionsList1);
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission1));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission1));
    Assert.assertEquals(balance - balance1, updateAccountPermissionFee);

    Transaction transaction = PublicMethodForMultiSign
        .sendcoinWithPermissionIdNotSign(foundationAddress, 1L, test001Address, 2, dev001Key,
            blockingStubFull);

    Transaction transaction1 = PublicMethod
        .addTransactionSign(transaction, sendAccountKey2, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethodForMultiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transactionSignWeight:" + transactionSignWeight);
    Return returnResult1 = PublicMethodForMultiSign
        .broadcastTransaction1(transaction1, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    logger.info("returnResult1:" + returnResult1);
    Assert.assertTrue(returnResult1.getResult());
    Account test001AddressAccount2 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    long balance2 = test001AddressAccount2.getBalance();
    logger.info("balance2:" + balance2);

    Assert.assertEquals(balance1 - balance2, 1L);
    Transaction transaction2 = PublicMethod
        .addTransactionSign(transaction1, sendAccountKey3, blockingStubFull);
    TransactionSignWeight transactionSignWeight1 = PublicMethodForMultiSign
        .getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("transaction1:" + transactionSignWeight1);

    Return returnResult2 = PublicMethodForMultiSign
        .broadcastTransaction1(transaction2, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    logger.info("returnResult2:" + returnResult2);
    Assert
        .assertThat(returnResult2.getCode().toString(), containsString("DUP_TRANSACTION_ERROR"));
    Account test001AddressAccount3 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    long balance3 = test001AddressAccount3.getBalance();
    Assert.assertEquals(balance3, balance2);

  }

  @Test(enabled = true, description =
      "Sendcoin,use active address sign,"
          + "not meet the requirements broadcastTransaction.Then use the same"
          + " permissionID active address to sign,broadcastTransaction.", groups = {"daily", "multisig"})
  public void testMultiUpdatepermissions_35() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    test001Address = ecKey.getAddress();
    long amount = updateAccountPermissionFee + 1;

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

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;
  String accountPermissionJson1 = "{\"owner_permission\":{\"type\":0,\"permission_name\":\""
        + "owner\",\"threshold\":1,\"keys\":[{\"address\":\""
        + "" + PublicMethod.getAddressString(dev001Key) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name"
        + "\":\"active0\",\"threshold\":1,\"operations\":\""
        + "0100000000000000000000000000000000000000000000000000000000000000\""
        + ",\"keys\":[{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey2) + "\","
        + "\"weight\":1},{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey3)
        + "\",\"weight\":1}]}]} ";
    Assert.assertTrue(PublicMethodForMultiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));

    Account test001AddressAccount1 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    long balance1 = test001AddressAccount1.getBalance();
    Assert.assertEquals(balance - balance1, updateAccountPermissionFee);
    PublicMethodForMultiSign.printPermissionList(permissionsList1);
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
    Assert
        .assertThat(transactionSignWeight.getResult().getCode().toString(),
            containsString("PERMISSION_ERROR"));
    Assert
        .assertThat(transactionSignWeight.getResult().getMessage(),
            containsString("Permission denied"));
    Return returnResult1 = PublicMethodForMultiSign
        .broadcastTransaction1(transaction1, blockingStubFull);
    Account test001AddressAccount2 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    long balance2 = test001AddressAccount2.getBalance();
    logger.info("balance2:" + balance2);
    Assert.assertEquals(balance1, balance2);

    logger.info("returnResult1:" + returnResult1);
    Assert
        .assertThat(returnResult1.getCode().toString(), containsString("SIGERROR"));
    Assert
        .assertThat(returnResult1.getMessage().toStringUtf8().toLowerCase(),
            containsString("Validate signature error: permission denied".toLowerCase()));
    Transaction transaction2 = PublicMethod
        .addTransactionSign(transaction1, sendAccountKey3, blockingStubFull);
    TransactionSignWeight transactionSignWeight1 = PublicMethodForMultiSign
        .getTransactionSignWeight(transaction2, blockingStubFull);

    logger.info("transaction1:" + transactionSignWeight1);

    Assert
        .assertThat(transactionSignWeight1.getResult().getCode().toString(),
            containsString("PERMISSION_ERROR"));
    Assert
        .assertThat(transactionSignWeight1.getResult().getMessage(),
            containsString("Permission denied"));

    Return returnResult2 = PublicMethodForMultiSign
        .broadcastTransaction1(transaction2, blockingStubFull);
    logger.info("returnResult2:" + returnResult2);
    Assert
        .assertThat(returnResult2.getCode().toString(), containsString("SIGERROR"));
    Account test001AddressAccount3 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    long balance3 = test001AddressAccount3.getBalance();
    logger.info("balance3:" + balance3);
    Assert.assertEquals(balance3, balance2);

  }

  @Test(enabled = true, description =
      "Sendcoin,use owner address sign, broadcast,Then use other owner address to sign,broadcast.", groups = {"daily", "multisig"})
  public void testMultiUpdatepermissions_36() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    test001Address = ecKey.getAddress();
    long amount = updateAccountPermissionFee + 1;

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

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;
  String accountPermissionJson1 = "{\"owner_permission\":{\"type\":0,\"permission_name\":"
        + "\"owner\",\"threshold\":1,\"keys\":[{\"address\":"
        + "\"" + PublicMethod.getAddressString(dev001Key) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey2) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\","
        + "\"threshold\":1,\"operations\":"
        + "\"0200000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey3)
        + "\",\"weight\":1}]}]} ";
    Assert.assertTrue(PublicMethodForMultiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount1 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    long balance1 = test001AddressAccount1.getBalance();
    Assert.assertEquals(balance - balance1, updateAccountPermissionFee);
    PublicMethodForMultiSign.printPermissionList(permissionsList1);
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission1));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission1));

    Transaction transaction = PublicMethodForMultiSign
        .sendcoinWithPermissionIdNotSign(foundationAddress, 1, test001Address, 0, dev001Key,
            blockingStubFull);
    Transaction transaction1 = PublicMethod
        .addTransactionSign(transaction, sendAccountKey2, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethodForMultiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transaction:" + transactionSignWeight);

    Return returnResult1 = PublicMethodForMultiSign
        .broadcastTransaction1(transaction1, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(returnResult1.getResult());

    logger.info("returnResult1:" + returnResult1);
    Account test001AddressAccount2 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    long balance2 = test001AddressAccount2.getBalance();
    Assert.assertEquals(balance1 - balance2, 1);
    Transaction transaction2 = PublicMethod
        .addTransactionSign(transaction1, dev001Key, blockingStubFull);
    TransactionSignWeight transactionSignWeight1 = PublicMethodForMultiSign
        .getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("transaction1:" + transactionSignWeight1);

    Return returnResult2 = PublicMethodForMultiSign
        .broadcastTransaction1(transaction2, blockingStubFull);
    logger.info("returnResult1:" + returnResult2);
    Assert
        .assertThat(returnResult2.getCode().toString(), containsString("DUP_TRANSACTION_ERROR"));
    Account test001AddressAccount3 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    long balance3 = test001AddressAccount3.getBalance();
    Assert.assertEquals(balance2, balance3);
  }

  @Test(enabled = true, description =
      "Sendcoin permission id 3,use active address in permission id 2 sign,"
          + "Then use active address"
          + " in permission id 3 to sign, meet all requirements.broadcastTransaction.", groups = {"daily", "multisig"})
  public void testMultiUpdatepermissions_37() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    test001Address = ecKey.getAddress();
    long amount = updateAccountPermissionFee + 1;

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

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;
  String accountPermissionJson1 = "{\"owner_permission\":{\"type\":0,\"permission_name\":"
        + "\"owner\",\"threshold\":1,\"keys\":[{\"address\""
        + ":\"" + PublicMethod.getAddressString(dev001Key) + "\",\"weight\":1}]},"
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
            blockingStubFull, 0,
            permissionKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount1 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    long balance1 = test001AddressAccount1.getBalance();
    Assert.assertEquals(balance - balance1, updateAccountPermissionFee);
    PublicMethodForMultiSign.printPermissionList(permissionsList1);
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission1));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission1));

    Transaction transaction = PublicMethodForMultiSign
        .sendcoinWithPermissionIdNotSign(foundationAddress, 1L, test001Address, 3, dev001Key,
            blockingStubFull);
    Transaction transaction1 = PublicMethod
        .addTransactionSign(transaction, sendAccountKey2, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethodForMultiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transaction:" + transactionSignWeight);
    Assert
        .assertThat(transactionSignWeight.getResult().getCode().toString(),
            containsString("PERMISSION_ERROR"));
    Assert
        .assertThat(transactionSignWeight.getResult().getMessage(),
            containsString("but it is not contained of permission"));
    Return returnResult1 = PublicMethodForMultiSign
        .broadcastTransaction1(transaction1, blockingStubFull);

    logger.info("returnResult1:" + returnResult1);
    Assert
        .assertThat(returnResult1.getCode().toString(), containsString("SIGERROR"));
    Assert
        .assertThat(returnResult1.getMessage().toStringUtf8(),
            containsString("but it is not contained of permission"));
    Account test001AddressAccount2 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    long balance2 = test001AddressAccount2.getBalance();
    Assert.assertEquals(balance1, balance2);
  //Assert.assertTrue(returnResult1.getResult());
    Transaction transaction2 = PublicMethodForMultiSign
        .addTransactionSignWithPermissionId(transaction1, sendAccountKey3, 3, blockingStubFull);
    TransactionSignWeight transactionSignWeight1 = PublicMethodForMultiSign
        .getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("transaction1:" + transactionSignWeight1);
    Assert
        .assertThat(transactionSignWeight1.getResult().getCode().toString(),
            containsString("PERMISSION_ERROR"));
    Assert
        .assertThat(transactionSignWeight1.getResult().getMessage(),
            containsString("Signature count is 2 more than key counts of permission : 1"));

    Return returnResult2 = PublicMethodForMultiSign
        .broadcastTransaction1(transaction2, blockingStubFull);
    logger.info("returnResult2:" + returnResult2);
    Assert
        .assertThat(returnResult2.getCode().toString(), containsString("SIGERROR"));
    Account test001AddressAccount3 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    long balance3 = test001AddressAccount3.getBalance();
    Assert.assertEquals(balance2, balance3);

  }

  @Test(enabled = true, description =
      "Sendcoin,use active address sign meet all requirements,broadcast,Then use active address"
          + "in wrong permission id to sign,not meet the requirements.broadcast.", groups = {"daily", "multisig"})
  public void testMultiUpdatepermissions_38() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    test001Address = ecKey.getAddress();
    long amount = updateAccountPermissionFee + 2;

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

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;
  String accountPermissionJson1 = "{\"owner_permission\":{\"type\":0,\"permission_name\":"
        + "\"owner\",\"threshold\":1,\"keys\":[{\"address\""
        + ":\"" + PublicMethod.getAddressString(dev001Key) + "\",\"weight\":1}]},"
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
            blockingStubFull, 0,
            permissionKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

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
        .sendcoinWithPermissionIdNotSign(foundationAddress, 1, test001Address, 3, dev001Key,
            blockingStubFull);
    Transaction transaction1 = PublicMethod
        .addTransactionSign(transaction, sendAccountKey3, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethodForMultiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transaction:" + transactionSignWeight);
    Return returnResult1 = PublicMethodForMultiSign
        .broadcastTransaction1(transaction1, blockingStubFull);
    logger.info("returnResult1:" + returnResult1);
    Account test001AddressAccount2 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    long balance2 = test001AddressAccount2.getBalance();
    Assert.assertEquals(balance1 - balance2, 1);

    Transaction transaction2 = PublicMethod
        .addTransactionSign(transaction1, sendAccountKey2, blockingStubFull);
    TransactionSignWeight transactionSignWeight1 = PublicMethodForMultiSign
        .getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("transaction1:" + transactionSignWeight1);
    Assert
        .assertThat(transactionSignWeight1.getResult().getCode().toString(),
            containsString("PERMISSION_ERROR"));
    Assert
        .assertThat(transactionSignWeight1.getResult().getMessage(),
            containsString("Signature count is 2 more than key counts of permission : 1"));

    Return returnResult2 = PublicMethodForMultiSign
        .broadcastTransaction1(transaction2, blockingStubFull);
    logger.info("returnResult2:" + returnResult2);
    Assert
        .assertThat(returnResult2.getCode().toString(), containsString("SIGERROR"));
    Account test001AddressAccount3 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    long balance3 = test001AddressAccount3.getBalance();
    Assert.assertEquals(balance3, balance2);

  }

  @Test(enabled = true, description =
      "Sendcoin,use active address sign, meet all requirements,Then use the other permissionID "
          + "in active address to sign, meet all requirements.broadcastTransaction.", groups = {"daily", "multisig"})
  public void testMultiUpdatepermissions_39() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    test001Address = ecKey.getAddress();
    long amount = updateAccountPermissionFee + 2;

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

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;
  String accountPermissionJson1 = "{\"owner_permission\":{\"type\":0,\"permission_name\":"
        + "\"owner\",\"threshold\":1,\"keys\":[{\"address\""
        + ":\"" + PublicMethod.getAddressString(dev001Key) + "\",\"weight\":1}]},"
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
            blockingStubFull, 0,
            permissionKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

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
        .sendcoinWithPermissionIdNotSign(foundationAddress, 1L, test001Address, 3, dev001Key,
            blockingStubFull);
    Transaction transaction1 = PublicMethod
        .addTransactionSign(transaction, sendAccountKey3, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethodForMultiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transaction:" + transactionSignWeight);
    Return returnResult1 = PublicMethodForMultiSign
        .broadcastTransaction1(transaction1, blockingStubFull);
    Account test001AddressAccount2 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    long balance2 = test001AddressAccount2.getBalance();
    Assert.assertEquals(balance1 - balance2, 1L);

    logger.info("returnResult1:" + returnResult1);
    Assert.assertTrue(returnResult1.getResult());
    Transaction transaction2 = PublicMethodForMultiSign
        .addTransactionSignWithPermissionId(transaction1, sendAccountKey2, 2, blockingStubFull);

    TransactionSignWeight transactionSignWeight1 = PublicMethodForMultiSign
        .getTransactionSignWeight(transaction2, blockingStubFull);
    Assert
        .assertThat(transactionSignWeight1.getResult().getCode().toString(),
            containsString("PERMISSION_ERROR"));
    Assert
        .assertThat(transactionSignWeight1.getResult().getMessage(),
            containsString("Signature count is 2 more than key counts of permission : 1"));
    logger.info("transaction1:" + transactionSignWeight1);

    Return returnResult2 = PublicMethodForMultiSign
        .broadcastTransaction1(transaction2, blockingStubFull);
    logger.info("returnResult2:" + returnResult2);
    Assert
        .assertThat(returnResult2.getCode().toString(), containsString("SIGERROR"));
    Account test001AddressAccount3 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    long balance3 = test001AddressAccount3.getBalance();
    Assert.assertEquals(balance3, balance2);

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
