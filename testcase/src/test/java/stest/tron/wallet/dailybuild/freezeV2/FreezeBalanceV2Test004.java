package stest.tron.wallet.dailybuild.freezeV2;

import com.google.protobuf.Any;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Account.UnFreezeV2;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.Protocol.Vote;
import org.tron.protos.contract.BalanceContract.FreezeBalanceV2Contract;
import org.tron.protos.contract.BalanceContract.UnfreezeBalanceV2Contract;
import org.tron.protos.contract.BalanceContract.WithdrawExpireUnfreezeContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.*;
import stest.tron.wallet.common.client.utils.Flaky;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
@Flaky(reason = "Timing-sensitive: unfreeze wait period race condition",
    since = "2026-04-03")
public class FreezeBalanceV2Test004 extends TronBaseTest {
  private static final long sendAmount = 10000000000L;
  private final byte[] witnessAddress = PublicMethod.getFinalAddress(witnessKey);
  private final Long periodTime = 60_000L;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] frozenBandwidthAddress = ecKey1.getAddress();
  String frozenBandwidthKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] frozenEnergyAddress = ecKey2.getAddress();
  String frozenEnergyKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  Long freezeBandwidthBalance = 2000000000L;
  Long unfreezeBalance = 1000000L;
  Long freezeEnergyBalance = 300000L;
  Integer maxUnfreezeListCount = 32;
  private String soliditynode = Configuration.getByPath("testng.conf").getStringList("solidityNode.ip.list")
      .get(0);
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFullSolidity = null;
  private String pbftnode =
      Configuration.getByPath("testng.conf").getStringList("solidityNode.ip.list")
          .get(2);

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() throws Exception{
    initPbftChannel();
    initSolidityChannel();
    PublicMethod.printAddress(frozenBandwidthKey);    if(!PublicMethod.freezeV2ProposalIsOpen(blockingStubFull)) {      throw new SkipException("Skipping freezeV2 test case");
    }
    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext()
        .build();
    blockingStubFullSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
    channelPbft = ManagedChannelBuilder.forTarget(pbftnode)
        .usePlaintext()
        .build();
    Assert.assertTrue(PublicMethod.sendcoin(frozenBandwidthAddress, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.sendcoin(frozenEnergyAddress, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethod.freezeBalanceV2(frozenBandwidthAddress,
        freezeBandwidthBalance,0,frozenBandwidthKey,blockingStubFull));
    if(PublicMethod.tronPowerProposalIsOpen(blockingStubFull)) {
      Assert.assertTrue(PublicMethod.freezeBalanceV2(frozenBandwidthAddress,
          freezeBandwidthBalance,2,frozenBandwidthKey,blockingStubFull));
    }
    PublicMethod.waitSolidityNodeSynFullNodeData(blockingStubFull,blockingStubFullSolidity);

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Max unfreeze balance list is 32", groups = {"daily", "staking"})
  public void test01MaxUnfreezeBalanceListIs32() throws Exception{
    Account account = PublicMethod.queryAccount(frozenBandwidthAddress,blockingStubFull);
  Long beforeBalance = account.getBalance();

    PublicMethod.getAvailableUnfreezeCount(frozenBandwidthAddress,blockingStubFull);
    Assert.assertTrue(PublicMethod.getAvailableUnfreezeCount(frozenBandwidthAddress
        ,blockingStubFull).get().getCount() == 32);
  //query solidity
    Assert.assertTrue(PublicMethod.getAvailableUnfreezeCountSolidity(frozenBandwidthAddress
        ,blockingStubFullSolidity).get().getCount() == 32);
  int unfreezeTimes = 0;
    while (unfreezeTimes++ <= 50) {
      PublicMethod.unFreezeBalanceV2(frozenBandwidthAddress,frozenBandwidthKey,unfreezeBalance,0,blockingStubFull);
      Thread.sleep(100L);
    }
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    logger.info("frozenBandwidthAddress111: " + Base58.encode58Check(frozenBandwidthAddress));
  Long count = PublicMethod.getAvailableUnfreezeCount(frozenBandwidthAddress
            , blockingStubFull).get().getCount();
    Account accountFrozenBandwidthAddress = PublicMethod.queryAccount(frozenBandwidthAddress, blockingStubFull);
    logger.info("account++: ");
    logger.info(accountFrozenBandwidthAddress.toString());
    Assert.assertEquals(count.longValue(), 0L);
  //query solidity
    PublicMethod.waitSolidityNodeSynFullNodeData(blockingStubFull,blockingStubFullSolidity);
  //query pbft
    Assert.assertTrue(PublicMethod.getAvailableUnfreezeCountSolidity(frozenBandwidthAddress
            ,blockingStubPbft).get().getCount() == 0);
    Assert.assertTrue(PublicMethod.getAvailableUnfreezeCountSolidity(frozenBandwidthAddress
        ,blockingStubFullSolidity).get().getCount() == 0);

    account = PublicMethod.queryAccount(frozenBandwidthAddress,blockingStubFull);
    Assert.assertTrue(account.getUnfrozenV2Count() == maxUnfreezeListCount);
  Long maxUnfreezeExpireTime = Long.MIN_VALUE;
  Long totalUnfreezeBalance = 0L;
    List<UnFreezeV2> list = account.getUnfrozenV2List();
    for(UnFreezeV2 unFreezeV2 : list) {
      totalUnfreezeBalance += unFreezeV2.getUnfreezeAmount();
      maxUnfreezeExpireTime = Math.max(maxUnfreezeExpireTime,unFreezeV2.getUnfreezeExpireTime());
    }

    logger.info("maxUnfreezeExpireTime" + maxUnfreezeExpireTime);
    Assert.assertTrue(totalUnfreezeBalance == maxUnfreezeListCount * unfreezeBalance);
  int retryTimes = 0;
    while (retryTimes++ <= periodTime / 3000L && System.currentTimeMillis() - 4000 <= maxUnfreezeExpireTime) {
      logger.info("System.currentTimeMillis()" + System.currentTimeMillis());
      PublicMethod.waitProduceNextBlock(blockingStubFull);
    }
    logger.info("Final System.currentTimeMillis()" + System.currentTimeMillis());
  String txId = PublicMethod.unFreezeBalanceV2AndGetTxId(frozenBandwidthAddress,frozenBandwidthKey,unfreezeBalance,0,blockingStubFull);

    Assert.assertNotNull(txId);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    TransactionInfo transactionInfo = PublicMethod.getTransactionInfoById(txId, blockingStubFull).get();

    account = PublicMethod.queryAccount(frozenBandwidthAddress,blockingStubFull);
  Long afterBalance = account.getBalance();
    Assert.assertEquals(account.getUnfrozenV2Count(),1);
    Assert.assertTrue((afterBalance - beforeBalance) == totalUnfreezeBalance);
    Assert.assertTrue(transactionInfo.getWithdrawExpireAmount() == totalUnfreezeBalance);
    Assert.assertTrue(transactionInfo.getUnfreezeAmount() == 0);

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Unfreeze cause dynamic decrease of vote", groups = {"daily", "staking"})
  public void test02UnfreezeCaseDynamicDecreaseOfVote() throws Exception{
    Long witness1ReceiveVote = 10L;
  Long witness2ReceiveVote = 20L;
    Account account = PublicMethod.queryAccount(frozenBandwidthAddress,blockingStubFull);
  Long votePower = !PublicMethod.tronPowerProposalIsOpen(blockingStubFull) ?
        account.getFrozenV2(0).getAmount() / 1000000L :
        account.getFrozenV2(2).getAmount() / 1000000L;

    HashMap<byte[],Long> voteMap = new HashMap<>();
    voteMap.put(PublicMethod.getFinalAddress(witnessKey),witness1ReceiveVote);
    voteMap.put(PublicMethod.getFinalAddress(witnessKey),witness2ReceiveVote);

    Assert.assertTrue(PublicMethod.voteWitness(frozenBandwidthAddress,frozenBandwidthKey,voteMap,blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long unfreezeBalance = (votePower - witness1ReceiveVote - witness2ReceiveVote) * 1000000L;
    Assert.assertTrue(PublicMethod.unFreezeBalanceV2(frozenBandwidthAddress,frozenBandwidthKey,unfreezeBalance,
        PublicMethod.tronPowerProposalIsOpen(blockingStubFull) ? 2 : 0,blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    account = PublicMethod.queryAccount(frozenBandwidthAddress,blockingStubFull);
    List<Vote> list = account.getVotesList();
    Assert.assertTrue(list.size() == 2);
  Long currentVote = 0L;
    for(Vote vote : list) {
      currentVote += vote.getVoteCount();
    }
    Assert.assertTrue(currentVote == witness1ReceiveVote + witness2ReceiveVote);

    Assert.assertTrue(PublicMethod.unFreezeBalanceV2(frozenBandwidthAddress,frozenBandwidthKey,(currentVote - witness1ReceiveVote / 10 - witness2ReceiveVote / 10) * 1000000L,
        PublicMethod.tronPowerProposalIsOpen(blockingStubFull) ? 2 : 0,blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    account = PublicMethod.queryAccount(frozenBandwidthAddress,blockingStubFull);
    list = account.getVotesList();
    Assert.assertTrue(list.size() == 2);
    currentVote = 0L;
    for(Vote vote : list) {
      currentVote += vote.getVoteCount();
    }
    Assert.assertTrue(currentVote * 10  == witness1ReceiveVote + witness2ReceiveVote);

    PublicMethod.unFreezeBalanceV2(frozenBandwidthAddress,frozenBandwidthKey,1L,
        PublicMethod.tronPowerProposalIsOpen(blockingStubFull) ? 2 : 0,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    account = PublicMethod.queryAccount(frozenBandwidthAddress,blockingStubFull);
    list = account.getVotesList();
    Assert.assertTrue(list.size() == 1);
    currentVote = 0L;
    for(Vote vote : list) {
      currentVote += vote.getVoteCount();
    }
    Assert.assertTrue(currentVote == 1L);

    if(PublicMethod.tronPowerProposalIsOpen(blockingStubFull)) {
      Assert.assertTrue(PublicMethod.freezeBalanceV2(frozenBandwidthAddress,
          (witness1ReceiveVote + witness2ReceiveVote) * 1000000L,2,frozenBandwidthKey,blockingStubFull));
    } else {
      Assert.assertTrue(PublicMethod.freezeBalanceV2(frozenBandwidthAddress,
          (witness1ReceiveVote + witness2ReceiveVote) * 1000000L,0,frozenBandwidthKey,blockingStubFull));
    }
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethod.voteWitness(frozenBandwidthAddress,frozenBandwidthKey,voteMap,blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    account = PublicMethod.queryAccount(frozenBandwidthAddress,blockingStubFull);
    list = account.getVotesList();
    Assert.assertTrue(list.size() == 2);
    currentVote = 0L;
    for(Vote vote : list) {
      currentVote += vote.getVoteCount();
    }
    Assert.assertTrue(currentVote == witness1ReceiveVote + witness2ReceiveVote);

  }

  /**
   * constructor.
   */
  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(frozenBandwidthAddress, frozenBandwidthKey, foundationAddress, blockingStubFull);  }
}

