package stest.tron.wallet.dailybuild.freezeV2;

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
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Utils;
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

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() throws Exception {
    PublicMethed.printAddress(frozenBandwidthKey);
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
    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubFullSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
    channelPbft = ManagedChannelBuilder.forTarget(pbftnode)
        .usePlaintext(true)
        .build();
    blockingStubPbft= WalletSolidityGrpc.newBlockingStub(channelPbft);
    PublicMethed.printAddress(frozenBandwidthKey);
    PublicMethed.printAddress(receiverKey);
    Assert.assertTrue(PublicMethed.sendcoin(frozenBandwidthAddress, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(receiverAddress, 1L,
        foundationAddress, foundationKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.freezeBalanceV2(frozenBandwidthAddress, freezeBandwidthBalance, 0,
        frozenBandwidthKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);


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
    Long beforeLockTrueTime = System.currentTimeMillis();

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


    Long unlockTimeStamp =
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

  @Test(enabled = true, description = "delegateResource bandwidth lock = true and lockPeriod is a non-zero value")
  public void test02DelegateResourceBandWidthLockPeriodValueIsNonZeroTest() {
    ECKey from = new ECKey(Utils.getRandom());
    byte[] fromAddress = from.getAddress();
    String fromKey = ByteArray.toHexString(from.getPrivKeyBytes());
    PublicMethed.sendcoin(fromAddress, sendAmount, foundationAddress, foundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.freezeBalanceV2(fromAddress, freezeBandwidthBalance, 0, fromKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    final Long lockPeriod = 3L;
    PublicMethed.delegateResourceV2Lock(fromAddress, delegateBalance, 0, true, lockPeriod, receiverAddress, fromKey, blockingStubFull);
    Long currentTime = System.currentTimeMillis();
    logger.info("nowTime: " + System.currentTimeMillis());
    Optional<DelegatedResourceList> delegatedResourceList =  PublicMethed.getDelegatedResourceV2(fromAddress, receiverAddress, blockingStubFull);
    Long expireTimeForBandwidth = delegatedResourceList.get().getDelegatedResource(0).getExpireTimeForBandwidth();
    logger.info("delegatedResourceList: " + delegatedResourceList.get());
    Assert.assertTrue(Math.abs((expireTimeForBandwidth - currentTime) -(lockPeriod * 3 * 1000L)) < 1500);
  }

  @Test(enabled = true, description = "unDelegateResource energy after lockPeriod")
  public void test03unDelegateEnergyAfterLockPeriod() {
    ECKey from = new ECKey(Utils.getRandom());
    byte[] fromAddress = from.getAddress();
    String fromKey = ByteArray.toHexString(from.getPrivKeyBytes());
    PublicMethed.sendcoin(fromAddress, sendAmount, foundationAddress, foundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.freezeBalanceV2(fromAddress, freezeBandwidthBalance, 1, fromKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    final Long lockPeriod = 3L;
    PublicMethed.delegateResourceV2Lock(fromAddress, delegateBalance, 1, true, lockPeriod, receiverAddress, fromKey, blockingStubFull);
    Long currentTime = System.currentTimeMillis();
    logger.info("nowTime: " + System.currentTimeMillis());
    Optional<DelegatedResourceList> delegatedResourceList =  PublicMethed.getDelegatedResourceV2(fromAddress, receiverAddress, blockingStubFull);
    Long expireTimeForEnergy = delegatedResourceList.get().getDelegatedResource(0).getExpireTimeForEnergy();
    logger.info("delegatedResourceList: " + delegatedResourceList.get());
    Assert.assertTrue(Math.abs((expireTimeForEnergy - currentTime) - (lockPeriod * 3 * 1000L)) < 1500);

    // It can not unDelegated before expire time
    String txId1 = PublicMethed.unDelegateResourceV2AndGetTxId(fromAddress, delegateBalance, 1, receiverAddress, fromKey, blockingStubFull);
    Assert.assertNull(txId1);
    int retryTimes = 3;
    while (retryTimes-- > 0) {
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      logger.info("wait one block...");
    }
    String txId2 = PublicMethed.unDelegateResourceV2AndGetTxId(fromAddress, delegateBalance, 1, receiverAddress, fromKey, blockingStubFull);
    logger.info("txId2:" + txId2);
    Assert.assertNotNull(txId2);
    Transaction transaction = PublicMethed.getTransactionById(txId2, blockingStubFull).get();
    Assert.assertEquals(transaction.getRet(0).getContractRet(), Transaction.Result.contractResult.SUCCESS);
  }

  @Test(enabled = true, description = "lock = false an lockPeriod = non-zero value, lockPeriod is invalid")
  public void test04InvalidLockPeriodValueWhenLockIsFalse() {
    ECKey from = new ECKey(Utils.getRandom());
    byte[] fromAddress = from.getAddress();
    String fromKey = ByteArray.toHexString(from.getPrivKeyBytes());
    PublicMethed.sendcoin(fromAddress, sendAmount, foundationAddress, foundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.freezeBalanceV2(fromAddress, freezeBandwidthBalance, 1, fromKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.delegateResourceV2Lock(fromAddress, delegateBalance, 1, false, 50L, receiverAddress, fromKey, blockingStubFull);
    logger.info("nowTime: " + System.currentTimeMillis());
    Optional<DelegatedResourceList> delegatedResourceList =  PublicMethed.getDelegatedResourceV2(fromAddress, receiverAddress, blockingStubFull);
    Long expireTimeForEnergy = delegatedResourceList.get().getDelegatedResource(0).getExpireTimeForEnergy();
    Assert.assertTrue(expireTimeForEnergy == 0);
    String txId1 = PublicMethed.unDelegateResourceV2AndGetTxId(fromAddress, delegateBalance, 1, receiverAddress, fromKey, blockingStubFull);
    logger.info("unDelegateResourceV2AndGetTxId: " + txId1);
    Assert.assertNotNull(txId1);
    Transaction transaction = PublicMethed.getTransactionById(txId1, blockingStubFull).get();
    Assert.assertEquals(transaction.getRet(0).getContractRet(), Transaction.Result.contractResult.SUCCESS);
  }
  @Test(enabled = true, description = "only lock = true equals lock = true and lockPeriod = 0")
  public void test05LockPeriodIsZero() {
    ECKey from = new ECKey(Utils.getRandom());
    byte[] fromAddress = from.getAddress();
    String fromKey = ByteArray.toHexString(from.getPrivKeyBytes());
    PublicMethed.sendcoin(fromAddress, sendAmount, foundationAddress, foundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.freezeBalanceV2(fromAddress, freezeBandwidthBalance, 1, fromKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long beforeLockTrueTime = System.currentTimeMillis();
    PublicMethed.delegateResourceV2Lock(fromAddress, delegateBalance, 1, true, 0L, receiverAddress, fromKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<DelegatedResourceList> delegatedResourceList = PublicMethed.getDelegatedResourceV2(fromAddress, receiverAddress, blockingStubFull);
    Long unlockTimeStamp = delegatedResourceList.get().getDelegatedResource(0).getExpireTimeForEnergy();
    logger.info("beforeLockTrueTime: " + beforeLockTrueTime);
    logger.info("unlockTimeStamp: " + unlockTimeStamp);
    Assert.assertTrue(Math.abs((unlockTimeStamp - beforeLockTrueTime) - delegateLockTime) < 1500L);
    Assert.assertTrue(
      unlockTimeStamp > beforeLockTrueTime
        &&
        unlockTimeStamp <= System.currentTimeMillis() + delegateLockTime);
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


