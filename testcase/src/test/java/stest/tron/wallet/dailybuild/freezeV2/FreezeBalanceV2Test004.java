package stest.tron.wallet.dailybuild.freezeV2;

import com.google.protobuf.Any;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletGrpc;
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

@Slf4j
public class FreezeBalanceV2Test004 {
  private static final long sendAmount = 10000000000L;

  private final String foundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] foundationAddress = PublicMethed.getFinalAddress(foundationKey);

  private final String witness1Key = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] witnessAddress = PublicMethed.getFinalAddress(witness1Key);

  private final String witness2Key = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] witness2Address = PublicMethed.getFinalAddress(witness2Key);

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

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String soliditynode = Configuration.getByPath("testng.conf").getStringList("solidityNode.ip.list")
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
  public void beforeClass() throws Exception{
    PublicMethed.printAddress(frozenBandwidthKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext()
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    if(!PublicMethed.freezeV2ProposalIsOpen(blockingStubFull)) {
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
    blockingStubPbft= WalletSolidityGrpc.newBlockingStub(channelPbft);
    Assert.assertTrue(PublicMethed.sendcoin(frozenBandwidthAddress, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(frozenEnergyAddress, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.freezeBalanceV2(frozenBandwidthAddress,
        freezeBandwidthBalance,0,frozenBandwidthKey,blockingStubFull));
    if(PublicMethed.tronPowerProposalIsOpen(blockingStubFull)) {
      Assert.assertTrue(PublicMethed.freezeBalanceV2(frozenBandwidthAddress,
          freezeBandwidthBalance,2,frozenBandwidthKey,blockingStubFull));
    }
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull,blockingStubFullSolidity);


  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Max unfreeze balance list is 32")
  public void test01MaxUnfreezeBalanceListIs32() throws Exception{
    Account account = PublicMethed.queryAccount(frozenBandwidthAddress,blockingStubFull);
    Long beforeBalance = account.getBalance();

    PublicMethed.getAvailableUnfreezeCount(frozenBandwidthAddress,blockingStubFull);
    Assert.assertTrue(PublicMethed.getAvailableUnfreezeCount(frozenBandwidthAddress
        ,blockingStubFull).get().getCount() == 32);
    //query solidity
    Assert.assertTrue(PublicMethed.getAvailableUnfreezeCountSolidity(frozenBandwidthAddress
        ,blockingStubFullSolidity).get().getCount() == 32);
    int unfreezeTimes = 0;
    while (unfreezeTimes++ <= 50) {
      PublicMethed.unFreezeBalanceV2(frozenBandwidthAddress,frozenBandwidthKey,unfreezeBalance,0,blockingStubFull);
      Thread.sleep(100L);
    }
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("frozenBandwidthAddress111: " + Base58.encode58Check(frozenBandwidthAddress));
    Long count = PublicMethed.getAvailableUnfreezeCount(frozenBandwidthAddress
            , blockingStubFull).get().getCount();
    Account accountFrozenBandwidthAddress = PublicMethed.queryAccount(frozenBandwidthAddress, blockingStubFull);
    logger.info("account++: ");
    logger.info(accountFrozenBandwidthAddress.toString());
    Assert.assertEquals(count.longValue(), 0L);
    //query solidity
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull,blockingStubFullSolidity);
    //query pbft
    Assert.assertTrue(PublicMethed.getAvailableUnfreezeCountSolidity(frozenBandwidthAddress
            ,blockingStubPbft).get().getCount() == 0);
    Assert.assertTrue(PublicMethed.getAvailableUnfreezeCountSolidity(frozenBandwidthAddress
        ,blockingStubFullSolidity).get().getCount() == 0);



    account = PublicMethed.queryAccount(frozenBandwidthAddress,blockingStubFull);
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
      PublicMethed.waitProduceNextBlock(blockingStubFull);
    }
    logger.info("Final System.currentTimeMillis()" + System.currentTimeMillis());

    String txId = PublicMethed.unFreezeBalanceV2AndGetTxId(frozenBandwidthAddress,frozenBandwidthKey,unfreezeBalance,0,blockingStubFull);

    Assert.assertNotNull(txId);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    TransactionInfo transactionInfo = PublicMethed.getTransactionInfoById(txId, blockingStubFull).get();

    account = PublicMethed.queryAccount(frozenBandwidthAddress,blockingStubFull);
    Long afterBalance = account.getBalance();
    Assert.assertEquals(account.getUnfrozenV2Count(),1);
    Assert.assertTrue((afterBalance - beforeBalance) == totalUnfreezeBalance);
    Assert.assertTrue(transactionInfo.getWithdrawExpireAmount() == totalUnfreezeBalance);
    Assert.assertTrue(transactionInfo.getUnfreezeAmount() == 0);



  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Unfreeze cause dynamic decrease of vote")
  public void test02UnfreezeCaseDynamicDecreaseOfVote() throws Exception{
    Long witness1ReceiveVote = 10L;
    Long witness2ReceiveVote = 20L;
    Account account = PublicMethed.queryAccount(frozenBandwidthAddress,blockingStubFull);
    Long votePower = !PublicMethed.tronPowerProposalIsOpen(blockingStubFull) ?
        account.getFrozenV2(0).getAmount() / 1000000L :
        account.getFrozenV2(2).getAmount() / 1000000L;

    HashMap<byte[],Long> voteMap = new HashMap<>();
    voteMap.put(PublicMethed.getFinalAddress(witness1Key),witness1ReceiveVote);
    voteMap.put(PublicMethed.getFinalAddress(witness2Key),witness2ReceiveVote);

    Assert.assertTrue(PublicMethed.voteWitness(frozenBandwidthAddress,frozenBandwidthKey,voteMap,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);


    Long unfreezeBalance = (votePower - witness1ReceiveVote - witness2ReceiveVote) * 1000000L;
    Assert.assertTrue(PublicMethed.unFreezeBalanceV2(frozenBandwidthAddress,frozenBandwidthKey,unfreezeBalance,
        PublicMethed.tronPowerProposalIsOpen(blockingStubFull) ? 2 : 0,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    account = PublicMethed.queryAccount(frozenBandwidthAddress,blockingStubFull);
    List<Vote> list = account.getVotesList();
    Assert.assertTrue(list.size() == 2);
    Long currentVote = 0L;
    for(Vote vote : list) {
      currentVote += vote.getVoteCount();
    }
    Assert.assertTrue(currentVote == witness1ReceiveVote + witness2ReceiveVote);



    Assert.assertTrue(PublicMethed.unFreezeBalanceV2(frozenBandwidthAddress,frozenBandwidthKey,(currentVote - witness1ReceiveVote / 10 - witness2ReceiveVote / 10) * 1000000L,
        PublicMethed.tronPowerProposalIsOpen(blockingStubFull) ? 2 : 0,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    account = PublicMethed.queryAccount(frozenBandwidthAddress,blockingStubFull);
    list = account.getVotesList();
    Assert.assertTrue(list.size() == 2);
    currentVote = 0L;
    for(Vote vote : list) {
      currentVote += vote.getVoteCount();
    }
    Assert.assertTrue(currentVote * 10  == witness1ReceiveVote + witness2ReceiveVote);





    PublicMethed.unFreezeBalanceV2(frozenBandwidthAddress,frozenBandwidthKey,1L,
        PublicMethed.tronPowerProposalIsOpen(blockingStubFull) ? 2 : 0,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    account = PublicMethed.queryAccount(frozenBandwidthAddress,blockingStubFull);
    list = account.getVotesList();
    Assert.assertTrue(list.size() == 1);
    currentVote = 0L;
    for(Vote vote : list) {
      currentVote += vote.getVoteCount();
    }
    Assert.assertTrue(currentVote == 1L);



    if(PublicMethed.tronPowerProposalIsOpen(blockingStubFull)) {
      Assert.assertTrue(PublicMethed.freezeBalanceV2(frozenBandwidthAddress,
          (witness1ReceiveVote + witness2ReceiveVote) * 1000000L,2,frozenBandwidthKey,blockingStubFull));
    } else {
      Assert.assertTrue(PublicMethed.freezeBalanceV2(frozenBandwidthAddress,
          (witness1ReceiveVote + witness2ReceiveVote) * 1000000L,0,frozenBandwidthKey,blockingStubFull));
    }
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.voteWitness(frozenBandwidthAddress,frozenBandwidthKey,voteMap,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    account = PublicMethed.queryAccount(frozenBandwidthAddress,blockingStubFull);
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
    PublicMethed.freedResource(frozenBandwidthAddress, frozenBandwidthKey, foundationAddress, blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


