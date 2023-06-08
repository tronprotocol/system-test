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
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Utils;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class FreezeBalanceV2Test007 {

  private final String testFoundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] testFoundationAddress = PublicMethed.getFinalAddress(testFoundationKey);

  private final String testWitnessKey = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] testWitnessAddress = PublicMethed.getFinalAddress(testWitnessKey);

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] testAddress001 = ecKey1.getAddress();
  String testKey001 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

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

    PublicMethed.printAddress(testKey001);
    Assert.assertTrue(PublicMethed.sendcoin(testAddress001,200_000000L, testFoundationAddress, testFoundationKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.freezeBalanceV2(testAddress001,20000000L,0,testKey001,blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceV2(testAddress001,20000000L,1,testKey001,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "cancel unexpired unfreeze net, unfreeze count=1")
  public void test01CancelAllUnfreezeNet() {
    GrpcAPI.AccountResourceMessage resource1 = PublicMethed.getAccountResource(testAddress001, blockingStubFull);
    Assert.assertEquals(resource1.getTronPowerLimit(),40);
    logger.info(resource1.toString());

    Assert.assertTrue(PublicMethed.unFreezeBalanceV2(testAddress001,testKey001,1000000L,0, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    GrpcAPI.AccountResourceMessage resource2 = PublicMethed.getAccountResource(testAddress001, blockingStubFull);
    Assert.assertEquals(resource2.getTronPowerLimit(), 39);
    logger.info(resource2.toString());
    Protocol.Account account2 = PublicMethed.queryAccount(testAddress001, blockingStubFull);
    long balance2 = account2.getBalance();

    String txid = PublicMethed.cancelAllUnFreezeBalanceV2AndGetTxid(testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Protocol.TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("test01CancelAllUnfreezeNet info: " + info.toString());
    Assert.assertEquals(info.getCancelAllUnfreezeV2Amount(), 1000000L);

    GrpcAPI.AccountResourceMessage resource3 = PublicMethed.getAccountResource(testAddress001, blockingStubFull);
    logger.info(resource3.toString());
    Assert.assertEquals(resource3.getTronPowerLimit(), 40);
    Protocol.Account account3 = PublicMethed.queryAccount(testAddress001, blockingStubFull);
    logger.info(account3.toString());
    long balance3 = account3.getBalance();
    Assert.assertEquals(balance2, balance3);
    Assert.assertEquals(account3.getFrozenV2(0).getAmount(), 20000000);
    Assert.assertEquals(account3.getFrozenV2(1).getAmount(), 20000000);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "cancel all unexpired unfreeze energy")
  public void test02CancelAllUnfreezeEnergy() {
    logger.info(PublicMethed.getAccountResource(testAddress001, blockingStubFull).toString());
    Assert.assertTrue(PublicMethed.unFreezeBalanceV2(testAddress001,testKey001,1000000L,1, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String txid = PublicMethed.cancelAllUnFreezeBalanceV2AndGetTxid(testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Protocol.TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("test02CancelAllUnfreezeEnergy info: " + info.toString());
    Assert.assertEquals(info.getCancelAllUnfreezeV2Amount(), 1000000L);
    GrpcAPI.AccountResourceMessage resource2 = PublicMethed.getAccountResource(testAddress001, blockingStubFull);
    logger.info(resource2.toString());
    Assert.assertEquals(resource2.getTronPowerLimit(), 40);
    Protocol.Account account2 = PublicMethed.queryAccount(testAddress001, blockingStubFull);
    logger.info("test02CancelAllUnfreezeEnergy account2: " + account2.toString());
    Assert.assertEquals(account2.getUnfrozenV2Count(), 0);
    Assert.assertEquals(account2.getFrozenV2(0).getAmount(), 20000000);
    Assert.assertEquals(account2.getFrozenV2(1).getAmount(), 20000000);
  }

  @Test(enabled = true, description = "cancel all unexpired unfreeze net and energy")
  public void test03CancelAllUnfreezeNetAndEnergy() {
    logger.info(PublicMethed.getAccountResource(testAddress001, blockingStubFull).toString());
    Assert.assertTrue(PublicMethed.unFreezeBalanceV2(testAddress001, testKey001,1000000L,0, blockingStubFull));
    Assert.assertTrue(PublicMethed.unFreezeBalanceV2(testAddress001, testKey001,1000000L,1, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    GrpcAPI.AccountResourceMessage resource1 = PublicMethed.getAccountResource(testAddress001, blockingStubFull);
    Assert.assertEquals(resource1.getTronPowerLimit(), 38);
    String txid = PublicMethed.cancelAllUnFreezeBalanceV2AndGetTxid(testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Protocol.TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("test03CancelAllUnfreezeNetAndEnergy info: " + info.toString());
    GrpcAPI.AccountResourceMessage resource2 = PublicMethed.getAccountResource(testAddress001, blockingStubFull);
    logger.info(resource2.toString());
    Assert.assertEquals(resource2.getTronPowerLimit(), 40);
    Protocol.Account account2 = PublicMethed.queryAccount(testAddress001, blockingStubFull);
    logger.info("test03 account2: " + account2.toString());
    Assert.assertEquals(account2.getUnfrozenV2Count(), 0);
    Assert.assertEquals(account2.getFrozenV2(0).getAmount(), 20000000);
    Assert.assertEquals(account2.getFrozenV2(1).getAmount(), 20000000);
  }

  @Test(enabled = true, description = "cancel 32  unexpired unfreeze")
  public void test04CancelAll32Unfreeze() {
    logger.info(PublicMethed.queryAccount(testAddress001, blockingStubFull).toString());
    for(int i=0;i<32;i++){
      Assert.assertTrue(PublicMethed.unFreezeBalanceV2(testAddress001, testKey001, i + 1,0, blockingStubFull));
    }
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    GrpcAPI.AccountResourceMessage resource1 = PublicMethed.getAccountResource(testAddress001, blockingStubFull);
    Assert.assertEquals( resource1.getTronPowerLimit(), 39);
    String txid = PublicMethed.cancelAllUnFreezeBalanceV2AndGetTxid(testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Protocol.TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("test04CancelAll32Unfreeze info: " + info.toString());
    Assert.assertEquals(info.getCancelAllUnfreezeV2Amount(), 528);
    GrpcAPI.AccountResourceMessage resource2 = PublicMethed.getAccountResource(testAddress001, blockingStubFull);
    logger.info(resource2.toString());
    Assert.assertEquals(resource2.getTronPowerLimit(), 40);
    Protocol.Account account2 = PublicMethed.queryAccount(testAddress001, blockingStubFull);
    logger.info(account2.toString());
    Assert.assertEquals(0, account2.getUnfrozenV2Count());
    Assert.assertEquals(account2.getFrozenV2(1).getAmount(), 20000000);
    Assert.assertEquals(account2.getFrozenV2(1).getAmount(), 20000000);
  }

  @Test(enabled = true, description = "cancel all net and energy include expired and unexpired")
  public void test05CancelAllUnfreeze() {
    logger.info(PublicMethed.getAccountResource(testAddress001, blockingStubFull).toString());
    for (int i = 0; i < 2; i++) {
      Assert.assertTrue(PublicMethed.unFreezeBalanceV2(testAddress001, testKey001, 1000000L*(i+1), 0, blockingStubFull));
      Assert.assertTrue(PublicMethed.unFreezeBalanceV2(testAddress001, testKey001, 1000000L*(i+1), 1, blockingStubFull));
      try {
        Thread.sleep(30000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    long expireTime = PublicMethed.queryAccount(testAddress001, blockingStubFull).getUnfrozenV2(0).getUnfreezeExpireTime();
    while (true) {
      if (System.currentTimeMillis() > expireTime) {
        break;
      }
      PublicMethed.waitProduceNextBlock(blockingStubFull);
    }

    String txid  = PublicMethed.cancelAllUnFreezeBalanceV2AndGetTxid(testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Protocol.TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("test05CancelAllUnfreeze info: " + info.toString());
    Assert.assertEquals(info.getCancelAllUnfreezeV2Amount(), 4000000);
    Assert.assertEquals(info.getWithdrawExpireAmount(), 2000000);
    GrpcAPI.AccountResourceMessage resource2 = PublicMethed.getAccountResource(testAddress001, blockingStubFull);
    Assert.assertEquals(resource2.getTronPowerLimit(), 38);
    logger.info(resource2.toString());
    Protocol.Account account2 = PublicMethed.queryAccount(testAddress001, blockingStubFull);
    Assert.assertEquals(account2.getUnfrozenV2Count(), 0);
    Assert.assertEquals(account2.getFrozenV2(0).getAmount(), 19000000);
    Assert.assertEquals(account2.getFrozenV2(1).getAmount(), 19000000);
    logger.info(PublicMethed.getAccountResource(testAddress001, blockingStubFull).toString());
  }

  @Test(enabled = true, description = "vote after cancel all unfreeze")
  public void test05VoteAfterCancelAllUnfreeze() {
    HashMap<byte[], Long> voteMap = new HashMap<>();
    voteMap.put(testWitnessAddress, 38L);
    Assert.assertTrue(PublicMethed.voteWitness(testAddress001, testKey001, voteMap, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Protocol.Account account1 = PublicMethed.queryAccount(testAddress001, blockingStubFull);
    Assert.assertEquals(account1.getVotesCount(), 1);
    byte[] witness = account1.getVotes(0).getVoteAddress().toByteArray();
    long voteCount = account1.getVotes(0).getVoteCount();
    Assert.assertEquals(witness, testWitnessAddress);
    Assert.assertEquals(voteCount, 38);
  }





  //todo: vote/unfreeze32+cancel/cancel expired/

  /**
   * constructor.
   */
  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    PublicMethed.freedResource(testAddress001, testKey001, testFoundationAddress, blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


