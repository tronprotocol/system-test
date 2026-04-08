package stest.tron.wallet.dailybuild.freezeV2;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.*;

import java.util.HashMap;
import stest.tron.wallet.common.client.utils.Flaky;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
@Flaky(reason = "Timing-sensitive: cancel unfreeze wait period",
    since = "2026-04-03")
public class FreezeBalanceV2Test007 extends TronBaseTest {

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] testAddress001 = ecKey1.getAddress();
  String testKey001 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private String soliditynode =
      Configuration.getByPath("testng.conf").getStringList("solidityNode.ip.list")
      .get(0);
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFullSolidity = null;
  private String pbftnode =
      Configuration.getByPath("testng.conf").getStringList("solidityNode.ip.list")
          .get(2);

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() throws Exception {    if (!PublicMethod.freezeV2ProposalIsOpen(blockingStubFull)) {      throw new SkipException("Skipping freezeV2 test case");
    }
    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext()
        .build();
    blockingStubFullSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
    channelPbft = ManagedChannelBuilder.forTarget(pbftnode)
        .usePlaintext()
        .build();
    blockingStubPbft= WalletSolidityGrpc.newBlockingStub(channelPbft);

    PublicMethod.printAddress(testKey001);
    Assert.assertTrue(PublicMethod.sendcoin(testAddress001,200_000000L, foundationAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethod.freezeBalanceV2(testAddress001,20000000L,0,testKey001,blockingStubFull));
    Assert.assertTrue(PublicMethod.freezeBalanceV2(testAddress001,20000000L,1,testKey001,blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "cancel unexpired unfreeze net, unfreeze count=1", groups = {"daily", "staking"})
  public void test01CancelAllUnfreezeNet() {
    GrpcAPI.AccountResourceMessage resource1 = PublicMethod.getAccountResource(testAddress001, blockingStubFull);
    Assert.assertEquals(resource1.getTronPowerLimit(),40);
    logger.info(resource1.toString());

    Assert.assertTrue(PublicMethod.unFreezeBalanceV2(testAddress001,testKey001,1000000L,0, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    GrpcAPI.AccountResourceMessage resource2 = PublicMethod.getAccountResource(testAddress001, blockingStubFull);
    Assert.assertEquals(resource2.getTronPowerLimit(), 39);
    logger.info(resource2.toString());
    Protocol.Account account2 = PublicMethod.queryAccount(testAddress001, blockingStubFull);
    long balance2 = account2.getBalance();
  String txid = PublicMethod.cancelAllUnFreezeBalanceV2AndGetTxid(testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Protocol.TransactionInfo info = PublicMethod.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("test01CancelAllUnfreezeNet info: " + info.toString());
    Assert.assertEquals(info.getCancelUnfreezeV2AmountMap().get("BANDWIDTH").longValue(), 1000000L);
    Assert.assertEquals(info.getWithdrawExpireAmount(), 0);
    GrpcAPI.AccountResourceMessage resource3 = PublicMethod.getAccountResource(testAddress001, blockingStubFull);
    logger.info(resource3.toString());
    Assert.assertEquals(resource3.getTronPowerLimit(), 40);
    Protocol.Account account3 = PublicMethod.queryAccount(testAddress001, blockingStubFull);
    logger.info(account3.toString());
    long balance3 = account3.getBalance();
    Assert.assertEquals(balance2, balance3);
    Assert.assertEquals(account3.getFrozenV2(0).getAmount(), 20000000);
    Assert.assertEquals(account3.getFrozenV2(1).getAmount(), 20000000);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "cancel all unexpired unfreeze energy", groups = {"daily", "staking"})
  public void test02CancelAllUnfreezeEnergy() {
    logger.info(PublicMethod.getAccountResource(testAddress001, blockingStubFull).toString());
    Assert.assertTrue(PublicMethod.unFreezeBalanceV2(testAddress001,testKey001,1000000L,1, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    long balance1 = PublicMethod.queryAccount(testAddress001, blockingStubFull).getBalance();
  String txid = PublicMethod.cancelAllUnFreezeBalanceV2AndGetTxid(testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Protocol.TransactionInfo info = PublicMethod.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("test02CancelAllUnfreezeEnergy info: " + info.toString());
    Assert.assertEquals(info.getWithdrawExpireAmount(), 0);
    Assert.assertEquals(info.getCancelUnfreezeV2AmountMap().get("ENERGY").longValue(), 1000000L);
    GrpcAPI.AccountResourceMessage resource2 = PublicMethod.getAccountResource(testAddress001, blockingStubFull);
    logger.info(resource2.toString());
    Assert.assertEquals(resource2.getTronPowerLimit(), 40);
    Protocol.Account account2 = PublicMethod.queryAccount(testAddress001, blockingStubFull);
    logger.info("test02CancelAllUnfreezeEnergy account2: " + account2.toString());
    Assert.assertEquals(account2.getUnfrozenV2Count(), 0);
    Assert.assertEquals(account2.getFrozenV2(0).getAmount(), 20000000);
    Assert.assertEquals(account2.getFrozenV2(1).getAmount(), 20000000);
    Assert.assertEquals(balance1 - info.getReceipt().getNetFee(), account2.getBalance());
  }

  @Test(enabled = true, description = "cancel all unexpired unfreeze net and energy", groups = {"daily", "staking"})
  public void test03CancelAllUnfreezeNetAndEnergy() {
    logger.info(PublicMethod.getAccountResource(testAddress001, blockingStubFull).toString());
    Assert.assertTrue(PublicMethod.unFreezeBalanceV2(testAddress001, testKey001,1000000L,0, blockingStubFull));
    Assert.assertTrue(PublicMethod.unFreezeBalanceV2(testAddress001, testKey001,1000000L,1, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    GrpcAPI.AccountResourceMessage resource1 = PublicMethod.getAccountResource(testAddress001, blockingStubFull);
    Assert.assertEquals(resource1.getTronPowerLimit(), 38);
    long balance1 = PublicMethod.queryAccount(testAddress001, blockingStubFull).getBalance();
  String txid = PublicMethod.cancelAllUnFreezeBalanceV2AndGetTxid(testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Protocol.TransactionInfo info = PublicMethod.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("test03CancelAllUnfreezeNetAndEnergy info: " + info.toString());
    Assert.assertEquals(info.getWithdrawExpireAmount(), 0);
    Assert.assertEquals(info.getCancelUnfreezeV2AmountMap().get("ENERGY").longValue(), 1000000L);
    Assert.assertEquals(info.getCancelUnfreezeV2AmountMap().get("BANDWIDTH").longValue(), 1000000L);
    GrpcAPI.AccountResourceMessage resource2 = PublicMethod.getAccountResource(testAddress001, blockingStubFull);
    logger.info(resource2.toString());
    Assert.assertEquals(resource2.getTronPowerLimit(), 40);
    Protocol.Account account2 = PublicMethod.queryAccount(testAddress001, blockingStubFull);
    logger.info("test03 account2: " + account2.toString());
    Assert.assertEquals(account2.getUnfrozenV2Count(), 0);
    Assert.assertEquals(account2.getFrozenV2(0).getAmount(), 20000000);
    Assert.assertEquals(account2.getFrozenV2(1).getAmount(), 20000000);
    Assert.assertEquals(balance1 - info.getReceipt().getNetFee(), account2.getBalance());
  }

  @Test(enabled = true, description = "cancel 32  unexpired unfreeze", groups = {"daily", "staking"})
  public void test04CancelAll32Unfreeze() {
    logger.info(PublicMethod.queryAccount(testAddress001, blockingStubFull).toString());
    for(int i=0;i<32;i++){
      Assert.assertTrue(PublicMethod.unFreezeBalanceV2(testAddress001, testKey001, i + 1,i%2, blockingStubFull));
    }
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    GrpcAPI.AccountResourceMessage resource1 = PublicMethod.getAccountResource(testAddress001, blockingStubFull);
    Assert.assertEquals( resource1.getTronPowerLimit(), 39);
    long balance1 = PublicMethod.queryAccount(testAddress001, blockingStubFull).getBalance();
  String txid = PublicMethod.cancelAllUnFreezeBalanceV2AndGetTxid(testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Protocol.TransactionInfo info = PublicMethod.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("test04CancelAll32Unfreeze info: " + info.toString());
    Assert.assertEquals(info.getCancelUnfreezeV2AmountMap().get("ENERGY").longValue(), 272L);
    Assert.assertEquals(info.getCancelUnfreezeV2AmountMap().get("BANDWIDTH").longValue(), 256L);
    Assert.assertEquals(info.getWithdrawExpireAmount(), 0);
    GrpcAPI.AccountResourceMessage resource2 = PublicMethod.getAccountResource(testAddress001, blockingStubFull);
    logger.info(resource2.toString());
    Assert.assertEquals(resource2.getTronPowerLimit(), 40);
    Protocol.Account account2 = PublicMethod.queryAccount(testAddress001, blockingStubFull);
    logger.info(account2.toString());
    Assert.assertEquals(0, account2.getUnfrozenV2Count());
    Assert.assertEquals(account2.getFrozenV2(1).getAmount(), 20000000);
    Assert.assertEquals(account2.getFrozenV2(1).getAmount(), 20000000);
    Assert.assertEquals(balance1 - info.getReceipt().getNetFee(), account2.getBalance());
  }

  @Test(enabled = true, description = "cancel all net and energy include expired and unexpired",
      groups = {"daily", "staking"})
  public void test05CancelAllUnfreeze() {
    logger.info(PublicMethod.getAccountResource(testAddress001, blockingStubFull).toString());
    for (int i = 0; i < 2; i++) {
      Assert.assertTrue(PublicMethod.unFreezeBalanceV2(testAddress001, testKey001, 1000000L*(i+1), 0, blockingStubFull));
      Assert.assertTrue(PublicMethod.unFreezeBalanceV2(testAddress001, testKey001, 1000000L*(i+1), 1, blockingStubFull));
      try {
        Thread.sleep(30000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    Protocol.Account account1 = PublicMethod.queryAccount(testAddress001, blockingStubFull);
    long expireTime = account1.getUnfrozenV2(0).getUnfreezeExpireTime();
    while (true) {
      Protocol.Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
      long nowTime=currentBlock.getBlockHeader().getRawData().getTimestamp();
      if (nowTime > expireTime) {
        break;
      }
      PublicMethod.waitProduceNextBlock(blockingStubFull);
    }
    long balanceBeforeCancel = account1.getBalance();
  String txid  = PublicMethod.cancelAllUnFreezeBalanceV2AndGetTxid(testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubFullSolidity);

    Protocol.TransactionInfo info = PublicMethod.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("test05CancelAllUnfreeze info: " + info.toString());
    Assert.assertEquals(info.getCancelUnfreezeV2AmountMap().get("ENERGY").longValue(), 2000000L);
    Assert.assertEquals(info.getCancelUnfreezeV2AmountMap().get("BANDWIDTH").longValue(), 2000000L);
    Assert.assertEquals(info.getWithdrawExpireAmount(), 2000000);
    Protocol.TransactionInfo info1 = PublicMethod.getTransactionInfoByIdFromSolidity(txid, blockingStubFullSolidity).get();
    Protocol.TransactionInfo info2 = PublicMethod.getTransactionInfoByIdFromSolidity(txid, blockingStubPbft).get();

    GrpcAPI.AccountResourceMessage resource2 = PublicMethod.getAccountResource(testAddress001, blockingStubFull);
    logger.info(resource2.toString());
    Assert.assertEquals(resource2.getTronPowerLimit(), 38);

    Protocol.Account account2 = PublicMethod.queryAccount(testAddress001, blockingStubFull);
    logger.info(account2.toString());
    Assert.assertEquals(account2.getUnfrozenV2Count(), 0);
    Assert.assertEquals(account2.getFrozenV2(0).getAmount(), 19000000);
    Assert.assertEquals(account2.getFrozenV2(1).getAmount(), 19000000);
    long balanceAfterCancel = account2.getBalance();
    Assert.assertEquals(balanceBeforeCancel - info.getReceipt().getNetFee() + info.getWithdrawExpireAmount(),
        balanceAfterCancel );
    Assert.assertEquals(info, info1);
    Assert.assertEquals(info, info2);
  }

  @Test(enabled = true, description = "vote after cancel all unfreeze", groups = {"daily", "staking"})
  public void test06VoteAfterCancelAllUnfreeze() {
    HashMap<byte[], Long> voteMap = new HashMap<>();
    voteMap.put(witnessAddress, 38L);
    Assert.assertTrue(PublicMethod.voteWitness(testAddress001, testKey001, voteMap, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Protocol.Account account1 = PublicMethod.queryAccount(testAddress001, blockingStubFull);
    Assert.assertEquals(account1.getVotesCount(), 1);
    byte[] witness = account1.getVotes(0).getVoteAddress().toByteArray();
    long voteCount = account1.getVotes(0).getVoteCount();
    Assert.assertEquals(witness, witnessAddress);
    Assert.assertEquals(voteCount, 38);
    GrpcAPI.AccountResourceMessage resource1 = PublicMethod.getAccountResource(testAddress001, blockingStubFull);
    Assert.assertEquals(resource1.getTronPowerLimit(), 38);
    Assert.assertEquals(resource1.getTronPowerLimit(), resource1.getTronPowerUsed());
  }

  @Test(enabled = true, description = "all freeze vote, then unfreeze some , then cancel unfreeze. expect vote desc", groups = {"daily", "staking"})
  public void test07UnfreezeWithVotedAndCancelAllUnfreeze() {
    Assert.assertTrue(PublicMethod.unFreezeBalanceV2(testAddress001, testKey001, 19000000L, 0, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Protocol.Account account1 = PublicMethod.queryAccount(testAddress001, blockingStubFull);
    long balance1 = account1.getBalance();
    Assert.assertEquals(account1.getVotesCount(), 1);
    byte[] witness = account1.getVotes(0).getVoteAddress().toByteArray();
    long voteCount = account1.getVotes(0).getVoteCount();
    Assert.assertEquals(witness, witnessAddress);
    Assert.assertEquals(voteCount, 19);
    GrpcAPI.AccountResourceMessage resource1 = PublicMethod.getAccountResource(testAddress001, blockingStubFull);
    Assert.assertEquals(resource1.getTronPowerLimit(), 19);
    Assert.assertEquals(resource1.getTronPowerLimit(), resource1.getTronPowerUsed());
    Assert.assertEquals(account1.getFrozenV2(1).getAmount(), 19000000);
    Assert.assertEquals(account1.getFrozenV2(0).getAmount(), 0);
    String txid  = PublicMethod.cancelAllUnFreezeBalanceV2AndGetTxid(testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Protocol.TransactionInfo info = PublicMethod.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("test06UnfreezeWithVotedAndCancelAllUnfreeze info: " + info.toString());
    Assert.assertEquals(info.getCancelUnfreezeV2AmountMap().get("BANDWIDTH").longValue(), 19000000L);
    Assert.assertEquals(info.getWithdrawExpireAmount(), 0);
    Protocol.Account account2 = PublicMethod.queryAccount(testAddress001, blockingStubFull);
    long balance2 = account2.getBalance();
    Assert.assertEquals(balance1 - info.getReceipt().getNetFee(), balance2 );
    Assert.assertEquals(account1.getVotesCount(), 1);
    witness = account1.getVotes(0).getVoteAddress().toByteArray();
    voteCount = account1.getVotes(0).getVoteCount();
    Assert.assertEquals(witness, witnessAddress);
    Assert.assertEquals(voteCount, 19);
    Assert.assertEquals(resource1.getTronPowerLimit(), resource1.getTronPowerUsed());
    Assert.assertEquals(account1.getFrozenV2(1).getAmount(), 19000000);
    Assert.assertEquals(account1.getFrozenV2(0).getAmount(), 0);
  }

  /**
   * constructor.
   */
  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(testAddress001, testKey001, foundationAddress, blockingStubFull);  }
}

