package stest.tron.wallet.dailybuild.multisign;

import static org.hamcrest.core.StringContains.containsString;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
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
public class MultiSign34 extends TronBaseTest {
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

  @Test(enabled = true, description = "SR witness,sendcoin, use witnessPermission address sign.", groups = {"daily", "multisig"})
  public void testMultiUpdatepermissions_42() {
    Assert.assertTrue(PublicMethod
        .sendcoin(witnessAddress2, 1000000000, foundationAddress, testKey002,
            blockingStubFull));
    Account test001AddressAccount = PublicMethod.queryAccount(witnessAddress2, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
  final long balance = test001AddressAccount.getBalance();
    PublicMethodForMultiSign.printPermissionList(permissionsList);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission));
    Transaction transaction = PublicMethodForMultiSign
        .sendcoinWithPermissionIdNotSign(foundationAddress, 1L, witnessAddress2, 1, witnessKey2,
            blockingStubFull);
    Transaction transaction1 = PublicMethod
        .addTransactionSign(transaction, witnessKey2, blockingStubFull);
  final TransactionSignWeight transactionSignWeight = PublicMethodForMultiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);

    Account test001AddressAccount1 = PublicMethod.queryAccount(witnessAddress2, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
  final long balance1 = test001AddressAccount1.getBalance();
    PublicMethodForMultiSign.printPermissionList(permissionsList1);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission1));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission1));
    Assert.assertEquals(balance, balance1);
    logger.info("transaction:" + transactionSignWeight);
    Assert
        .assertThat(transactionSignWeight.getResult().getCode().toString(),
            containsString("PERMISSION_ERROR"));
    Assert
        .assertThat(transactionSignWeight.getResult().getMessage(),
            containsString("Permission for this, does not exist!"));

    Return returnResult1 = PublicMethodForMultiSign
        .broadcastTransaction1(transaction1, blockingStubFull);
    logger.info("returnResult1:" + returnResult1);
    Assert
        .assertThat(returnResult1.getCode().toString(), containsString("SIGERROR"));
    Assert
        .assertThat(returnResult1.getMessage().toStringUtf8(),
            containsString("permission isn't exit"));

  }

  @Test(enabled = true, description = "SR witness,sendcoin, use active address sign.", groups = {"daily", "multisig"})
  public void testMultiUpdatepermissions_43() {
    Assert.assertTrue(PublicMethod
        .sendcoin(witnessAddress2, 1000000000, foundationAddress, testKey002,
            blockingStubFull));
    Account test001AddressAccount = PublicMethod.queryAccount(witnessAddress2, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
  final long balance = test001AddressAccount.getBalance();
    PublicMethodForMultiSign.printPermissionList(permissionsList);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission));
    Transaction transaction = PublicMethodForMultiSign
        .sendcoinWithPermissionIdNotSign(foundationAddress, 1L, witnessAddress2, 2, witnessKey2,
            blockingStubFull);
  final Transaction transaction1 = PublicMethod
        .addTransactionSign(transaction, witnessKey2, blockingStubFull);
  final TransactionSignWeight transactionSignWeight = PublicMethodForMultiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transaction:" + transactionSignWeight);
    Assert
        .assertThat(transactionSignWeight.getResult().getCode().toString(),
            containsString("PERMISSION_ERROR"));
    Assert
        .assertThat(transactionSignWeight.getResult().getMessage(),
            containsString("Permission for this, does not exist!"));
    Return returnResult1 = PublicMethodForMultiSign
        .broadcastTransaction1(transaction1, blockingStubFull);
    logger.info("returnResult1:" + returnResult1);
    Account test001AddressAccount1 = PublicMethod.queryAccount(witnessAddress2, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
  final long balance1 = test001AddressAccount1.getBalance();
    PublicMethodForMultiSign.printPermissionList(permissionsList1);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission1));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission1));
    Assert.assertEquals(balance, balance1);
    Assert
        .assertThat(returnResult1.getCode().toString(), containsString("SIGERROR"));
    Assert
        .assertThat(returnResult1.getMessage().toStringUtf8(),
            containsString("permission isn't exit"));

  }

  @Test(enabled = true, description = "SR witness,sendcoin, use owner address sign.", groups = {"daily", "multisig"})
  public void testMultiUpdatepermissions_44() {
    Assert.assertTrue(PublicMethod
        .sendcoin(witnessAddress2, 1000000000, foundationAddress, testKey002,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Account test001AddressAccount = PublicMethod.queryAccount(witnessAddress2, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
  final long balance = test001AddressAccount.getBalance();
    PublicMethodForMultiSign.printPermissionList(permissionsList);
    logger.info("balance: " + balance);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission));
    Transaction transaction = PublicMethodForMultiSign
        .sendcoinWithPermissionIdNotSign(foundationAddress, 1L, witnessAddress2, 0, witnessKey2,
            blockingStubFull);
  final Transaction transaction1 = PublicMethod
        .addTransactionSign(transaction, witnessKey2, blockingStubFull);
    Return returnResult1 = PublicMethodForMultiSign
        .broadcastTransaction1(transaction1, blockingStubFull);
    logger.info("returnResult1:" + returnResult1);
    Assert.assertTrue(returnResult1.getResult());
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Account test001AddressAccount1 = PublicMethod.queryAccount(witnessAddress2, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
  final long balance1 = test001AddressAccount1.getBalance();
    logger.info("balance1: " + balance1);
    PublicMethodForMultiSign.printPermissionList(permissionsList1);
    logger.info(PublicMethodForMultiSign.printPermission(ownerPermission1));
    logger.info(PublicMethodForMultiSign.printPermission(witnessPermission1));
    Assert.assertEquals(balance - balance1, 1);

  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {  }

}
