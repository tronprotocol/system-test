package stest.tron.wallet.dailybuild.multisign;

import static org.hamcrest.core.StringContains.containsString;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
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
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.PublicMethodForMultiSign;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class MultiSign37 extends TronBaseTest {
  private ManagedChannel searchChannelFull = null;
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
      "Sendcoin with permission id 0,use owner address sign, weight<threshold.Then use  "
          + " active address to sign, meet all requirements,broadcastTransaction.", groups = {"daily", "multisig"})
  public void testMultiUpdatepermissions_47() {
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
  final Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    long balance = test001AddressAccount.getBalance();
    logger.info("balance:" + balance);
    PublicMethodForMultiSign.printPermissionList(permissionsList);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission));
    dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;
  String accountPermissionJson1 = "{\"owner_permission\":{\"type\":0,\"permission_name\":"
        + "\"owner\",\"threshold\":2,\"keys\":[{\"address\":"
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

    Account test001AddressAccount1 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
  final Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    long balance1 = test001AddressAccount1.getBalance();
    logger.info("balance1:" + balance1);

    PublicMethodForMultiSign.printPermissionList(permissionsList1);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission1));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission1));
    Assert.assertEquals(balance - balance1, updateAccountPermissionFee);

    Transaction transaction = PublicMethodForMultiSign
        .sendcoinWithPermissionIdNotSign(foundationAddress, 1L, test001Address, 0, dev001Key,
            blockingStubFull);

    Transaction transaction1 = PublicMethod
        .addTransactionSign(transaction, dev001Key, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethodForMultiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transactionSignWeight:" + transactionSignWeight);
    Assert
        .assertThat(transactionSignWeight.getResult().getCode().toString(),
            containsString("NOT_ENOUGH_PERMISSION"));
    Transaction transaction2 = PublicMethodForMultiSign
        .addTransactionSignWithPermissionId(transaction1, sendAccountKey3, 2, blockingStubFull);
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
    Assert
        .assertThat(returnResult2.getMessage().toStringUtf8(),
            containsString(
                "Signature count is 2 more than key counts of permission : 1"));
    Assert.assertEquals(balance1, balance3);
  }

  @Test(enabled = true, description =
      "Sendcoin,use owner address sign,  not meet all requirements.Then use  "
          + " active address to sign, not meet all requirements,broadcastTransaction.", groups = {"daily", "multisig"})
  public void testMultiUpdatepermissions_48() {
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
  final Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    long balance = test001AddressAccount.getBalance();
    logger.info("balance:" + balance);
    PublicMethodForMultiSign.printPermissionList(permissionsList);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission));
    dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;
  String accountPermissionJson1 = "{\"owner_permission\":{\"type\":0,\"permission_name\":"
        + "\"owner\",\"threshold\":2,\"keys\":[{\"address\":"
        + "\"" + PublicMethod.getAddressString(dev001Key) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey2) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\","
        + "\"threshold\":1,\"operations\":"
        + "\"0100000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey3)
        + "\",\"weight\":1}]}]} ";
    Assert.assertTrue(PublicMethodForMultiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));

    Account test001AddressAccount1 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
  final Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    long balance1 = test001AddressAccount1.getBalance();
    logger.info("balance1:" + balance1);

    PublicMethodForMultiSign.printPermissionList(permissionsList1);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission1));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission1));
    Assert.assertEquals(balance - balance1, updateAccountPermissionFee);

    Transaction transaction = PublicMethodForMultiSign
        .sendcoinWithPermissionIdNotSign(foundationAddress, 1L, test001Address, 0, dev001Key,
            blockingStubFull);

    Transaction transaction1 = PublicMethod
        .addTransactionSign(transaction, dev001Key, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethodForMultiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transactionSignWeight:" + transactionSignWeight);
    Assert.assertThat(transactionSignWeight.getResult().getCode().toString(),
            containsString("NOT_ENOUGH_PERMISSION"));
    Transaction transaction2 = PublicMethodForMultiSign
        .addTransactionSignWithPermissionId(transaction1, sendAccountKey3, 2, blockingStubFull);
    TransactionSignWeight transactionSignWeight1 = PublicMethodForMultiSign
        .getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("transaction1:" + transactionSignWeight1);
    Assert.assertThat(transactionSignWeight1.getResult().getCode().toString(),
            containsString("PERMISSION_ERROR"));
    Assert.assertThat(transactionSignWeight1.getResult().getMessage(),
            containsString("Permission denied"));
    Return returnResult2 = PublicMethodForMultiSign
        .broadcastTransaction1(transaction2, blockingStubFull);
    logger.info("returnResult2:" + returnResult2);
    Assert
        .assertThat(returnResult2.getCode().toString(), containsString("SIGERROR"));
    Account test001AddressAccount3 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    long balance3 = test001AddressAccount3.getBalance();
    Assert.assertThat(returnResult2.getMessage().toStringUtf8(),
            containsString("Permission denied"));
    Assert.assertEquals(balance1, balance3);
  }

  @Test(enabled = true, description =
      "Sendcoin,use two owner address sign,sum(weight)==threshold,broadcastTransaction.", groups = {"daily", "multisig"})
  public void testMultiUpdatepermissions_49() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    test001Address = ecKey.getAddress();
    long amount = updateAccountPermissionFee + multiSignFee + 1000000;
    Assert.assertTrue(PublicMethod
        .sendcoin(test001Address, amount, foundationAddress, testKey002,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethod.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
  final Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    long balance = test001AddressAccount.getBalance();
    logger.info("balance:" + balance);
    PublicMethodForMultiSign.printPermissionList(permissionsList);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission));
    dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;
  String accountPermissionJson1 = "{\"owner_permission\":{\"type\":0,\"permission_name\":"
        + "\"owner\",\"threshold\":2,\"keys\":[{\"address\":"
        + "\"" + PublicMethod.getAddressString(dev001Key) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey2) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\","
        + "\"threshold\":1,\"operations\":"
        + "\"0100000000000000000000000000000000000000000000000000000000000000\","
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
  final Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    long balance1 = test001AddressAccount1.getBalance();
    logger.info("balance1:" + balance1);

    PublicMethodForMultiSign.printPermissionList(permissionsList1);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission1));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission1));
    Assert.assertEquals(balance - balance1, updateAccountPermissionFee);

    Transaction transaction = PublicMethodForMultiSign
        .sendcoinWithPermissionIdNotSign(foundationAddress, 1L, test001Address, 0, dev001Key,
            blockingStubFull);

    Transaction transaction1 = PublicMethod
        .addTransactionSign(transaction, dev001Key, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethodForMultiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transactionSignWeight:" + transactionSignWeight);
    Assert
        .assertThat(transactionSignWeight.getResult().getCode().toString(),
            containsString("NOT_ENOUGH_PERMISSION"));
    Transaction transaction2 = PublicMethodForMultiSign
        .addTransactionSignWithPermissionId(transaction1, sendAccountKey2, 0, blockingStubFull);
    TransactionSignWeight transactionSignWeight1 = PublicMethodForMultiSign
        .getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("transaction1:" + transactionSignWeight1);
    Assert
        .assertThat(transactionSignWeight1.getResult().getCode().toString(),
            containsString("ENOUGH_PERMISSION"));
    Return returnResult2 = PublicMethodForMultiSign
        .broadcastTransaction1(transaction2, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    logger.info("returnResult2:" + returnResult2);
    Account test001AddressAccount3 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    long balance3 = test001AddressAccount3.getBalance();
    Assert.assertEquals(balance1 - balance3, multiSignFee + 1);
    Assert.assertTrue(returnResult2.getResult());
  }

  /**
   * constructor.
   */
  private Transaction setReference(Transaction transaction, long blockNum,
      byte[] blockHash) {
    byte[] refBlockNum = ByteArray.fromLong(blockNum);
    Transaction.raw rawData = transaction.getRawData().toBuilder()
        .setRefBlockHash(ByteString.copyFrom(blockHash))
        .setRefBlockBytes(ByteString.copyFrom(refBlockNum))
        .build();
    return transaction.toBuilder().setRawData(rawData).build();
    }

  /**
   * constructor.
   */
  public Transaction setExpiration(Transaction transaction, long expiration) {
    Transaction.raw rawData = transaction.getRawData().toBuilder().setExpiration(expiration)
        .build();
    return transaction.toBuilder().setRawData(rawData).build();
    }

  /**
   * constructor.
   */
  public Transaction createTransaction(com.google.protobuf.Message message,
      ContractType contractType) {
    Transaction.raw.Builder transactionBuilder = Transaction.raw.newBuilder().addContract(
        Transaction.Contract.newBuilder().setType(contractType).setParameter(
            Any.pack(message)).build());

    Transaction transaction = Transaction.newBuilder().setRawData(transactionBuilder.build())
        .build();
    long time = System.currentTimeMillis();
    AtomicLong count = new AtomicLong();
    long gtime = count.incrementAndGet() + time;
  String ref = "" + gtime;

    transaction = setReference(transaction, gtime, ByteArray.fromString(ref));

    transaction = setExpiration(transaction, gtime);

    return transaction;
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
