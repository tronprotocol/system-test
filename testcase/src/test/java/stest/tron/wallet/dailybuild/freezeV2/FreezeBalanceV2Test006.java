package stest.tron.wallet.dailybuild.freezeV2;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.protobuf.Any;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.GrpcAPI.DelegatedResourceList;
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
import stest.tron.wallet.common.client.utils.Flaky;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
@Flaky(reason = "Timing-sensitive: delegate resource expiry timing",
    since = "2026-04-03")
public class FreezeBalanceV2Test006 extends TronBaseTest {
  private static final long sendAmount = 10000000000L;
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
  private String soliditynode =
      Configuration.getByPath("testng.conf").getStringList("solidityNode.ip.list")
      .get(0);
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFullSolidity = null;
  private String pbftnode =
      Configuration.getByPath("testng.conf").getStringList("solidityNode.ip.list")
          .get(2);
  private Long maxDelegateLockPeriod = null;

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() throws Exception {
    initPbftChannel();
    initSolidityChannel();
    PublicMethod.printAddress(frozenBandwidthKey);    if (!PublicMethod.freezeV2ProposalIsOpen(blockingStubFull)) {      throw new SkipException("Skipping freezeV2 test case");
    }
    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext()
        .build();
    blockingStubFullSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
    channelPbft = ManagedChannelBuilder.forTarget(pbftnode)
        .usePlaintext()
        .build();
    PublicMethod.printAddress(frozenBandwidthKey);
    PublicMethod.printAddress(receiverKey);
    Assert.assertTrue(PublicMethod.sendcoin(frozenBandwidthAddress, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.sendcoin(receiverAddress, 1L,
        foundationAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethod.freezeBalanceV2(frozenBandwidthAddress, freezeBandwidthBalance,
        0, frozenBandwidthKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    maxDelegateLockPeriod
        = PublicMethod
        .getChainParametersValue(
            ProposalEnum.getMaxDelegateLockPeriod.getProposalName(), blockingStubFull
        );

  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Delegate lock value is true.", groups = {"daily", "staking"})
  public void test01DelegateResourceLockValueIsTrueTest() throws Exception {
    Assert.assertTrue(PublicMethod.delegateResourceV2Lock(frozenBandwidthAddress,
        delegateBalance, 0, false, null,
        receiverAddress, frozenBandwidthKey, blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  final long beforeLockTrueTime = System.currentTimeMillis();

    Assert.assertTrue(PublicMethod.delegateResourceV2Lock(frozenBandwidthAddress,
        delegateBalance, 0, true, null,
        receiverAddress, frozenBandwidthKey, blockingStubFull));
    ;
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    DelegatedResourceList delegatedResourceList = PublicMethod
        .getDelegatedResource(frozenBandwidthAddress, receiverAddress, blockingStubFull).get();
    PublicMethod.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubFullSolidity);
  //query solidity
    DelegatedResourceList delegatedResourceListSolidity = PublicMethod
        .getDelegatedResourceV2Solidity(frozenBandwidthAddress,
            receiverAddress, blockingStubFullSolidity).get();
    Assert.assertEquals(delegatedResourceListSolidity, delegatedResourceList);
  //query pbft
    PublicMethod.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubPbft);
    DelegatedResourceList delegatedResourceListPbft = PublicMethod
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


    Assert.assertFalse(PublicMethod.unDelegateResourceV2(frozenBandwidthAddress,
        delegateBalance + 1, 0,
        receiverAddress, frozenBandwidthKey, blockingStubFull));


    Assert.assertTrue(PublicMethod.unDelegateResourceV2(frozenBandwidthAddress,
        delegateBalance, 0,
        receiverAddress, frozenBandwidthKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    delegatedResourceList = PublicMethod
        .getDelegatedResource(frozenBandwidthAddress, receiverAddress, blockingStubFull).get();
    Assert.assertEquals(delegatedResourceList.getDelegatedResourceCount(), 1);
    Assert.assertTrue(
        delegatedResourceList.getDelegatedResource(0).getExpireTimeForBandwidth() > 0L);


    Assert.assertFalse(PublicMethod.unDelegateResourceV2(frozenBandwidthAddress,
        delegateBalance, 0,
        receiverAddress, frozenBandwidthKey, blockingStubFull));
  int retryTimes = 0;
    while (retryTimes++ <= 50 && System.currentTimeMillis() <= unlockTimeStamp + 3000L) {
      PublicMethod.waitProduceNextBlock(blockingStubFull);
    }

    Assert.assertTrue(PublicMethod.unDelegateResourceV2(frozenBandwidthAddress,
        delegateBalance, 0,
        receiverAddress, frozenBandwidthKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    delegatedResourceList = PublicMethod
        .getDelegatedResource(frozenBandwidthAddress, receiverAddress, blockingStubFull).get();
    Assert.assertEquals(delegatedResourceList.getDelegatedResourceCount(), 0);


  }

  @Test(enabled = true, description =
      "delegateResource bandwidth lock = true and lockPeriod is a non-zero value", groups = {"daily", "staking"})
  public void test02DelegateResourceBandWidthLockPeriodValueIsNonZeroTest() {
    ECKey from = new ECKey(Utils.getRandom());
  byte[] fromAddress = from.getAddress();
  String fromKey = ByteArray.toHexString(from.getPrivKeyBytes());
    PublicMethod.sendcoin(fromAddress, sendAmount, foundationAddress, foundationKey,
        blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.freezeBalanceV2(fromAddress, freezeBandwidthBalance, 0, fromKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  final Long lockPeriod = 3L;
    Account beforeDelegateAccount =  PublicMethod.queryAccount(fromAddress, blockingStubFull);
  String txId = PublicMethod.delegateResourceV2LockAndGetTxId(
        fromAddress, delegateBalance, 0, true,
        lockPeriod, receiverAddress, fromKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    TransactionInfo info = PublicMethod.getTransactionInfoById(txId, blockingStubFull).get();
  Long currentTime = info.getBlockTimeStamp() - 3000L;
    logger.info("nowTime: " + currentTime);
    Account afterDelegateAccount = PublicMethod.queryAccount(fromAddress, blockingStubFull);
    Assert.assertEquals(
        beforeDelegateAccount.getFrozenV2(0).getAmount()
            - afterDelegateAccount.getFrozenV2(0).getAmount(),
        delegateBalance.longValue());
    Optional<DelegatedResourceList> delegatedResourceList
        = PublicMethod.getDelegatedResourceV2(fromAddress, receiverAddress, blockingStubFull);
  Long expireTimeForBandwidth
        = delegatedResourceList.get().getDelegatedResource(0).getExpireTimeForBandwidth();
    logger.info("delegatedResourceList: " + delegatedResourceList.get());
    Assert.assertEquals(expireTimeForBandwidth - currentTime, lockPeriod * 3 * 1000L);
  }

  @Test(enabled = true, description = "unDelegateResource energy after lockPeriod", groups = {"daily", "staking"})
  public void test03unDelegateEnergyAfterLockPeriod() {
    ECKey from = new ECKey(Utils.getRandom());
  byte[] fromAddress = from.getAddress();
  String fromKey = ByteArray.toHexString(from.getPrivKeyBytes());
    PublicMethod.sendcoin(fromAddress, sendAmount, foundationAddress, foundationKey,
        blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.freezeBalanceV2(fromAddress, freezeBandwidthBalance, 1, fromKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  final long lockPeriod = 3L;
  String txId = PublicMethod.delegateResourceV2LockAndGetTxId(fromAddress,
        delegateBalance, 1, true, lockPeriod, receiverAddress, fromKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    TransactionInfo info = PublicMethod.getTransactionInfoById(txId, blockingStubFull).get();
  Long currentTime = info.getBlockTimeStamp() - 3000L;
    logger.info("nowTime: " + currentTime);
    Optional<DelegatedResourceList> delegatedResourceList =
        PublicMethod.getDelegatedResourceV2(fromAddress, receiverAddress, blockingStubFull);
  Long expireTimeForEnergy
        = delegatedResourceList.get().getDelegatedResource(0).getExpireTimeForEnergy();
    logger.info("delegatedResourceList: " + delegatedResourceList.get());
    Assert.assertEquals(
        expireTimeForEnergy - currentTime, lockPeriod * 3 * 1000L);
  // It can not unDelegated before expire time
    GrpcAPI.TransactionExtention ext
        = PublicMethod.unDelegateResourceV2AndGetTransactionExtention(
            fromAddress, delegateBalance, 1, receiverAddress, fromKey, blockingStubFull);
    logger.info("ext: " + ext);
    Assert.assertTrue(ext.toString().contains("CONTRACT_VALIDATE_ERROR"));
    Assert.assertTrue(ext.toString().contains(
            "Contract validate error : insufficient delegateFrozenBalance(Energy), request="
                + delegateBalance + ", unlock_balance=0"));
  int retryTimes = 3;
    while (retryTimes-- > 0) {
      PublicMethod.waitProduceNextBlock(blockingStubFull);
      logger.info("wait one block...");
    }
    String txId2 = PublicMethod.unDelegateResourceV2AndGetTxId(
        fromAddress, delegateBalance, 1, receiverAddress, fromKey, blockingStubFull);
    logger.info("txId2:" + txId2);
    Assert.assertNotNull(txId2);
    Transaction transaction = PublicMethod.getTransactionById(txId2, blockingStubFull).get();
    Assert.assertEquals(
        transaction.getRet(0).getContractRet(), Transaction.Result.contractResult.SUCCESS);
  }

  @Test(enabled = true, description
      = "lock = false an lockPeriod = non-zero value, lockPeriod is invalid", groups = {"daily", "staking"})
  public void test04InvalidLockPeriodValueWhenLockIsFalse() {
    ECKey from = new ECKey(Utils.getRandom());
  byte[] fromAddress = from.getAddress();
  String fromKey = ByteArray.toHexString(from.getPrivKeyBytes());
    PublicMethod.sendcoin(fromAddress, sendAmount, foundationAddress, foundationKey,
        blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.freezeBalanceV2(fromAddress, freezeBandwidthBalance, 1, fromKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.delegateResourceV2Lock(fromAddress, delegateBalance, 1, false, 50L,
        receiverAddress, fromKey, blockingStubFull);
    logger.info("nowTime: " + System.currentTimeMillis());
    Optional<DelegatedResourceList> delegatedResourceList
        = PublicMethod.getDelegatedResourceV2(fromAddress, receiverAddress, blockingStubFull);
    long expireTimeForEnergy
        = delegatedResourceList.get().getDelegatedResource(0).getExpireTimeForEnergy();
    Assert.assertEquals(expireTimeForEnergy, 0);
  String txId1 = PublicMethod.unDelegateResourceV2AndGetTxId(fromAddress, delegateBalance, 1,
        receiverAddress, fromKey, blockingStubFull);
    logger.info("unDelegateResourceV2AndGetTxId: " + txId1);
    Assert.assertNotNull(txId1);
    Transaction transaction = PublicMethod.getTransactionById(txId1, blockingStubFull).get();
    Assert.assertEquals(
        transaction.getRet(0).getContractRet(), Transaction.Result.contractResult.SUCCESS);
  }

  @Test(enabled = true, description = "only lock = true equals lock = true and lockPeriod = 0", groups = {"daily", "staking"})
  public void test05LockPeriodIsZero() {
    ECKey from = new ECKey(Utils.getRandom());
  byte[] fromAddress = from.getAddress();
  String fromKey = ByteArray.toHexString(from.getPrivKeyBytes());
    PublicMethod.sendcoin(fromAddress, sendAmount, foundationAddress, foundationKey,
        blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.freezeBalanceV2(fromAddress, freezeBandwidthBalance, 1, fromKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String txId = PublicMethod.delegateResourceV2LockAndGetTxId(fromAddress,
        delegateBalance, 1, true, 0L, receiverAddress, fromKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    TransactionInfo info = PublicMethod.getTransactionInfoById(txId, blockingStubFull).get();
  Long currentTime = info.getBlockTimeStamp() - 3000L;
    Optional<DelegatedResourceList> delegatedResourceList
        = PublicMethod.getDelegatedResourceV2(fromAddress, receiverAddress, blockingStubFull);
  Long unlockTimeStamp
        = delegatedResourceList.get().getDelegatedResource(0).getExpireTimeForEnergy();
    logger.info("beforeLockTrueTime: " + currentTime);
    logger.info("unlockTimeStamp: " + unlockTimeStamp);
    Assert.assertEquals(unlockTimeStamp - currentTime, delegateLockTime.longValue());
  }

  @Test(enabled = true, description = "Period boundary test", groups = {"daily", "staking"})
  public void test06LockPeriodBoundaryTest() {
    Long maxPeriod = maxDelegateLockPeriod;
  ECKey from = new ECKey(Utils.getRandom());
  byte[] fromAddress = from.getAddress();
  String fromKey = ByteArray.toHexString(from.getPrivKeyBytes());
    PublicMethod.sendcoin(fromAddress, sendAmount, foundationAddress, foundationKey,
        blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.freezeBalanceV2(fromAddress, freezeBandwidthBalance, 1, fromKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    GrpcAPI.TransactionExtention ext = PublicMethod.delegateResourceV2AndGetTransactionExtention(
        fromAddress, delegateBalance, 1, true, maxPeriod + 1,
        receiverAddress, fromKey, blockingStubFull);
    logger.info("ext.toString(): " + ext);
    Assert.assertTrue(ext.toString().contains("CONTRACT_VALIDATE_ERROR"));
    Assert.assertTrue(ext.toString().contains(
            "The lock period of delegate resource cannot be less than 0 and cannot exceed "
                + maxPeriod + "!")
    );
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String txId2 = PublicMethod.delegateResourceV2LockAndGetTxId(
        fromAddress, delegateBalance, 1, true, maxPeriod,
        receiverAddress, fromKey, blockingStubFull);
    Assert.assertNotNull(txId2);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    TransactionInfo info = PublicMethod.getTransactionInfoById(txId2, blockingStubFull).get();
    long lastBlockNumber = info.getBlockNumber() - 1;
    Long lockStartTime = PublicMethod.getBlock2(lastBlockNumber, blockingStubFull).getBlockHeader().getRawData().getTimestamp();
    Optional<DelegatedResourceList> delegatedResourceList
        = PublicMethod.getDelegatedResourceV2(fromAddress, receiverAddress, blockingStubFull);
    long unlockTimeStamp
        = delegatedResourceList.get().getDelegatedResource(0).getExpireTimeForEnergy();
    logger.info("beforeLockTrueTime: " + lockStartTime);
    logger.info("unlockTimeStamp: " + unlockTimeStamp);
    Assert.assertEquals(unlockTimeStamp - lockStartTime, maxPeriod * 3 * 1000L);

  }

  @Test(enabled = true, description
      = "Period less than current expire time and bigger than expire time", groups = {"daily", "staking"})
  public void test07LockPeriodLessAndBiggerThanCurrentExpireTime() {
    long lockPeriod = 5000L;
  ECKey from = new ECKey(Utils.getRandom());
  byte[] fromAddress = from.getAddress();
  String fromKey = ByteArray.toHexString(from.getPrivKeyBytes());
    PublicMethod.sendcoin(fromAddress, sendAmount, foundationAddress, foundationKey,
        blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.freezeBalanceV2(fromAddress, freezeBandwidthBalance, 1, fromKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long beforeDelegateTime = System.currentTimeMillis();
    PublicMethod.delegateResourceV2LockAndGetTxId(fromAddress, delegateBalance, 1, true, lockPeriod,
        receiverAddress, fromKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<DelegatedResourceList> beforeDelegatedResourceList
        = PublicMethod.getDelegatedResourceV2(fromAddress, receiverAddress, blockingStubFull);
  Long beforeUnlockTimeStamp
        = beforeDelegatedResourceList.get().getDelegatedResource(0).getExpireTimeForEnergy();
    Assert.assertTrue(
        Math.abs((beforeUnlockTimeStamp - beforeDelegateTime) - (lockPeriod * 3 * 1000)) < 5000L);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    GrpcAPI.TransactionExtention ext = PublicMethod.delegateResourceV2AndGetTransactionExtention(
        fromAddress, delegateBalance, 1, true, lockPeriod - 1000L,
        receiverAddress, fromKey, blockingStubFull);
    logger.info("ext.toString(): " + ext);
    Assert.assertTrue(ext.toString().contains("CONTRACT_VALIDATE_ERROR"));
    Assert.assertTrue(ext.toString().contains(
        "The lock period for ENERGY this time cannot be less than the remaining time")
    );
  String txId = PublicMethod.delegateResourceV2LockAndGetTxId(
        fromAddress, delegateBalance, 1, true, lockPeriod + 1000,
        receiverAddress, fromKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    TransactionInfo info = PublicMethod.getTransactionInfoById(txId, blockingStubFull).get();
  Long afterDelegateTime = info.getBlockTimeStamp() - 3000L;

    Optional<DelegatedResourceList> afterDelegatedResourceList
        = PublicMethod.getDelegatedResourceV2(fromAddress, receiverAddress, blockingStubFull);
  Long afterUnlockTimeStamp
        = afterDelegatedResourceList.get().getDelegatedResource(0).getExpireTimeForEnergy();
    Assert.assertEquals(afterUnlockTimeStamp - afterDelegateTime, (lockPeriod + 1000) * 3 * 1000);
    Long normalInterval = (lockPeriod + 1000) * 3 * 1000;
    Long maintenanceInterval = (lockPeriod + 1000) * 3 * 1000;
  }

  @Test(enabled = true, description = "Delegate twice in same block and lock = true,lockPeriod=0L", groups = {"daily", "staking"})
  public void test08DelegateLockTwiceInSameBlock() {
    ECKey from = new ECKey(Utils.getRandom());
  byte[] fromAddress = from.getAddress();
  String fromKey = ByteArray.toHexString(from.getPrivKeyBytes());
    PublicMethod.printAddress(fromKey);
    PublicMethod.sendcoin(
        fromAddress, 1000000000L, foundationAddress, foundationKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.freezeBalanceV2(fromAddress, 200000000L, 0, fromKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    GrpcAPI.CanDelegatedMaxSizeResponseMessage message
        = PublicMethod.getCanDelegatedMaxSize(fromAddress, 0, blockingStubFull).get();
    logger.info("canDelegateResource:" + message);
  Long canDelegateResource = message.getMaxSize();
  String txId1 = PublicMethod.delegateResourceV2LockAndGetTxId(fromAddress,
        canDelegateResource / 3, 0, true, null, receiverAddress, fromKey, blockingStubFull);
  String txId2 = PublicMethod.delegateResourceV2LockAndGetTxId(fromAddress,
        canDelegateResource / 4, 0, true, null, receiverAddress, fromKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Transaction transaction1
        =  PublicMethod.getTransactionById(txId1, blockingStubFull).get();
    Transaction transaction2
        = PublicMethod.getTransactionById(txId2, blockingStubFull).get();
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
    PublicMethod.freeResource(frozenBandwidthAddress,
        frozenBandwidthKey, foundationAddress, blockingStubFull);  }
}


