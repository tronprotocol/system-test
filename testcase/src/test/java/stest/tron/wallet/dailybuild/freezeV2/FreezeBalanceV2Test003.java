package stest.tron.wallet.dailybuild.freezeV2;

import com.google.protobuf.ByteString;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.PublicMethodForMultiSign;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.Flaky;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
@Flaky(reason = "Timing-sensitive: depends on maintenance interval",
    since = "2026-04-03")
public class FreezeBalanceV2Test003 extends TronBaseTest {
  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = now;
  private static String name = "MultiSign001_" + Long.toString(now);
  private final String testKey001 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] foundationAddress01 = PublicMethod.getFinalAddress(testKey001);
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
  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] ownerAddress = ecKey3.getAddress();
  String ownerKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
  ECKey ecKey4 = new ECKey(Utils.getRandom());
  byte[] receiverAddress = ecKey4.getAddress();
  String receiverKey = ByteArray.toHexString(ecKey4.getPrivKeyBytes());
  private long multiSignFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.multiSignFee");
  private long updateAccountPermissionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.updateAccountPermissionFee");
  private long freezeBalance = 100000000L;
  private long delegateBalance = freezeBalance / 2;

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() throws Exception {    if (!PublicMethod.freezeV2ProposalIsOpen(blockingStubFull)) {      throw new SkipException("Skipping freezeV2 test case");
    }

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

    long needCoin = updateAccountPermissionFee * 1 + multiSignFee * 3;

    Assert.assertTrue(
        PublicMethod.sendcoin(ownerAddress,
            needCoin + 2048000000L, foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(
        PublicMethod.sendcoin(receiverAddress, 1L, foundationAddress, foundationKey,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    permissionKeyString[0] = manager1Key;
    permissionKeyString[1] = manager2Key;
    ownerKeyString[0] = ownerKey;
    ownerKeyString[1] = manager1Key;
    String[] permissionKeyString1 = new String[1];
    permissionKeyString1[0] = ownerKey;
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
            + "\"operations\":\"" + operations + "\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(manager2Key) + "\",\"weight\":1}"
            + "]}]}";

    logger.info(accountPermissionJson);
  String txid = PublicMethodForMultiSign
        .accountPermissionUpdateForTransactionId(accountPermissionJson, ownerAddress, ownerKey,
            blockingStubFull, ownerKeyString);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(PublicMethod.getTransactionById(txid, blockingStubFull).get()
        .getSignatureCount() == 1);

  }

  @Test(enabled = true, description = "MultiSign for freeze balance V2", groups = {"daily", "staking"})
  public void test01MultiSignForFreezeBalanceV2() {
    Assert.assertTrue(
        PublicMethodForMultiSign.freezeBalanceV2WithPermissionId(ownerAddress, freezeBalance,
         0, 2, ownerKey, blockingStubFull, permissionKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Transaction transaction = PublicMethod.getTransactionById(PublicMethodForMultiSign
        .freezeV2Txid, blockingStubFull).get();
    logger.info("FreezeBalanceV2 txid:" + PublicMethodForMultiSign.freezeV2Txid);
    Assert.assertTrue(transaction.getSignatureCount() == 2);
    Assert.assertEquals(transaction.getRawData().getContract(0).getType(),
        ContractType.FreezeBalanceV2Contract);
    Assert.assertTrue(
        PublicMethod.getTransactionInfoById(PublicMethodForMultiSign.freezeV2Txid, blockingStubFull)
        .get().getFee() >= multiSignFee);
  }


  @Test(enabled = true, description = "MultiSign for delegate resource", groups = {"daily", "staking"})
  public void test02MultiSignForDelegateResource() {
    Assert.assertTrue(PublicMethodForMultiSign.delegateResourceWithPermissionId(ownerAddress,
        delegateBalance, 0, receiverAddress, 2, ownerKey,
        blockingStubFull, permissionKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Transaction transaction = PublicMethod.getTransactionById(PublicMethodForMultiSign
        .freezeV2Txid, blockingStubFull).get();
    Assert.assertTrue(transaction.getSignatureCount() == 2);
    Assert.assertEquals(transaction.getRawData().getContract(0).getType(),
        ContractType.DelegateResourceContract);
    Assert.assertTrue(PublicMethod.getTransactionInfoById(PublicMethodForMultiSign.freezeV2Txid,
        blockingStubFull).get().getFee() >= multiSignFee);
  }


  @Test(enabled = true, description = "MultiSign for release delegate resource", groups = {"daily", "staking"})
  public void test03MultiSignForUnDelegateResource() {
    Assert.assertTrue(PublicMethodForMultiSign.unDelegateResourceWithPermissionId(ownerAddress,
        delegateBalance, 0, receiverAddress, 2, ownerKey, blockingStubFull, permissionKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Transaction transaction = PublicMethod.getTransactionById(PublicMethodForMultiSign
        .freezeV2Txid, blockingStubFull).get();
    Assert.assertTrue(transaction.getSignatureCount() == 2);
    Assert.assertEquals(transaction.getRawData().getContract(0).getType(),
        ContractType.UnDelegateResourceContract);
    Assert.assertTrue(PublicMethod.getTransactionInfoById(PublicMethodForMultiSign.freezeV2Txid,
        blockingStubFull).get().getFee() >= multiSignFee);
  }


  @Test(enabled = true, description = "MultiSign for unFreezeBalanceV2", groups = {"daily", "staking"})
  public void test04MultiSignForUnFreezeBalanceV2() {
    Assert.assertTrue(PublicMethodForMultiSign.unFreezeBalanceV2WithPermissionId(ownerAddress,
        delegateBalance,  0, 2, ownerKey, blockingStubFull, permissionKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Transaction transaction = PublicMethod.getTransactionById(PublicMethodForMultiSign
        .freezeV2Txid, blockingStubFull).get();
    Assert.assertTrue(transaction.getSignatureCount() == 2);
    Assert.assertEquals(transaction.getRawData().getContract(0).getType(),
        ContractType.UnfreezeBalanceV2Contract);
    Assert.assertTrue(PublicMethod.getTransactionInfoById(PublicMethodForMultiSign.freezeV2Txid,
        blockingStubFull).get().getFee() >= multiSignFee);
  }


  @Test(enabled = true, description = "MultiSign for withdrawExpireUnfreezeBalance", groups = {"daily", "staking"})
  public void test05WithdrawExpireUnfreezeBalance() {
    Account account = PublicMethod.queryAccount(ownerAddress, blockingStubFull);
    if (System.currentTimeMillis() - 2000L < account.getUnfrozenV2(0).getUnfreezeExpireTime()) {
      Assert.assertFalse(PublicMethodForMultiSign
          .withdrawExpireUnfreezeBalanceWithPermissionId(ownerAddress, 2,
              ownerKey, blockingStubFull, permissionKeyString));
      logger.info("Check before expire time ,can't withdraw, function pass");
  int retryTimes = 0;
  Long unfreezeExpireTime = account.getUnfrozenV2(0).getUnfreezeExpireTime();
      while (retryTimes++ <= 10 && System.currentTimeMillis() < unfreezeExpireTime) {
        PublicMethod.waitProduceNextBlock(blockingStubFull);
      }
    }
    Assert.assertTrue(PublicMethodForMultiSign
        .withdrawExpireUnfreezeBalanceWithPermissionId(ownerAddress, 2,
            ownerKey, blockingStubFull, permissionKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Transaction transaction = PublicMethod.getTransactionById(PublicMethodForMultiSign
        .freezeV2Txid, blockingStubFull).get();
    Assert.assertTrue(transaction.getSignatureCount() == 2);
    Assert.assertEquals(transaction.getRawData().getContract(0).getType(),
        ContractType.WithdrawExpireUnfreezeContract);
    Assert.assertTrue(PublicMethod.getTransactionInfoById(PublicMethodForMultiSign.freezeV2Txid,
        blockingStubFull).get().getFee() >= multiSignFee);
  }

  @Test(enabled = true, description = "MultiSign for cancelUnfreeze", groups = {"daily", "staking"})
  public void test06MultiSignForCancelUnfreeze() {
    logger.info(PublicMethod.queryAccount(ownerAddress,blockingStubFull).toString());
    Assert.assertTrue(PublicMethodForMultiSign.unFreezeBalanceV2WithPermissionId(ownerAddress,
        1,  0, 2, ownerKey, blockingStubFull, permissionKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Account account0 = PublicMethod.queryAccount(ownerAddress,blockingStubFull);
    Assert.assertEquals(account0.getUnfrozenV2Count(), 1);
    logger.info(account0.toString());

    List<Integer> li = new ArrayList<>();
    li.add(0);
    Assert.assertTrue(PublicMethodForMultiSign
        .cancelAllUnfreezeWithPermissionId(ownerAddress, 2, blockingStubFull, permissionKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    TransactionInfo info = PublicMethod.getTransactionInfoById(PublicMethodForMultiSign.cancelUnfreezeTxId,
        blockingStubFull).get();
    Assert.assertTrue(info.getFee() >= multiSignFee);
    Account account1 = PublicMethod.queryAccount(ownerAddress,blockingStubFull);
    logger.info(account1.toString());
    Assert.assertEquals(account1.getUnfrozenV2Count(), 0);

  }


  /**
   * constructor.
   */
  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {  }
}


