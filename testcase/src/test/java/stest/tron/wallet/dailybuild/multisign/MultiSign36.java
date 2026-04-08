package stest.tron.wallet.dailybuild.multisign;

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
import org.tron.api.GrpcAPI.TransactionApprovedList;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Permission;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.BalanceContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.PublicMethodForMultiSign;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class MultiSign36 extends TronBaseTest {
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

  @Test(enabled = true, description = "two owner address sign update permission transaction "
      + "sum(weight)==threshold,get approve list", groups = {"daily", "multisig"})
  public void getTransactionApprovedList_01() {
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
    PublicMethodForMultiSign.printPermissionList(permissionsList);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission));
    dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    String[] permissionKeyString = new String[2];
    permissionKeyString[0] = dev001Key;
    permissionKeyString[1] = sendAccountKey2;
  String accountPermissionJson1 = "{\"owner_permission\":{\"type\":0,\"permission_name\":"
        + "\"owner\",\"threshold\":2,\"keys\":[{\"address\":"
        + "\"" + PublicMethod.getAddressString(dev001Key) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey2) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\","
        + "\"threshold\":1,\"operations\":"
        + "\"0200000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey3)
        + "\",\"weight\":1}]}]} ";

    Transaction transaction = PublicMethodForMultiSign
        .accountPermissionUpdateWithoutSign(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull,
            permissionKeyString);

    Transaction transaction1 = PublicMethodForMultiSign
        .addTransactionSignWithPermissionId(transaction, dev001Key, 0, blockingStubFull);
    Transaction transaction2 = PublicMethod
        .addTransactionSign(transaction1, sendAccountKey2, blockingStubFull);
    TransactionApprovedList transactionApprovedList = PublicMethod
        .getTransactionApprovedList(transaction2, blockingStubFull);
    logger.info("test001Address:" + Base58.encode58Check(test001Address));
    logger.info(
        "transactionApprovedList:" + Base58
            .encode58Check(transactionApprovedList.getApprovedList(0).toByteArray()));
    Assert.assertEquals(Base58.encode58Check(test001Address), Base58
        .encode58Check(transactionApprovedList.getApprovedList(0).toByteArray()));
    Assert.assertEquals(Base58.encode58Check(test002Address), Base58
        .encode58Check(transactionApprovedList.getApprovedList(1).toByteArray()));
    Assert.assertEquals(2, transactionApprovedList.getApprovedListCount());
    Assert.assertEquals(0,
        transactionApprovedList.getTransaction().getTransaction().getRawData().getContract(0)
            .getPermissionId());
    Account test001AddressAccount1 = PublicMethod
        .queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    PublicMethodForMultiSign.printPermissionList(permissionsList1);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission1));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission1));

  }

  @Test(enabled = true, description =
      "sendcoin,use active address sign meet all requirement,delete "
      + "the used active address,"
      + "and change active no right to sendcoin. get first transaction approve list", groups = {"daily", "multisig"})
  public void getTransactionApprovedList_02() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    test001Address = ecKey.getAddress();
    long amount = 2 * updateAccountPermissionFee + 1;

    Assert.assertTrue(PublicMethod
        .sendcoin(test001Address, amount, foundationAddress, testKey002,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethod.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    PublicMethodForMultiSign.printPermissionList(permissionsList);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission));
    dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;
  String accountPermissionJson1 =
        "{\"owner_permission\":{\"type\":0,\"permission_name\""
            + ":\"owner\",\"threshold\":1,\"keys\":[{\"address\""
            + ":\"" + PublicMethod.getAddressString(dev001Key) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":"
            + "\"active0\",\"threshold\":1,\"operations\""
            + ":\"0200000000000000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey2)
            + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey3)
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
    PublicMethodForMultiSign.printPermissionList(permissionsList1);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission1));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission1));

    Transaction transaction = PublicMethodForMultiSign
        .sendcoinWithPermissionIdNotSign(foundationAddress, 1L, test001Address, 2, dev001Key,
            blockingStubFull);
    Transaction transaction1 = PublicMethodForMultiSign
        .addTransactionSignWithPermissionId(transaction, sendAccountKey2, 2, blockingStubFull);
  String accountPermissionJson2 =
        "{\"owner_permission\":{\"type\":0,\"permission_name\""
            + ":\"owner\",\"threshold\":1,\"keys\":[{\"address\""
            + ":\"" + PublicMethod.getAddressString(dev001Key) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":"
            + "\"active0\",\"threshold\":1,\"operations\""
            + ":\"0100000000000000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey4)
            + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey3)
            + "\",\"weight\":1}]}]} ";
    Assert.assertTrue(PublicMethodForMultiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson2, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));
    TransactionApprovedList transactionApprovedList = PublicMethod
        .getTransactionApprovedList(transaction1, blockingStubFull);

    logger.info("transactionSignWeight:" + transactionApprovedList);
    logger.info("transactionSignWeight:" + transactionApprovedList);
    logger.info("test001Address:" + Base58.encode58Check(test001Address));
    logger.info(
        "transactionApprovedList:" + Base58
            .encode58Check(transactionApprovedList.getApprovedList(0).toByteArray()));
    Assert.assertEquals(Base58.encode58Check(test002Address), Base58
        .encode58Check(transactionApprovedList.getApprovedList(0).toByteArray()));
    Assert.assertEquals(1, transactionApprovedList.getApprovedListCount());
    Assert.assertEquals(2,
        transactionApprovedList.getTransaction().getTransaction().getRawData().getContract(0)
            .getPermissionId());
    Account test001AddressAccount2 = PublicMethod.queryAccount(sendAccountKey2, blockingStubFull);
    List<Permission> permissionsList2 = test001AddressAccount2.getActivePermissionList();
    Permission ownerPermission2 = test001AddressAccount2.getOwnerPermission();
    Permission witnessPermission2 = test001AddressAccount2.getWitnessPermission();
    PublicMethodForMultiSign.printPermissionList(permissionsList2);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission2));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission2));

  }

  @Test(enabled = true, description = "sendcoin,use active address sign,"
      + "sum(weight)==threshold,get approve list", groups = {"daily", "multisig"})
  public void getTransactionApprovedList_03() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    test001Address = ecKey.getAddress();
    long amount = updateAccountPermissionFee + 1 + 1100000;

    Assert.assertTrue(PublicMethod
        .sendcoin(test001Address, amount, foundationAddress, testKey002,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethod.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    PublicMethodForMultiSign.printPermissionList(permissionsList);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission));
    dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;
  String accountPermissionJson1 =
        "{\"owner_permission\":{\"type\":0,\"permission_name\""
            + ":\"owner\",\"threshold\":1,\"keys\":[{\"address\""
            + ":\"" + PublicMethod.getAddressString(dev001Key) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":"
            + "\"active0\",\"threshold\":2,\"operations\""
            + ":\"0200000000000000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey2)
            + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey3)
            + "\",\"weight\":1}]}]} ";

    Assert.assertTrue(PublicMethodForMultiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));

    Account test001AddressAccount1 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    PublicMethodForMultiSign.printPermissionList(permissionsList1);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission1));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission1));

    Transaction transaction = PublicMethodForMultiSign
        .sendcoinWithPermissionIdNotSign(test005Address, 1L, test001Address, 2, dev001Key,
            blockingStubFull);
    Transaction transaction1 = PublicMethod
        .addTransactionSign(transaction, sendAccountKey2, blockingStubFull);
    Transaction transaction2 = PublicMethod
        .addTransactionSign(transaction1, sendAccountKey3, blockingStubFull);
    TransactionApprovedList transactionApprovedList = PublicMethod
        .getTransactionApprovedList(transaction2, blockingStubFull);

    logger.info("transactionSignWeight:" + transactionApprovedList);
    logger.info("test001Address:" + Base58.encode58Check(test001Address));
    logger.info(
        "transactionApprovedList:" + Base58
            .encode58Check(transactionApprovedList.getApprovedList(0).toByteArray()));
    Assert.assertEquals(Base58.encode58Check(test002Address), Base58
        .encode58Check(transactionApprovedList.getApprovedList(0).toByteArray()));
    Assert.assertEquals(Base58.encode58Check(test003Address), Base58
        .encode58Check(transactionApprovedList.getApprovedList(1).toByteArray()));
    Assert.assertEquals(2, transactionApprovedList.getApprovedListCount());
    Assert.assertEquals(2,
        transactionApprovedList.getTransaction().getTransaction().getRawData().getContract(0)
            .getPermissionId());
  }

  @Test(enabled = true, description = "Multi-signature unfinished transaction,get approve list", groups = {"daily", "multisig"})
  public void getTransactionApprovedList_06() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    test001Address = ecKey.getAddress();
    long amount = updateAccountPermissionFee + 1 + 1100000;
    Assert.assertTrue(PublicMethod
        .sendcoin(test001Address, amount, foundationAddress, testKey002,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethod.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    PublicMethodForMultiSign.printPermissionList(permissionsList);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission));
    dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;
  String accountPermissionJson1 =
        "{\"owner_permission\":{\"type\":0,\"permission_name\""
            + ":\"owner\",\"threshold\":1,\"keys\":[{\"address\""
            + ":\"" + PublicMethod.getAddressString(dev001Key) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":"
            + "\"active0\",\"threshold\":2,\"operations\""
            + ":\"0200000000000000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey2)
            + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey3)
            + "\",\"weight\":1}]}]} ";

    Assert.assertTrue(PublicMethodForMultiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));

    Account test001AddressAccount1 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    PublicMethodForMultiSign.printPermissionList(permissionsList1);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission1));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission1));

    Transaction transaction = PublicMethodForMultiSign
        .sendcoinWithPermissionIdNotSign(test005Address, 1L, test001Address, 2, dev001Key,
            blockingStubFull);
    Transaction transaction1 = PublicMethod
        .addTransactionSign(transaction, sendAccountKey2, blockingStubFull);
    TransactionApprovedList transactionApprovedList = PublicMethod
        .getTransactionApprovedList(transaction1, blockingStubFull);

    logger.info("transactionSignWeight:" + transactionApprovedList);
    logger.info("test001Address:" + Base58.encode58Check(test001Address));

    Assert.assertEquals(1, transactionApprovedList.getApprovedListCount());
    Assert.assertEquals(Base58.encode58Check(test002Address), Base58
        .encode58Check(transactionApprovedList.getApprovedList(0).toByteArray()));
    Assert.assertEquals(2,
        transactionApprovedList.getTransaction().getTransaction().getRawData().getContract(0)
            .getPermissionId());
  }

  @Test(enabled = true, description = "sendcoin transaction in owner permission,"
      + "but sign use active address,"
      + "get approve list", groups = {"daily", "multisig"})
  public void getTransactionApprovedList_07() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    test001Address = ecKey.getAddress();
    long amount = updateAccountPermissionFee + 1100000;

    Assert.assertTrue(PublicMethod
        .sendcoin(test001Address, amount, foundationAddress, testKey002,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethod.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    PublicMethodForMultiSign.printPermissionList(permissionsList);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission));
    dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;
  String accountPermissionJson1 =
        "{\"owner_permission\":{\"type\":0,\"permission_name\""
            + ":\"owner\",\"threshold\":1,\"keys\":[{\"address\""
            + ":\"" + PublicMethod.getAddressString(dev001Key) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":"
            + "\"active0\",\"threshold\":2,\"operations\""
            + ":\"0200000000000000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey2)
            + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(sendAccountKey3)
            + "\",\"weight\":1}]}]} ";

    Assert.assertTrue(PublicMethodForMultiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));

    Account test001AddressAccount1 = PublicMethod.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    PublicMethodForMultiSign.printPermissionList(permissionsList1);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission1));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission1));

    Transaction transaction = PublicMethodForMultiSign
        .sendcoinWithPermissionIdNotSign(test005Address, 1L, test001Address, 0, dev001Key,
            blockingStubFull);
    Transaction transaction1 = PublicMethod
        .addTransactionSign(transaction, sendAccountKey2, blockingStubFull);
    TransactionApprovedList transactionApprovedList = PublicMethod
        .getTransactionApprovedList(transaction1, blockingStubFull);

    logger.info("transactionSignWeight:" + transactionApprovedList);

    Assert.assertEquals(1, transactionApprovedList.getApprovedListCount());
    Assert.assertEquals(Base58.encode58Check(test002Address), Base58
        .encode58Check(transactionApprovedList.getApprovedList(0).toByteArray()));
    Assert.assertEquals(0,
        transactionApprovedList.getTransaction().getTransaction().getRawData().getContract(0)
            .getPermissionId());
  }

  /**
   * constructor.
   */
  public Protocol.Transaction createFakeTransaction(byte[] toAddrss, Long amount,
      byte[] foundationAddress) {

    BalanceContract.TransferContract contract = BalanceContract.TransferContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(foundationAddress))
        .setToAddress(ByteString.copyFrom(toAddrss))
        .setAmount(amount)
        .build();
    Protocol.Transaction transaction = createTransaction(contract, ContractType.TransferContract);

    return transaction;
  }

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
