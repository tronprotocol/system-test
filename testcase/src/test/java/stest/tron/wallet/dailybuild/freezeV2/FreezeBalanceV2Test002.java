package stest.tron.wallet.dailybuild.freezeV2;

import com.google.protobuf.Any;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
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
import org.tron.protos.Protocol.Account.AccountResource;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.contract.BalanceContract.DelegateResourceContract;
import org.tron.protos.contract.BalanceContract.FreezeBalanceV2Contract;
import org.tron.protos.contract.BalanceContract.UnDelegateResourceContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Utils;
import zmq.socket.pubsub.Pub;

@Slf4j
public class FreezeBalanceV2Test002 {
  private static final long sendAmount = 10000000000L;

  private final String foundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] foundationAddress = PublicMethed.getFinalAddress(foundationKey);

  private final String witnessKey = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] witnessAddress = PublicMethed.getFinalAddress(witnessKey);

  private final Long periodTime = 60_000L * 5;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] frozenBandwidthAddress = ecKey1.getAddress();
  String frozenBandwidthKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] frozenEnergyAddress = ecKey2.getAddress();
  String frozenEnergyKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());


  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] receiveEnergyAddress = ecKey3.getAddress();
  String receiveEnergyKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());


  ECKey ecKey4 = new ECKey(Utils.getRandom());
  byte[] receiveBandwidthAddress = ecKey4.getAddress();
  String receiveBandwidthKey = ByteArray.toHexString(ecKey4.getPrivKeyBytes());


  ECKey ecKey5 = new ECKey(Utils.getRandom());
  byte[] delegateFromAddress = ecKey5.getAddress();
  String delegateFromKey = ByteArray.toHexString(ecKey5.getPrivKeyBytes());

  Long freezeBandwidthBalance = 4000000L;

  Long delegateBandwidthAmount = freezeBandwidthBalance / 2;
  Long freezeEnergyBalance = 300000000L;

  Long delegateEnergyAmount = freezeEnergyBalance / 2;

  Long beforeDelegateBandwidthNetLimit = -1L;

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode =
      Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list").get(0);
  private String soliditynode =
      Configuration.getByPath("testng.conf").getStringList("solidityNode.ip.list").get(0);
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
    PublicMethed.printAddress(frozenEnergyKey);
    PublicMethed.printAddress(receiveBandwidthKey);
    PublicMethed.printAddress(receiveBandwidthKey);
    PublicMethed.printAddress(delegateFromKey);
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
    Assert.assertTrue(PublicMethed.sendcoin(frozenBandwidthAddress, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(frozenEnergyAddress, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(receiveBandwidthAddress, 1L,
        foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(receiveEnergyAddress, 1L,
        foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(delegateFromAddress, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.freezeBalanceV2(frozenBandwidthAddress, freezeBandwidthBalance,
        0, frozenBandwidthKey, blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceV2(frozenEnergyAddress, freezeEnergyBalance,
        1, frozenEnergyKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);




  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Delegate resource of bandwidth")
  public void test01DelegateResourceOfBandwidth() throws Exception {
    Account account = PublicMethed.queryAccount(frozenBandwidthAddress, blockingStubFull);
    AccountResourceMessage accountResource = PublicMethed
        .getAccountResource(frozenBandwidthAddress, blockingStubFull);
    final Long beforeLenderFrozenAmount = account.getFrozenV2(0).getAmount();
    final Long beforeLenderNetLimit = accountResource.getNetLimit();
    logger.info("beforeLenderNetLimit: " + beforeLenderNetLimit);
    beforeDelegateBandwidthNetLimit = beforeLenderNetLimit;

    String txId = PublicMethed.delegateResourceV2AndGetTxId(frozenBandwidthAddress,
        delegateBandwidthAmount, 0, receiveBandwidthAddress, frozenBandwidthKey, blockingStubFull);
    Assert.assertNotNull(txId);
    PublicMethed.waitProduceNextBlock(blockingStubFull);


    Transaction transaction = PublicMethed.getTransactionById(txId, blockingStubFull).get();
    Any any = transaction.getRawData().getContract(0).getParameter();
    DelegateResourceContract delegateResourceContract
        = any.unpack(DelegateResourceContract.class);
    Assert.assertTrue(delegateResourceContract.getBalance() == delegateBandwidthAmount);
    Assert.assertEquals(delegateResourceContract.getOwnerAddress().toByteArray(),
        frozenBandwidthAddress);
    Assert.assertTrue(delegateResourceContract.getResourceValue() == 0);
    Assert.assertEquals(delegateResourceContract.getReceiverAddress().toByteArray(),
        receiveBandwidthAddress);




    account = PublicMethed.queryAccount(frozenBandwidthAddress,
        blockingStubFull);
    accountResource = PublicMethed
        .getAccountResource(frozenBandwidthAddress, blockingStubFull);
    final Long afterLenderFrozenAmount = account.getFrozenV2(0).getAmount();
    final Long afterLenderNetLimit = accountResource.getNetLimit();
    logger.info("afterLenderNetLimit: " + afterLenderNetLimit);
    final Long afterLenderAmount = account.getAcquiredDelegatedFrozenBalanceForBandwidth()
        + account.getAcquiredDelegatedFrozenV2BalanceForBandwidth() + afterLenderFrozenAmount;


    Assert.assertTrue(
        beforeLenderFrozenAmount - afterLenderFrozenAmount == delegateBandwidthAmount);
    Assert.assertTrue(account.getDelegatedFrozenV2BalanceForBandwidth() == delegateBandwidthAmount);

    Long netTotalWeight = accountResource.getTotalNetWeight();
    Long netTotalLimit = accountResource.getTotalNetLimit();
    final double afterLenderNetLimitDouble =
        ((double) afterLenderAmount / (double) (netTotalWeight * 1000000)) * netTotalLimit;
    logger.info("afterLenderNetLimitDouble: " + afterLenderNetLimitDouble);
    final Long afterLenderNetLimitShouldBe = Math.round(afterLenderNetLimitDouble);
    Assert.assertTrue(Math.abs(afterLenderNetLimitShouldBe - afterLenderNetLimit) <= 1);


    accountResource = PublicMethed
        .getAccountResource(receiveBandwidthAddress, blockingStubFull);
    final Long afterReceiverNetLimit = accountResource.getNetLimit();
    logger.info("afterReceiverNetLimit: " + afterReceiverNetLimit);

    account = PublicMethed.queryAccount(receiveBandwidthAddress, blockingStubFull);
    final Long receiverAcquiredDelegatedFrozenBalanceForBandwidth = account
        .getAcquiredDelegatedFrozenV2BalanceForBandwidth();
    final Long afterReceiverAmount = account.getAcquiredDelegatedFrozenBalanceForBandwidth()
        + account.getAcquiredDelegatedFrozenV2BalanceForBandwidth()
        + account.getFrozenV2(0).getAmount();

    netTotalWeight = accountResource.getTotalNetWeight();
    netTotalLimit = accountResource.getTotalNetLimit();
    final double afterReceiverNetLimitDouble =
        ((double) afterReceiverAmount / (double) (netTotalWeight * 1000000)) * netTotalLimit;
    logger.info("afterReceiverNetLimitDouble: " + afterReceiverNetLimitDouble);
    final Long afterReceiverNetLimitShouldBe = Math.round(afterReceiverNetLimitDouble);
    logger.info("afterReceiverNetLimitShouldBe: " + afterReceiverNetLimitShouldBe);
    Assert.assertTrue(
        Math.abs(afterReceiverNetLimitShouldBe - afterReceiverNetLimit) <= 1);
    Assert.assertEquals(
        receiverAcquiredDelegatedFrozenBalanceForBandwidth, delegateBandwidthAmount);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Undelegate resource of bandwidth")
  public void test02UnDelegateResourceOfBandwidth() throws Exception {
    Account account = PublicMethed.queryAccount(frozenBandwidthAddress, blockingStubFull);
    AccountResourceMessage accountResource = PublicMethed
         .getAccountResource(frozenBandwidthAddress, blockingStubFull);
    final Long beforeLenderFrozenAmount = account.getFrozenV2(0).getAmount();
    final Long beforeLenderNetLimit = accountResource.getNetLimit();

    accountResource = PublicMethed
         .getAccountResource(receiveBandwidthAddress, blockingStubFull);
    final Long beforeReceiverNetLimit = accountResource.getNetLimit();
    account = PublicMethed.queryAccount(receiveBandwidthAddress, blockingStubFull);
    final Long beforeAcquiredDelegatedFrozenBalanceForBandwidth = account
            .getAcquiredDelegatedFrozenBalanceForBandwidth();


    String txId = PublicMethed.unDelegateResourceV2AndGetTxId(frozenBandwidthAddress,
        delegateBandwidthAmount, 0, receiveBandwidthAddress, frozenBandwidthKey, blockingStubFull);
    Assert.assertNotNull(txId);
    PublicMethed.waitProduceNextBlock(blockingStubFull);


    Transaction transaction = PublicMethed.getTransactionById(txId, blockingStubFull).get();
    Any any = transaction.getRawData().getContract(0).getParameter();
    UnDelegateResourceContract unDelegateResourceContract
            = any.unpack(UnDelegateResourceContract.class);
    Assert.assertTrue(unDelegateResourceContract.getBalance() == delegateBandwidthAmount);
    Assert.assertEquals(unDelegateResourceContract.getOwnerAddress().toByteArray(),
        frozenBandwidthAddress);
    Assert.assertTrue(unDelegateResourceContract.getResourceValue() == 0);
    Assert.assertEquals(unDelegateResourceContract.getReceiverAddress().toByteArray(),
        receiveBandwidthAddress);



    account = PublicMethed.queryAccount(frozenBandwidthAddress, blockingStubFull);
    accountResource = PublicMethed
            .getAccountResource(frozenBandwidthAddress, blockingStubFull);
    final Long afterLenderFrozenAmount = account.getFrozenV2(0).getAmount();
    final Long afterDelegateResourceAmount = account.getDelegatedFrozenBalanceForBandwidth();
    final Long afterLenderNetLimit = accountResource.getNetLimit();
    final Long afterLenderAmount = account.getAcquiredDelegatedFrozenBalanceForBandwidth()
            + account.getAcquiredDelegatedFrozenV2BalanceForBandwidth()
            + account.getFrozenV2(0).getAmount();

    Assert.assertTrue(
        beforeLenderFrozenAmount - afterLenderFrozenAmount == -delegateBandwidthAmount);
    final Long netTotalWeight = accountResource.getTotalNetWeight();
    final Long netTotalLimit = accountResource.getTotalNetLimit();
    final double netLimitDouble =
        ((double) afterLenderAmount / (double) (netTotalWeight * 1000000)) * (double) netTotalLimit;
    logger.info("netLimitDouble: " + netLimitDouble);
    logger.info("afterLenderNetLimit: " + afterLenderNetLimit);
    Long netLimitShouldBe = Math.round(netLimitDouble);
    Assert.assertTrue(Math.abs(netLimitShouldBe - afterLenderNetLimit) <= 1);

    accountResource = PublicMethed
            .getAccountResource(receiveBandwidthAddress, blockingStubFull);
    final Long afterReceiverNetLimit = accountResource.getNetLimit();
    account = PublicMethed.queryAccount(receiveBandwidthAddress, blockingStubFull);
    final Long receiverAcquiredDelegatedFrozenBalanceForBandwidth = account
            .getAcquiredDelegatedFrozenBalanceForBandwidth();
    Assert.assertEquals((long) receiverAcquiredDelegatedFrozenBalanceForBandwidth, 0);
    Assert.assertEquals((long) afterReceiverNetLimit, 0);
    Assert.assertEquals((long) afterDelegateResourceAmount, 0);

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Test GetCanDelegatedMaxSize api")
  public void test03TestGetCanDelegatedMaxSizeApi() throws Exception {
    Long canDelegatedMaxSizeWithNoNetUsed = PublicMethed
        .getCanDelegatedMaxSize(frozenBandwidthAddress, 0, blockingStubFull)
        .get()
        .getMaxSize();
    logger.info("canDelegatedMaxSizeWithNoNetUsed" + canDelegatedMaxSizeWithNoNetUsed);
    //query solidity
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubFullSolidity);
    Long canDelegatedMaxSizeWithNoNetUsedSolidity =  PublicMethed
        .getCanDelegatedMaxSizeSolidity(frozenBandwidthAddress, 0, blockingStubFullSolidity)
        .get()
        .getMaxSize();
    logger.info(
        "canDelegatedMaxSizeWithNoNetUsedSolidity" + canDelegatedMaxSizeWithNoNetUsedSolidity
    );
    //query pbft
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubPbft);
    Long canDelegatedMaxSizeWithNoNetUsedPbft =  PublicMethed
        .getCanDelegatedMaxSizeSolidity(frozenBandwidthAddress, 0, blockingStubPbft)
        .get()
        .getMaxSize();
    logger.info(
        "canDelegatedMaxSizeWithNoNetUsedPbft"
            + canDelegatedMaxSizeWithNoNetUsedPbft
    );
    int retryTimes = 0;
    while (PublicMethed.getCanDelegatedMaxSize(frozenBandwidthAddress, 0, blockingStubFull).get()
        .getMaxSize() + 1100000 > canDelegatedMaxSizeWithNoNetUsed && retryTimes++ <= 5000) {
      logger.info(
          "Current" + PublicMethed
              .getCanDelegatedMaxSize(frozenBandwidthAddress, 0, blockingStubFull)
              .get()
              .getMaxSize()
      );
      PublicMethed.sendcoinGetTransactionIdForConstructData(foundationAddress,
          1L, frozenBandwidthAddress, frozenBandwidthKey, blockingStubFull);
      Thread.sleep(10L);
    }

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long canDelegatedMaxSizeWithNetUsed =  PublicMethed.getCanDelegatedMaxSize(
        frozenBandwidthAddress, 0, blockingStubFull)
        .get()
        .getMaxSize();
    Assert.assertTrue(canDelegatedMaxSizeWithNoNetUsed > canDelegatedMaxSizeWithNetUsed + 1000000);

    Assert.assertFalse(
        PublicMethed.delegateResourceV2(
            frozenBandwidthAddress,
            canDelegatedMaxSizeWithNetUsed + 1,
            0,
            receiveBandwidthAddress,
            frozenBandwidthKey,
            blockingStubFull
        )
    );
    Assert.assertTrue(PublicMethed.delegateResourceV2(
        frozenBandwidthAddress,
        canDelegatedMaxSizeWithNetUsed,
        0,
        receiveBandwidthAddress,
        frozenBandwidthKey,
        blockingStubFull)
    );

    //query solidity
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubFullSolidity);
    Long canDelegatedMaxSizeWithNetUsedSolidity =  PublicMethed.getCanDelegatedMaxSizeSolidity(
        frozenBandwidthAddress, 0, blockingStubFullSolidity)
        .get()
        .getMaxSize();
    logger.info("canDelegatedMaxSizeWithNetUsedSolidity: " + canDelegatedMaxSizeWithNetUsedSolidity);
    //query pbft
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubPbft);
    Long canDelegatedMaxSizeWithNetUsedPbft =  PublicMethed.getCanDelegatedMaxSizeSolidity(
        frozenBandwidthAddress, 0, blockingStubPbft)
        .get()
        .getMaxSize();
    logger.info("canDelegatedMaxSizeWithNetUsedPbft: " + canDelegatedMaxSizeWithNetUsedPbft);
    Assert.assertEquals(canDelegatedMaxSizeWithNetUsedPbft.longValue(), canDelegatedMaxSizeWithNetUsedSolidity.longValue());

    Assert.assertTrue(PublicMethed.freezeBalanceV2(delegateFromAddress,
        freezeBandwidthBalance, 0, delegateFromKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.delegateResourceV2(delegateFromAddress,
        PublicMethed.getCanDelegatedMaxSize(delegateFromAddress, 0, blockingStubFull)
            .get()
            .getMaxSize(),
        0,
        frozenBandwidthAddress,
        delegateFromKey,
        blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Long canDelegatedMaxSizeWithOtherDelegatedToMe =  PublicMethed.getCanDelegatedMaxSize(
        frozenBandwidthAddress, 0, blockingStubFull)
        .get()
        .getMaxSize();

    Assert.assertEquals(
        (long) freezeBandwidthBalance,
        canDelegatedMaxSizeWithNetUsed + canDelegatedMaxSizeWithOtherDelegatedToMe);

    Assert.assertTrue(PublicMethed.delegateResourceV2(
        frozenBandwidthAddress,
        canDelegatedMaxSizeWithOtherDelegatedToMe,
        0,
        receiveBandwidthAddress,
        frozenBandwidthKey,
        blockingStubFull)
    );


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


