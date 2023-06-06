package stest.tron.wallet.dailybuild.freezeV2;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.PublicMethedForMutiSign;
import stest.tron.wallet.common.client.utils.Utils;

@Slf4j
public class FreezeBalanceV2Test003 {
  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = now;
  private static String name = "MutiSign001_" + Long.toString(now);
  private final String foundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] foundationAddress = PublicMethed.getFinalAddress(foundationKey);
  private final String testKey001 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] foundationAddress01 = PublicMethed.getFinalAddress(testKey001);
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
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);



  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() throws Exception {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);


    if (!PublicMethed.freezeV2ProposalIsOpen(blockingStubFull)) {
      if (channelFull != null) {
        channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
      }
      throw new SkipException("Skipping freezeV2 test case");
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
    PublicMethed.printAddress(ownerKey);

    long needCoin = updateAccountPermissionFee * 1 + multiSignFee * 3;

    Assert.assertTrue(
        PublicMethed.sendcoin(ownerAddress,
            needCoin + 2048000000L, foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(
        PublicMethed.sendcoin(receiverAddress, 1L, foundationAddress, foundationKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    permissionKeyString[0] = manager1Key;
    permissionKeyString[1] = manager2Key;
    ownerKeyString[0] = ownerKey;
    ownerKeyString[1] = manager1Key;
    String[] permissionKeyString1 = new String[1];
    permissionKeyString1[0] = ownerKey;
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
            + "\"operations\":\"" + operations + "\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(manager2Key) + "\",\"weight\":1}"
            + "]}]}";

    logger.info(accountPermissionJson);
    String txid = PublicMethedForMutiSign
        .accountPermissionUpdateForTransactionId(accountPermissionJson, ownerAddress, ownerKey,
            blockingStubFull, ownerKeyString);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(PublicMethed.getTransactionById(txid, blockingStubFull).get()
        .getSignatureCount() == 1);

  }

  @Test(enabled = true, description = "MutiSign for freeze balance V2")
  public void test01MutiSignForFreezeBalanceV2() {
    Assert.assertTrue(
        PublicMethedForMutiSign.freezeBalanceV2WithPermissionId(ownerAddress, freezeBalance,
         0, 2, ownerKey, blockingStubFull, permissionKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Transaction transaction = PublicMethed.getTransactionById(PublicMethedForMutiSign
        .freezeV2Txid, blockingStubFull).get();
    logger.info("FreezeBalanceV2 txid:" + PublicMethedForMutiSign.freezeV2Txid);
    Assert.assertTrue(transaction.getSignatureCount() == 2);
    Assert.assertEquals(transaction.getRawData().getContract(0).getType(),
        ContractType.FreezeBalanceV2Contract);
    Assert.assertTrue(
        PublicMethed.getTransactionInfoById(PublicMethedForMutiSign.freezeV2Txid, blockingStubFull)
        .get().getFee() >= multiSignFee);
  }


  @Test(enabled = true, description = "MutiSign for delegate resource")
  public void test02MutiSignForDelegateResource() {
    Assert.assertTrue(PublicMethedForMutiSign.delegateResourceWithPermissionId(ownerAddress,
        delegateBalance, 0, receiverAddress, 2, ownerKey,
        blockingStubFull, permissionKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Transaction transaction = PublicMethed.getTransactionById(PublicMethedForMutiSign
        .freezeV2Txid, blockingStubFull).get();
    Assert.assertTrue(transaction.getSignatureCount() == 2);
    Assert.assertEquals(transaction.getRawData().getContract(0).getType(),
        ContractType.DelegateResourceContract);
    Assert.assertTrue(PublicMethed.getTransactionInfoById(PublicMethedForMutiSign.freezeV2Txid,
        blockingStubFull).get().getFee() >= multiSignFee);
  }


  @Test(enabled = true, description = "MutiSign for release delegate resource")
  public void test03MutiSignForUnDelegateResource() {
    Assert.assertTrue(PublicMethedForMutiSign.unDelegateResourceWithPermissionId(ownerAddress,
        delegateBalance, 0, receiverAddress, 2, ownerKey, blockingStubFull, permissionKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Transaction transaction = PublicMethed.getTransactionById(PublicMethedForMutiSign
        .freezeV2Txid, blockingStubFull).get();
    Assert.assertTrue(transaction.getSignatureCount() == 2);
    Assert.assertEquals(transaction.getRawData().getContract(0).getType(),
        ContractType.UnDelegateResourceContract);
    Assert.assertTrue(PublicMethed.getTransactionInfoById(PublicMethedForMutiSign.freezeV2Txid,
        blockingStubFull).get().getFee() >= multiSignFee);
  }


  @Test(enabled = true, description = "MutiSign for unFreezeBalanceV2")
  public void test04MutiSignForUnFreezeBalanceV2() {
    Assert.assertTrue(PublicMethedForMutiSign.unFreezeBalanceV2WithPermissionId(ownerAddress,
        delegateBalance,  0, 2, ownerKey, blockingStubFull, permissionKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Transaction transaction = PublicMethed.getTransactionById(PublicMethedForMutiSign
        .freezeV2Txid, blockingStubFull).get();
    Assert.assertTrue(transaction.getSignatureCount() == 2);
    Assert.assertEquals(transaction.getRawData().getContract(0).getType(),
        ContractType.UnfreezeBalanceV2Contract);
    Assert.assertTrue(PublicMethed.getTransactionInfoById(PublicMethedForMutiSign.freezeV2Txid,
        blockingStubFull).get().getFee() >= multiSignFee);
  }


  @Test(enabled = true, description = "MutiSign for withdrawExpireUnfreezeBalance")
  public void test05WithdrawExpireUnfreezeBalance() {
    Account account = PublicMethed.queryAccount(ownerAddress, blockingStubFull);
    if (System.currentTimeMillis() - 2000L < account.getUnfrozenV2(0).getUnfreezeExpireTime()) {
      Assert.assertFalse(PublicMethedForMutiSign
          .withdrawExpireUnfreezeBalanceWithPermissionId(ownerAddress, 2,
              ownerKey, blockingStubFull, permissionKeyString));
      logger.info("Check before expire time ,can't withdraw, function pass");
      int retryTimes = 0;
      Long unfreezeExpireTime = account.getUnfrozenV2(0).getUnfreezeExpireTime();
      while (retryTimes++ <= 10 && System.currentTimeMillis() < unfreezeExpireTime) {
        PublicMethed.waitProduceNextBlock(blockingStubFull);
      }
    }
    Assert.assertTrue(PublicMethedForMutiSign
        .withdrawExpireUnfreezeBalanceWithPermissionId(ownerAddress, 2,
            ownerKey, blockingStubFull, permissionKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Transaction transaction = PublicMethed.getTransactionById(PublicMethedForMutiSign
        .freezeV2Txid, blockingStubFull).get();
    Assert.assertTrue(transaction.getSignatureCount() == 2);
    Assert.assertEquals(transaction.getRawData().getContract(0).getType(),
        ContractType.WithdrawExpireUnfreezeContract);
    Assert.assertTrue(PublicMethed.getTransactionInfoById(PublicMethedForMutiSign.freezeV2Txid,
        blockingStubFull).get().getFee() >= multiSignFee);
  }

  @Test(enabled = true, description = "MutiSign for cancelUnfreeze")
  public void test06MultiSignForCancelUnfreeze() {
    logger.info(PublicMethed.queryAccount(ownerAddress,blockingStubFull).toString());
    Assert.assertTrue(PublicMethedForMutiSign.unFreezeBalanceV2WithPermissionId(ownerAddress,
        1,  0, 2, ownerKey, blockingStubFull, permissionKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account account0 = PublicMethed.queryAccount(ownerAddress,blockingStubFull);
    Assert.assertEquals(account0.getUnfrozenV2Count(), 1);
    logger.info(account0.toString());

    List<Integer> li = new ArrayList<>();
    li.add(0);
    Assert.assertTrue(PublicMethedForMutiSign
        .cancelUnfreezeWithPermissionId(ownerAddress, 2, li, blockingStubFull, permissionKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo info = PublicMethed.getTransactionInfoById(PublicMethedForMutiSign.cancelUnfreezeTxId,
        blockingStubFull).get();
    Assert.assertTrue(info.getFee() >= multiSignFee);
    Account account1 = PublicMethed.queryAccount(ownerAddress,blockingStubFull);
    logger.info(account1.toString());
    Assert.assertEquals(account1.getUnfrozenV2Count(), 0);

  }




  /**
   * constructor.
   */
  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


