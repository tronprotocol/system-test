package stest.tron.wallet.dailybuild.multisign;

import static org.hamcrest.CoreMatchers.containsString;
import static org.tron.api.GrpcAPI.TransactionSignWeight.Result.response_code.ENOUGH_PERMISSION;
import static org.tron.api.GrpcAPI.TransactionSignWeight.Result.response_code.NOT_ENOUGH_PERMISSION;
import static org.tron.api.GrpcAPI.TransactionSignWeight.Result.response_code.PERMISSION_ERROR;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.GrpcAPI.TransactionSignWeight;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.AccountContract.AccountPermissionUpdateContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.PublicMethodForMultiSign;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.MultiNode;

@Slf4j
@MultiNode
public class MultiSign23 extends TronBaseTest {
  private static final String AVAILABLE_OPERATION =
      "7fff1fc0037e0000000000000000000000000000000000000000000000000000";
  private static final String DEFAULT_OPERATION =
      "7fff1fc0033e0000000000000000000000000000000000000000000000000000";
  private final byte[] witnessAddress001 = PublicMethod.getFinalAddress(witnessKey);
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
  private String fullnodeLocal = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  private String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {  }

  @Test(enabled = true, description = "Add sign for multi sign normal transaction", groups = {"daily", "multisig"})
  public void test01MultiSignNormalTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long needCoin = updateAccountPermissionFee * 2 + multiSignFee;

    Assert.assertTrue(PublicMethod.sendcoin(ownerAddress, needCoin + 1_000_000, foundationAddress,
        foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
//    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    PublicMethod.printAddress(ownerKey);
    PublicMethod.printAddress(tmpKey02);

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();
    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(witnessKey);
    activePermissionKeys.add(tmpKey02);

    logger.info("** update owner and active permission to two address");
  String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner1\","
            + "\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethodForMultiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethod.waitProduceNextBlock(blockingStubFull);
//    PublicMethod.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, PublicMethodForMultiSign.getActivePermissionKeyCount(
        PublicMethod.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1, PublicMethod.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethodForMultiSign.printPermissionList(PublicMethod.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out
        .printf(PublicMethodForMultiSign.printPermission(PublicMethod.queryAccount(ownerAddress,
            blockingStubFull).getOwnerPermission()));

    logger.info("** trigger a normal transaction");
    Transaction transaction = PublicMethodForMultiSign
        .sendcoin2(foundationAddress, 1000_000, ownerAddress, ownerKey, blockingStubFull);

    Transaction transaction1 = PublicMethodForMultiSign.addTransactionSignWithPermissionId(
        transaction, tmpKey02, 2, blockingStubFull);

    Transaction transaction2 = PublicMethodForMultiSign.addTransactionSignWithPermissionId(
        transaction1, witnessKey, 2, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction2.toByteArray()));

    TransactionSignWeight txWeight = PublicMethodForMultiSign
        .getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("TransactionSignWeight info : " + txWeight);

    Assert.assertTrue(PublicMethodForMultiSign.broadcastTransaction(transaction2, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    PublicMethodForMultiSign
        .recoverAccountPermission(ownerKey, ownerPermissionKeys, blockingStubFull);

    txWeight = PublicMethodForMultiSign.getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("TransactionSignWeight info : " + txWeight);

    Transaction transaction3 = PublicMethodForMultiSign
        .sendcoin2(foundationAddress, 1000_000, ownerAddress, ownerKey, blockingStubFull);

    Transaction transaction4 = PublicMethodForMultiSign.addTransactionSignWithPermissionId(
        transaction, tmpKey02, 2, blockingStubFull);
    Assert.assertFalse(PublicMethodForMultiSign.broadcastTransaction(transaction3,blockingStubFull));
    Assert.assertFalse(PublicMethodForMultiSign.broadcastTransaction(transaction4,blockingStubFull));
  Long balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin + 1000000);

  }


  @Test(enabled = true, description = "Add sign for multi sign permission transaction", groups = {"daily", "multisig"})
  public void test02MultiSignPermissionTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
  String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long needCoin = updateAccountPermissionFee * 2 + multiSignFee;

    PublicMethod.sendcoin(ownerAddress, needCoin, foundationAddress, foundationKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
//    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();

    PublicMethod.printAddress(ownerKey);

    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(ownerKey);

    Integer[] ints = {ContractType.AccountPermissionUpdateContract_VALUE};
  String operations = PublicMethodForMultiSign.getOperations(ints);

    logger.info("** update owner and active permission to two address");
  String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner1\","
            + "\"threshold\":5,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethod.getAddressString(foundationKey) + "\",\"weight\":3}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":3,"
            + "\"operations\":\"" + operations + "\",\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethodForMultiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.add(foundationKey);
    activePermissionKeys.add(tmpKey02);

    Assert.assertEquals(2,
        PublicMethodForMultiSign.getActivePermissionKeyCount(PublicMethod.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethod.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethodForMultiSign.printPermissionList(PublicMethod.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out
        .printf(PublicMethodForMultiSign.printPermission(PublicMethod.queryAccount(ownerAddress,
            blockingStubFull).getOwnerPermission()));

    logger.info("** trigger a permission transaction");
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey) + "\",\"weight\":1}"
            + "]}]}";

    Transaction transaction = PublicMethodForMultiSign.accountPermissionUpdateWithoutSign(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()]));

    Transaction transaction1 = PublicMethodForMultiSign
        .addTransactionSignWithPermissionId(transaction, tmpKey02, 2, blockingStubFull);

    Transaction transaction2 = PublicMethodForMultiSign
        .addTransactionSignWithPermissionId(transaction1, ownerKey, 2, blockingStubFull);

    TransactionSignWeight txWeight = PublicMethodForMultiSign
        .getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("TransactionSignWeight info : " + txWeight);

    Assert.assertTrue(PublicMethodForMultiSign.broadcastTransaction(transaction2, blockingStubFull));
  Long balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);
  }

  @Test(enabled = true, description = ""
      + "Add sign for single sign normal transaction,get approve list", groups = {"daily", "multisig"})
  public void test03SingleSignNormalTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long needCoin = updateAccountPermissionFee;

    Assert.assertTrue(PublicMethod.sendcoin(ownerAddress, needCoin + 1_000_000, foundationAddress,
        foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
//    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();

    PublicMethod.printAddress(ownerKey);

    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(ownerKey);

    Integer[] ints = {ContractType.TransferContract_VALUE};
  String operations = PublicMethodForMultiSign.getOperations(ints);
  String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner1\","
            + "\"threshold\":5,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethod.getAddressString(foundationKey) + "\",\"weight\":3}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"" + operations + "\",\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethodForMultiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Assert.assertEquals(2, PublicMethodForMultiSign.getActivePermissionKeyCount(
        PublicMethod.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethod.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethodForMultiSign.printPermissionList(PublicMethod.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    PublicMethodForMultiSign.printPermission(PublicMethod.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission());

    logger.info("** trigger a normal transaction");
    Transaction transaction = PublicMethodForMultiSign
        .sendcoin2(foundationAddress, 1000_000, ownerAddress, ownerKey, blockingStubFull);

    Transaction transaction1 = PublicMethodForMultiSign
        .addTransactionSignWithPermissionId(transaction, tmpKey02, 2, blockingStubFull);

    TransactionSignWeight txWeight = PublicMethodForMultiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("TransactionSignWeight info : " + txWeight);

    Assert.assertTrue(PublicMethodForMultiSign.broadcastTransaction(transaction1, blockingStubFull));
  Long balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin + 1000000);

    GrpcAPI.TransactionApprovedList transactionApprovedList = PublicMethod
        .getTransactionApprovedList(transaction1, blockingStubFull);

    logger.info("transactionApprovedList:" + transactionApprovedList);
    Assert.assertEquals(Base58.encode58Check(tmpAddr02), Base58
        .encode58Check(transactionApprovedList.getApprovedList(0).toByteArray()));
    Assert.assertEquals(2,
        transactionApprovedList.getTransaction().getTransaction().getRawData().getContract(0)
            .getPermissionId());
    Assert.assertEquals(1, transactionApprovedList.getApprovedListCount());

  }

  @Test(enabled = true, description = "Add sign for normal transaction "
      + "with address not in permission list", groups = {"daily", "multisig"})
  public void test06SignFailedTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    long needCoin = updateAccountPermissionFee;

    Assert.assertTrue(PublicMethod.sendcoin(ownerAddress, needCoin + 1000_000, foundationAddress,
        foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
//    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();

    PublicMethod.printAddress(ownerKey);

    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(ownerKey);
  String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner1\","
            + "\"threshold\":5,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethod.getAddressString(foundationKey) + "\",\"weight\":3}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"" + AVAILABLE_OPERATION + "\",\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethodForMultiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Assert.assertEquals(2,
        PublicMethodForMultiSign.getActivePermissionKeyCount(PublicMethod.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethod.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethodForMultiSign.printPermissionList(PublicMethod.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    PublicMethodForMultiSign.printPermission(PublicMethod.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission());

    ownerPermissionKeys.add(foundationKey);
    activePermissionKeys.add(tmpKey02);

    logger.info("** trigger a normal transaction");
    Transaction transaction = PublicMethodForMultiSign
        .sendcoin2(foundationAddress, 1000_000, ownerAddress, ownerKey, blockingStubFull);

    Transaction transaction1 = PublicMethodForMultiSign
        .addTransactionSignWithPermissionId(transaction, foundationKey, 2, blockingStubFull);

    TransactionSignWeight txWeight = PublicMethodForMultiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("TransactionSignWeight info : " + txWeight);

    Assert
        .assertFalse(PublicMethodForMultiSign.broadcastTransaction(transaction1, blockingStubFull));
  Long balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);

  }

  @Test(enabled = true, description = "Add sign for timeout normal transaction,get approve list", groups = {"daily", "multisig"})
  public void test07TimeoutTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long needCoin = updateAccountPermissionFee;

    Assert.assertTrue(PublicMethod.sendcoin(ownerAddress, needCoin + 1_000_000, foundationAddress,
        foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
//    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    PublicMethod.printAddress(ownerKey);

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();
    ownerPermissionKeys.add(ownerKey);
    ownerPermissionKeys.add(foundationKey);
    activePermissionKeys.add(ownerKey);
  String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner1\","
            + "\"threshold\":5,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethod.getAddressString(foundationKey) + "\",\"weight\":3}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"" + AVAILABLE_OPERATION + "\",\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethodForMultiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Assert.assertEquals(2, PublicMethodForMultiSign.getActivePermissionKeyCount(
        PublicMethod.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethod.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethodForMultiSign.printPermissionList(PublicMethod.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    PublicMethodForMultiSign.printPermission(PublicMethod.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission());

    ownerPermissionKeys.add(foundationKey);
    activePermissionKeys.add(tmpKey02);

    logger.info("** trigger a normal transaction");
    Transaction transaction = PublicMethodForMultiSign
        .sendcoin2(foundationAddress, 1000_000, ownerAddress, ownerKey, blockingStubFull);

    try {
      Thread.sleep(70000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    Transaction transaction1 = PublicMethodForMultiSign
        .addTransactionSignWithPermissionId(transaction, tmpKey02, 2, blockingStubFull);

    TransactionSignWeight txWeight = PublicMethodForMultiSign.getTransactionSignWeight(
        transaction1, blockingStubFull);
    logger.info("TransactionSignWeight info : " + txWeight);

    Assert
        .assertFalse(PublicMethodForMultiSign.broadcastTransaction(transaction1, blockingStubFull));
  Long balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);

    GrpcAPI.TransactionApprovedList transactionApprovedList = PublicMethod
        .getTransactionApprovedList(transaction1, blockingStubFull);

    logger.info("transactionApprovedList:" + transactionApprovedList);
    logger.info("Base58.encode58Check(test001Address)1:" + Base58
        .encode58Check(transactionApprovedList.getApprovedList(0).toByteArray()));
    Assert.assertEquals(1, transactionApprovedList.getApprovedListCount());
    Assert.assertEquals(Base58.encode58Check(tmpAddr02), Base58
        .encode58Check(transactionApprovedList.getApprovedList(0).toByteArray()));
    Assert.assertEquals(2,
        transactionApprovedList.getTransaction().getTransaction().getRawData().getContract(0)
            .getPermissionId());

  }

  @Test(enabled = true, description = "Add sign for empty transaction,get approve list", groups = {"daily", "multisig"})
  public void test08EmptyTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    Assert.assertTrue(PublicMethod.sendcoin(ownerAddress, 1_000_000, foundationAddress,
        foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
//    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    PublicMethod.printAddress(ownerKey);

    logger.info("** created an empty transaction");

    AccountPermissionUpdateContract.Builder builder =
        AccountPermissionUpdateContract.newBuilder();

    AccountPermissionUpdateContract contract = builder.build();
    TransactionExtention transactionExtention =
        blockingStubFull.accountPermissionUpdate(contract);
    Transaction transaction = transactionExtention.getTransaction();

    Transaction transaction1 = PublicMethod
        .addTransactionSign(transaction, ownerKey, blockingStubFull);

    TransactionSignWeight txWeight = PublicMethodForMultiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("TransactionSignWeight info : " + txWeight);

    Assert
        .assertFalse(PublicMethodForMultiSign.broadcastTransaction(transaction1, blockingStubFull));
  Long balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore, balanceAfter);
    logger.info("transaction hex string is " + ByteArray.toHexString(transaction.toByteArray()));
    GrpcAPI.TransactionApprovedList transactionApprovedList = PublicMethod
        .getTransactionApprovedList(transaction, blockingStubFull);
    logger.info("Before broadcast transactionApprovedList info :\n" + transactionApprovedList);
    Assert.assertEquals("Invalid transaction: no valid contract",
        transactionApprovedList.getResult().getMessage());
    Assert.assertFalse(PublicMethodForMultiSign
        .broadcastTransaction(transaction1, blockingStubFull));
    logger.info("transaction hex string is " + ByteArray.toHexString(transaction1.toByteArray()));
    transactionApprovedList = PublicMethod
        .getTransactionApprovedList(transaction1, blockingStubFull);
    Assert.assertEquals("Invalid transaction: no valid contract",
        transactionApprovedList.getResult().getMessage());
  }

  @Test(enabled = true, description = "Add sign for fake transaction,get approve list", groups = {"daily", "multisig"})
  public void test09ErrorTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long needCoin = updateAccountPermissionFee;

    Assert.assertTrue(PublicMethod.sendcoin(ownerAddress, needCoin + 1_000_000, foundationAddress,
        foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
//    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();

    PublicMethod.printAddress(ownerKey);

    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(ownerKey);
  String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner1\","
            + "\"threshold\":5,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethod.getAddressString(foundationKey) + "\",\"weight\":3}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"" + AVAILABLE_OPERATION + "\",\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":2}"
            + "]}]}";

    Assert.assertTrue(PublicMethodForMultiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Assert.assertEquals(2,
        PublicMethodForMultiSign.getActivePermissionKeyCount(PublicMethod.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethod.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethodForMultiSign.printPermissionList(PublicMethod.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    PublicMethodForMultiSign.printPermission(PublicMethod.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission());

    ownerPermissionKeys.add(foundationKey);
    activePermissionKeys.add(tmpKey02);

    logger.info("** trigger a fake transaction");
    Transaction transaction = PublicMethodForMultiSign
        .createFakeTransaction(ownerAddress, 1_000_000L, ownerAddress);
    Transaction transaction1 = PublicMethodForMultiSign
        .addTransactionSignWithPermissionId(transaction, tmpKey02, 2, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction1.toByteArray()));
    TransactionSignWeight txWeight = PublicMethodForMultiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("Before broadcast permission TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(2, txWeight.getCurrentWeight());

    Assert
        .assertFalse(PublicMethodForMultiSign.broadcastTransaction(transaction1, blockingStubFull));
  Long balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);
    GrpcAPI.TransactionApprovedList transactionApprovedList = PublicMethod
        .getTransactionApprovedList(transaction1, blockingStubFull);
    Assert.assertEquals(1, transactionApprovedList.getApprovedListCount());
    Assert.assertEquals(Base58.encode58Check(tmpAddr02), Base58
        .encode58Check(transactionApprovedList.getApprovedList(0).toByteArray()));
    Assert.assertEquals(2,
        transactionApprovedList.getTransaction().getTransaction().getRawData().getContract(0)
            .getPermissionId());
  }

  @Test(enabled = true, description = "Add sign transaction with mix order", groups = {"daily", "multisig"})
  public void test10MultiSignNormalTransactionWithMixOrder() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long needCoin = updateAccountPermissionFee + multiSignFee;

    Assert.assertTrue(PublicMethod.sendcoin(ownerAddress, needCoin + 1_000_000, foundationAddress,
        foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
//    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethod.printAddress(ownerKey);
    PublicMethod.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);
  String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner1\","
            + "\"threshold\":5,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethod.getAddressString(foundationKey) + "\",\"weight\":3}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":3,"
            + "\"operations\":\"" + DEFAULT_OPERATION + "\",\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethodForMultiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.add(foundationKey);

    Assert.assertEquals(3, PublicMethodForMultiSign.getActivePermissionKeyCount(
        PublicMethod.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethod.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethodForMultiSign.printPermissionList(PublicMethod.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    PublicMethodForMultiSign.printPermission(PublicMethod.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission());

    logger.info("** trigger a normal transaction");
    Transaction transaction = PublicMethodForMultiSign
        .sendcoin2(foundationAddress, 1000_000, ownerAddress, ownerKey, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction.toByteArray()));
    TransactionSignWeight txWeight = PublicMethodForMultiSign.getTransactionSignWeight(
        transaction, blockingStubFull);
    logger.info("Before Sign TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(NOT_ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());

    Transaction transaction1 = PublicMethodForMultiSign
        .addTransactionSignWithPermissionId(transaction, tmpKey02, 2, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(
        transaction1.toByteArray()));
    txWeight = PublicMethodForMultiSign.getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("Before broadcast1 TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(NOT_ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(1, txWeight.getCurrentWeight());

    Transaction transaction2 = PublicMethodForMultiSign
        .addTransactionSignWithPermissionId(transaction1, ownerKey, 2, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction2.toByteArray()));
    txWeight = PublicMethodForMultiSign.getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("Before broadcast2 TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(NOT_ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(2, txWeight.getCurrentWeight());

    Transaction transaction3 = PublicMethodForMultiSign
        .addTransactionSignWithPermissionId(transaction2, witnessKey, 2, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction3.toByteArray()));
    txWeight = PublicMethodForMultiSign.getTransactionSignWeight(transaction3, blockingStubFull);
    logger.info("Before broadcast2 TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(3, txWeight.getCurrentWeight());

    Assert.assertTrue(PublicMethodForMultiSign.broadcastTransaction(transaction3, blockingStubFull));
  Long balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin + 1000000);
  }

  @Test(enabled = true, description = "Add sign transaction with same address", groups = {"daily", "multisig"})
  public void test11MultiSignNormalTransactionBySameAccount() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long needCoin = updateAccountPermissionFee;

    Assert.assertTrue(PublicMethod.sendcoin(ownerAddress, needCoin + 1_000_000, foundationAddress,
        foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
//    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethod.printAddress(ownerKey);
    PublicMethod.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    logger.info("** update owner and active permission to two address");
  String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner1\","
            + "\"threshold\":5,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethod.getAddressString(foundationKey) + "\",\"weight\":3}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":3,"
            + "\"operations\":\"" + DEFAULT_OPERATION + "\",\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethodForMultiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.add(foundationKey);

    Assert.assertEquals(3,
        PublicMethodForMultiSign.getActivePermissionKeyCount(PublicMethod.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethod.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethodForMultiSign.printPermissionList(PublicMethod.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    PublicMethodForMultiSign.printPermission(PublicMethod.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission());

    logger.info("** trigger a normal transaction");
    Transaction transaction = PublicMethodForMultiSign
        .sendcoin2(foundationAddress, 1000_000, ownerAddress, ownerKey, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction.toByteArray()));
    TransactionSignWeight txWeight =
        PublicMethodForMultiSign.getTransactionSignWeight(transaction, blockingStubFull);
    logger.info("Before Sign TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(NOT_ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());

    Transaction transaction1 = PublicMethodForMultiSign
        .addTransactionSignWithPermissionId(transaction, tmpKey02, 2, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction1.toByteArray()));
    txWeight = PublicMethodForMultiSign.getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("Before broadcast1 TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(NOT_ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(1, txWeight.getCurrentWeight());

    Transaction transaction2 = PublicMethodForMultiSign
        .addTransactionSignWithPermissionId(transaction1, ownerKey, 2, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction2.toByteArray()));
    txWeight = PublicMethodForMultiSign.getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("Before broadcast2 TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(NOT_ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(2, txWeight.getCurrentWeight());

    Transaction transaction3 = PublicMethodForMultiSign
        .addTransactionSignWithPermissionId(transaction2, ownerKey, 2, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction3.toByteArray()));
    txWeight = PublicMethodForMultiSign.getTransactionSignWeight(transaction3, blockingStubFull);
    logger.info("Before broadcast2 TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(PERMISSION_ERROR, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());
    Assert.assertThat(txWeight.getResult().getMessage(),
        containsString("has signed twice!"));

    Assert.assertFalse(PublicMethodForMultiSign.broadcastTransaction(
        transaction3, blockingStubFull));
  Long balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);
  }

  @Test(enabled = true, description = "Add sign transaction with null address", groups = {"daily", "multisig"})
  public void test12MultiSignNormalTransactionByNullKey() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey1.getAddress();
    ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    long needCoin = updateAccountPermissionFee;

    Assert.assertTrue(PublicMethod.sendcoin(ownerAddress, needCoin + 1_000_000, foundationAddress,
        foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
//    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethod.printAddress(ownerKey);
    PublicMethod.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);
  String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner1\","
            + "\"threshold\":5,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethod.getAddressString(foundationKey) + "\",\"weight\":3}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":3,"
            + "\"operations\":\"" + DEFAULT_OPERATION + "\",\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethodForMultiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.add(foundationKey);

    Assert.assertEquals(3,
        PublicMethodForMultiSign.getActivePermissionKeyCount(PublicMethod.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethod.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethodForMultiSign.printPermissionList(PublicMethod.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    PublicMethodForMultiSign.printPermission(PublicMethod.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission());

    logger.info("** trigger a normal transaction");
    Transaction transaction = PublicMethodForMultiSign
        .sendcoin2(foundationAddress, 1000_000, ownerAddress, ownerKey, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction.toByteArray()));
    TransactionSignWeight txWeight =
        PublicMethodForMultiSign.getTransactionSignWeight(transaction, blockingStubFull);
    logger.info("Before Sign TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(NOT_ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());

    Transaction transaction1 = null;
    boolean ret = false;
    try {
      transaction1 = PublicMethodForMultiSign
          .addTransactionSignWithPermissionId(transaction, null, 2, blockingStubFull);
    } catch (NullPointerException e) {
      logger.info("java.lang.NullPointerException");
      ret = true;
    }
    Assert.assertTrue(ret);

    ret = false;
    try {
      transaction1 = PublicMethodForMultiSign
          .addTransactionSignWithPermissionId(transaction, "", 2, blockingStubFull);
    } catch (NumberFormatException e) {
      logger.info("NumberFormatException: Zero length BigInteger");
      ret = true;
    } catch (NullPointerException e) {
      logger.info("NullPointerException");
      ret = true;
    }
    Assert.assertTrue(ret);

    ret = false;
    try {
      transaction1 = PublicMethodForMultiSign
          .addTransactionSignWithPermissionId(transaction, "abcd1234", 2, blockingStubFull);
    } catch (Exception e) {
      logger.info("Exception!!");
      ret = true;
    }
    Assert.assertFalse(ret);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction1.toByteArray()));
    txWeight = PublicMethodForMultiSign.getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("Before broadcast TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(PERMISSION_ERROR, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());
    Assert.assertThat(txWeight.getResult().getMessage(),
        containsString("but it is not contained of permission"));
  Long balanceAfter = PublicMethod.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);

  }

  @AfterMethod
  public void aftertest() {
    PublicMethod.freeResource(ownerAddress, ownerKey, foundationAddress, blockingStubFull);
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {  }

}
