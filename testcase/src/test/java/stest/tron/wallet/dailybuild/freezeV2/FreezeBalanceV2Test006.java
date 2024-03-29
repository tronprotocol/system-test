package stest.tron.wallet.dailybuild.freezeV2;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.protobuf.Any;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.GrpcAPI.DelegatedResourceList;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.DelegatedResource;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.BalanceContract.FreezeBalanceV2Contract;
import org.tron.protos.contract.BalanceContract.UnfreezeBalanceV2Contract;
import org.tron.protos.contract.BalanceContract.WithdrawExpireUnfreezeContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.*;
import zmq.socket.pubsub.Pub;

@Slf4j
public class FreezeBalanceV2Test006 {
  private static final long sendAmount = 10000000000L;

  private final String foundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] foundationAddress = PublicMethed.getFinalAddress(foundationKey);

  private final String witnessKey = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] witnessAddress = PublicMethed.getFinalAddress(witnessKey);

  private final Long periodTime = 60_000L;
  private final Long delegateLockTime = 60_000L;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] frozenBandwidthAddress = ecKey1.getAddress();
  String frozenBandwidthKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] receiverAddress = ecKey2.getAddress();
  String receiverKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  Long freezeBandwidthBalance = 6000000L;
  Long delegateBalance = 1000000L;

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String soliditynode =
      Configuration.getByPath("testng.conf").getStringList("solidityNode.ip.list")
      .get(0);
  private ManagedChannel channelSolidity = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFullSolidity = null;
  private String pbftnode =
      Configuration.getByPath("testng.conf").getStringList("solidityNode.ip.list")
          .get(2);
  private ManagedChannel channelPbft = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubPbft = null;

  private Long maxDelegateLockPeriod = null;

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() throws Exception {
    PublicMethed.printAddress(frozenBandwidthKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext()
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    if (!PublicMethed.freezeV2ProposalIsOpen(blockingStubFull)) {
      if (channelFull != null) {
        channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
      }
      throw new SkipException("Skipping freezeV2 test case");
    }
    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext()
        .build();
    blockingStubFullSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
    channelPbft = ManagedChannelBuilder.forTarget(pbftnode)
        .usePlaintext()
        .build();
    blockingStubPbft = WalletSolidityGrpc.newBlockingStub(channelPbft);
    PublicMethed.printAddress(frozenBandwidthKey);
    PublicMethed.printAddress(receiverKey);
    Assert.assertTrue(PublicMethed.sendcoin(frozenBandwidthAddress, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(receiverAddress, 1L,
        foundationAddress, foundationKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.freezeBalanceV2(frozenBandwidthAddress, freezeBandwidthBalance,
        0, frozenBandwidthKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    maxDelegateLockPeriod
        = PublicMethed
        .getChainParametersValue(
            ProposalEnum.getMaxDelegateLockPeriod.getProposalName(), blockingStubFull
        );

  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Delegate lock value is true.")
  public void test01DelegateResourceLockValueIsTrueTest() throws Exception {
    Assert.assertTrue(PublicMethed.delegateResourceV2Lock(frozenBandwidthAddress,
        delegateBalance, 0, false, null,
        receiverAddress, frozenBandwidthKey, blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    final long beforeLockTrueTime = System.currentTimeMillis();

    Assert.assertTrue(PublicMethed.delegateResourceV2Lock(frozenBandwidthAddress,
        delegateBalance, 0, true, null,
        receiverAddress, frozenBandwidthKey, blockingStubFull));
    ;
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    DelegatedResourceList delegatedResourceList = PublicMethed
        .getDelegatedResource(frozenBandwidthAddress, receiverAddress, blockingStubFull).get();
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubFullSolidity);
    //query solidity
    DelegatedResourceList delegatedResourceListSolidity = PublicMethed
        .getDelegatedResourceV2Solidity(frozenBandwidthAddress,
            receiverAddress, blockingStubFullSolidity).get();
    Assert.assertEquals(delegatedResourceListSolidity, delegatedResourceList);
    //query pbft
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubPbft);
    DelegatedResourceList delegatedResourceListPbft = PublicMethed
        .getDelegatedResourceV2Solidity(frozenBandwidthAddress,
            receiverAddress, blockingStubPbft).get();
    Assert.assertEquals(delegatedResourceListPbft, delegatedResourceList);


    long unlockTimeStamp =
        delegatedResourceList.getDelegatedResource(1).getExpireTimeForBandwidth();
    Assert.assertTrue(
        delegatedResourceList.getDelegatedResource(0).getExpireTimeForBandwidth() == 0L);
    Assert.assertTrue(
        unlockTimeStamp > beforeLockTrueTime
            &&
            unlockTimeStamp <= System.currentTimeMillis() + delegateLockTime);


    Assert.assertFalse(PublicMethed.unDelegateResourceV2(frozenBandwidthAddress,
        delegateBalance + 1, 0,
        receiverAddress, frozenBandwidthKey, blockingStubFull));


    Assert.assertTrue(PublicMethed.unDelegateResourceV2(frozenBandwidthAddress,
        delegateBalance, 0,
        receiverAddress, frozenBandwidthKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    delegatedResourceList = PublicMethed
        .getDelegatedResource(frozenBandwidthAddress, receiverAddress, blockingStubFull).get();
    Assert.assertEquals(delegatedResourceList.getDelegatedResourceCount(), 1);
    Assert.assertTrue(
        delegatedResourceList.getDelegatedResource(0).getExpireTimeForBandwidth() > 0L);


    Assert.assertFalse(PublicMethed.unDelegateResourceV2(frozenBandwidthAddress,
        delegateBalance, 0,
        receiverAddress, frozenBandwidthKey, blockingStubFull));

    int retryTimes = 0;
    while (retryTimes++ <= 50 && System.currentTimeMillis() <= unlockTimeStamp + 3000L) {
      PublicMethed.waitProduceNextBlock(blockingStubFull);
    }

    Assert.assertTrue(PublicMethed.unDelegateResourceV2(frozenBandwidthAddress,
        delegateBalance, 0,
        receiverAddress, frozenBandwidthKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    delegatedResourceList = PublicMethed
        .getDelegatedResource(frozenBandwidthAddress, receiverAddress, blockingStubFull).get();
    Assert.assertEquals(delegatedResourceList.getDelegatedResourceCount(), 0);


  }

  @Test(enabled = true, description =
      "delegateResource bandwidth lock = true and lockPeriod is a non-zero value")
  public void test02DelegateResourceBandWidthLockPeriodValueIsNonZeroTest() {
    ECKey from = new ECKey(Utils.getRandom());
    byte[] fromAddress = from.getAddress();
    String fromKey = ByteArray.toHexString(from.getPrivKeyBytes());
    PublicMethed.sendcoin(fromAddress, sendAmount, foundationAddress, foundationKey,
        blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.freezeBalanceV2(fromAddress, freezeBandwidthBalance, 0, fromKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    final Long lockPeriod = 3L;
    Account beforeDelegateAccount =  PublicMethed.queryAccount(fromAddress, blockingStubFull);
    String txId = PublicMethed.delegateResourceV2LockAndGetTxId(
        fromAddress, delegateBalance, 0, true,
        lockPeriod, receiverAddress, fromKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo info = PublicMethed.getTransactionInfoById(txId, blockingStubFull).get();
    Long currentTime = info.getBlockTimeStamp() - 3000L;
    logger.info("nowTime: " + currentTime);
    Account afterDelegateAccount = PublicMethed.queryAccount(fromAddress, blockingStubFull);
    Assert.assertEquals(
        beforeDelegateAccount.getFrozenV2(0).getAmount()
            - afterDelegateAccount.getFrozenV2(0).getAmount(),
        delegateBalance.longValue());
    Optional<DelegatedResourceList> delegatedResourceList
        = PublicMethed.getDelegatedResourceV2(fromAddress, receiverAddress, blockingStubFull);
    Long expireTimeForBandwidth
        = delegatedResourceList.get().getDelegatedResource(0).getExpireTimeForBandwidth();
    logger.info("delegatedResourceList: " + delegatedResourceList.get());
    Assert.assertEquals(expireTimeForBandwidth - currentTime, lockPeriod * 3 * 1000L);
  }

  @Test(enabled = true, description = "unDelegateResource energy after lockPeriod")
  public void test03unDelegateEnergyAfterLockPeriod() {
    ECKey from = new ECKey(Utils.getRandom());
    byte[] fromAddress = from.getAddress();
    String fromKey = ByteArray.toHexString(from.getPrivKeyBytes());
    PublicMethed.sendcoin(fromAddress, sendAmount, foundationAddress, foundationKey,
        blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.freezeBalanceV2(fromAddress, freezeBandwidthBalance, 1, fromKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    final long lockPeriod = 3L;
    String txId = PublicMethed.delegateResourceV2LockAndGetTxId(fromAddress,
        delegateBalance, 1, true, lockPeriod, receiverAddress, fromKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo info = PublicMethed.getTransactionInfoById(txId, blockingStubFull).get();
    Long currentTime = info.getBlockTimeStamp() - 3000L;
    logger.info("nowTime: " + currentTime);
    Optional<DelegatedResourceList> delegatedResourceList =
        PublicMethed.getDelegatedResourceV2(fromAddress, receiverAddress, blockingStubFull);
    Long expireTimeForEnergy
        = delegatedResourceList.get().getDelegatedResource(0).getExpireTimeForEnergy();
    logger.info("delegatedResourceList: " + delegatedResourceList.get());
    Assert.assertEquals(
        expireTimeForEnergy - currentTime, lockPeriod * 3 * 1000L);

    // It can not unDelegated before expire time
    GrpcAPI.TransactionExtention ext
        = PublicMethed.unDelegateResourceV2AndGetTransactionExtention(
            fromAddress, delegateBalance, 1, receiverAddress, fromKey, blockingStubFull);
    logger.info("ext: " + ext);
    Assert.assertTrue(ext.toString().contains("CONTRACT_VALIDATE_ERROR"));
    Assert.assertTrue(ext.toString().contains(
            "Contract validate error : insufficient delegateFrozenBalance(Energy), request="
                + delegateBalance + ", unlock_balance=0"));
    int retryTimes = 3;
    while (retryTimes-- > 0) {
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      logger.info("wait one block...");
    }
    String txId2 = PublicMethed.unDelegateResourceV2AndGetTxId(
        fromAddress, delegateBalance, 1, receiverAddress, fromKey, blockingStubFull);
    logger.info("txId2:" + txId2);
    Assert.assertNotNull(txId2);
    Transaction transaction = PublicMethed.getTransactionById(txId2, blockingStubFull).get();
    Assert.assertEquals(
        transaction.getRet(0).getContractRet(), Transaction.Result.contractResult.SUCCESS);
  }

  @Test(enabled = true, description
      = "lock = false an lockPeriod = non-zero value, lockPeriod is invalid")
  public void test04InvalidLockPeriodValueWhenLockIsFalse() {
    ECKey from = new ECKey(Utils.getRandom());
    byte[] fromAddress = from.getAddress();
    String fromKey = ByteArray.toHexString(from.getPrivKeyBytes());
    PublicMethed.sendcoin(fromAddress, sendAmount, foundationAddress, foundationKey,
        blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.freezeBalanceV2(fromAddress, freezeBandwidthBalance, 1, fromKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.delegateResourceV2Lock(fromAddress, delegateBalance, 1, false, 50L,
        receiverAddress, fromKey, blockingStubFull);
    logger.info("nowTime: " + System.currentTimeMillis());
    Optional<DelegatedResourceList> delegatedResourceList
        = PublicMethed.getDelegatedResourceV2(fromAddress, receiverAddress, blockingStubFull);
    long expireTimeForEnergy
        = delegatedResourceList.get().getDelegatedResource(0).getExpireTimeForEnergy();
    Assert.assertEquals(expireTimeForEnergy, 0);
    String txId1 = PublicMethed.unDelegateResourceV2AndGetTxId(fromAddress, delegateBalance, 1,
        receiverAddress, fromKey, blockingStubFull);
    logger.info("unDelegateResourceV2AndGetTxId: " + txId1);
    Assert.assertNotNull(txId1);
    Transaction transaction = PublicMethed.getTransactionById(txId1, blockingStubFull).get();
    Assert.assertEquals(
        transaction.getRet(0).getContractRet(), Transaction.Result.contractResult.SUCCESS);
  }

  @Test(enabled = true, description = "only lock = true equals lock = true and lockPeriod = 0")
  public void test05LockPeriodIsZero() {
    ECKey from = new ECKey(Utils.getRandom());
    byte[] fromAddress = from.getAddress();
    String fromKey = ByteArray.toHexString(from.getPrivKeyBytes());
    PublicMethed.sendcoin(fromAddress, sendAmount, foundationAddress, foundationKey,
        blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.freezeBalanceV2(fromAddress, freezeBandwidthBalance, 1, fromKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String txId = PublicMethed.delegateResourceV2LockAndGetTxId(fromAddress,
        delegateBalance, 1, true, 0L, receiverAddress, fromKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo info = PublicMethed.getTransactionInfoById(txId, blockingStubFull).get();
    Long currentTime = info.getBlockTimeStamp() - 3000L;
    Optional<DelegatedResourceList> delegatedResourceList
        = PublicMethed.getDelegatedResourceV2(fromAddress, receiverAddress, blockingStubFull);
    Long unlockTimeStamp
        = delegatedResourceList.get().getDelegatedResource(0).getExpireTimeForEnergy();
    logger.info("beforeLockTrueTime: " + currentTime);
    logger.info("unlockTimeStamp: " + unlockTimeStamp);
    Assert.assertEquals(unlockTimeStamp - currentTime, delegateLockTime.longValue());
  }

  @Test(enabled = true, description = "Period boundary test")
  public void test06LockPeriodBoundaryTest() {
    Long maxPeriod = maxDelegateLockPeriod;
    ECKey from = new ECKey(Utils.getRandom());
    byte[] fromAddress = from.getAddress();
    String fromKey = ByteArray.toHexString(from.getPrivKeyBytes());
    PublicMethed.sendcoin(fromAddress, sendAmount, foundationAddress, foundationKey,
        blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.freezeBalanceV2(fromAddress, freezeBandwidthBalance, 1, fromKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    GrpcAPI.TransactionExtention ext = PublicMethed.delegateResourceV2AndGetTransactionExtention(
        fromAddress, delegateBalance, 1, true, maxPeriod + 1,
        receiverAddress, fromKey, blockingStubFull);
    logger.info("ext.toString(): " + ext);
    Assert.assertTrue(ext.toString().contains("CONTRACT_VALIDATE_ERROR"));
    Assert.assertTrue(ext.toString().contains(
            "The lock period of delegate resource cannot be less than 0 and cannot exceed "
                + maxPeriod + "!")
    );
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String txId2 = PublicMethed.delegateResourceV2LockAndGetTxId(
        fromAddress, delegateBalance, 1, true, maxPeriod,
        receiverAddress, fromKey, blockingStubFull);
    Assert.assertNotNull(txId2);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo info = PublicMethed.getTransactionInfoById(txId2, blockingStubFull).get();
    Long beforeLockTrueTime = info.getBlockTimeStamp() - 3000L;
    Optional<DelegatedResourceList> delegatedResourceList
        = PublicMethed.getDelegatedResourceV2(fromAddress, receiverAddress, blockingStubFull);
    long unlockTimeStamp
        = delegatedResourceList.get().getDelegatedResource(0).getExpireTimeForEnergy();
    logger.info("beforeLockTrueTime: " + beforeLockTrueTime);
    logger.info("unlockTimeStamp: " + unlockTimeStamp);
    Assert.assertEquals(unlockTimeStamp - beforeLockTrueTime, maxPeriod * 3 * 1000L);

  }

  @Test(enabled = true, description
      = "Period less than current expire time and bigger than expire time")
  public void test07LockPeriodLessAndBiggerThanCurrentExpireTime() {
    long lockPeriod = 5000L;
    ECKey from = new ECKey(Utils.getRandom());
    byte[] fromAddress = from.getAddress();
    String fromKey = ByteArray.toHexString(from.getPrivKeyBytes());
    PublicMethed.sendcoin(fromAddress, sendAmount, foundationAddress, foundationKey,
        blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.freezeBalanceV2(fromAddress, freezeBandwidthBalance, 1, fromKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long beforeDelegateTime = System.currentTimeMillis();
    PublicMethed.delegateResourceV2LockAndGetTxId(fromAddress, delegateBalance, 1, true, lockPeriod,
        receiverAddress, fromKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<DelegatedResourceList> beforeDelegatedResourceList
        = PublicMethed.getDelegatedResourceV2(fromAddress, receiverAddress, blockingStubFull);
    Long beforeUnlockTimeStamp
        = beforeDelegatedResourceList.get().getDelegatedResource(0).getExpireTimeForEnergy();
    Assert.assertTrue(
        Math.abs((beforeUnlockTimeStamp - beforeDelegateTime) - (lockPeriod * 3 * 1000)) < 5000L);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    GrpcAPI.TransactionExtention ext = PublicMethed.delegateResourceV2AndGetTransactionExtention(
        fromAddress, delegateBalance, 1, true, lockPeriod - 5,
        receiverAddress, fromKey, blockingStubFull);
    logger.info("ext.toString(): " + ext);
    Assert.assertTrue(ext.toString().contains("CONTRACT_VALIDATE_ERROR"));
    Assert.assertTrue(ext.toString().contains(
        "The lock period for ENERGY this time cannot be less than the remaining time")
    );
    String txId = PublicMethed.delegateResourceV2LockAndGetTxId(
        fromAddress, delegateBalance, 1, true, lockPeriod + 1000,
        receiverAddress, fromKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo info = PublicMethed.getTransactionInfoById(txId, blockingStubFull).get();
    Long afterDelegateTime = info.getBlockTimeStamp() - 3000L;

    Optional<DelegatedResourceList> afterDelegatedResourceList
        = PublicMethed.getDelegatedResourceV2(fromAddress, receiverAddress, blockingStubFull);
    Long afterUnlockTimeStamp
        = afterDelegatedResourceList.get().getDelegatedResource(0).getExpireTimeForEnergy();
    Assert.assertEquals(afterUnlockTimeStamp - afterDelegateTime, (lockPeriod + 1000) * 3 * 1000);
  }

  @Test(enabled = true, description = "Delegate twice in same block and lock = true,lockPeriod=0L")
  public void test08DelegateLockTwiceInSameBlock() {
    ECKey from = new ECKey(Utils.getRandom());
    byte[] fromAddress = from.getAddress();
    String fromKey = ByteArray.toHexString(from.getPrivKeyBytes());
    PublicMethed.printAddress(fromKey);
    PublicMethed.sendcoin(
        fromAddress, 1000000000L, foundationAddress, foundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.freezeBalanceV2(fromAddress, 200000000L, 0, fromKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    GrpcAPI.CanDelegatedMaxSizeResponseMessage message
        = PublicMethed.getCanDelegatedMaxSize(fromAddress, 0, blockingStubFull).get();
    logger.info("canDelegateResource:" + message);
    Long canDelegateResource = message.getMaxSize();
    String txId1 = PublicMethed.delegateResourceV2LockAndGetTxId(fromAddress,
        canDelegateResource / 3, 0, true, null, receiverAddress, fromKey, blockingStubFull);
    String txId2 = PublicMethed.delegateResourceV2LockAndGetTxId(fromAddress,
        canDelegateResource / 4, 0, true, null, receiverAddress, fromKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Transaction transaction1
        =  PublicMethed.getTransactionById(txId1, blockingStubFull).get();
    Transaction transaction2
        = PublicMethed.getTransactionById(txId2, blockingStubFull).get();
    Assert.assertNotNull(transaction1);
    Assert.assertNotNull(transaction2);
    Assert.assertEquals(
        transaction1.getRet(0).getContractRet().name(), "SUCCESS");
    Assert.assertEquals(
        transaction2.getRet(0).getContractRet().name(), "SUCCESS");
  }


  /**
   * constructor.
   */
  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    PublicMethed.freedResource(frozenBandwidthAddress,
        frozenBandwidthKey, foundationAddress, blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


