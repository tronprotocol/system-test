package stest.tron.wallet.dailybuild.freezeV2;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
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
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.AccountContract.AccountPermissionUpdateContract;
import org.tron.protos.contract.BalanceContract.FreezeBalanceV2Contract;
import org.tron.protos.contract.BalanceContract.UnfreezeBalanceV2Contract;
import org.tron.protos.contract.BalanceContract.WithdrawExpireUnfreezeContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.ProposalEnum;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Utils;

@Slf4j
public class FreezeBalanceV2Test001 {
  private static final long sendAmount = 10000000000L;

  private final String foundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] foundationAddress = PublicMethed.getFinalAddress(foundationKey);

  private final String witnessKey = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] witnessAddress = PublicMethed.getFinalAddress(witnessKey);

  private final Long periodTime = 60_000L;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] frozenBandwidthAddress = ecKey1.getAddress();
  String frozenBandwidthKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] frozenEnergyAddress = ecKey2.getAddress();
  String frozenEnergyKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  Long freezeBandwidthBalance = 2000000L;
  Long freezeEnergyBalance = 3000000L;

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode =
      Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
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
    blockingStubPbft= WalletSolidityGrpc.newBlockingStub(channelPbft);
    Assert.assertTrue(PublicMethed.sendcoin(frozenBandwidthAddress, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(frozenEnergyAddress, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);


  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Freeze balance to get bandwidth")
  public void test01FreezeBalanceV2GetBandwidth() throws Exception {
    AccountResourceMessage accountResource = PublicMethed
        .getAccountResource(frozenBandwidthAddress, blockingStubFull);
    final Long beforeTotalNetWeight = accountResource.getTotalNetWeight();

    String txId = PublicMethed.freezeBalanceV2AndGetTxId(frozenBandwidthAddress,
            freezeBandwidthBalance, 0, frozenBandwidthKey, blockingStubFull);
    Assert.assertNotNull(txId);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Transaction transaction =
        PublicMethed.getTransactionById(txId, blockingStubFull).get();
    Any any = transaction.getRawData().getContract(0).getParameter();
    FreezeBalanceV2Contract freezeBalanceV2Contract
        = any.unpack(FreezeBalanceV2Contract.class);
    Assert.assertTrue(freezeBalanceV2Contract.getFrozenBalance() == freezeBandwidthBalance);
    Assert.assertEquals(freezeBalanceV2Contract.getOwnerAddress().toByteArray(),
        frozenBandwidthAddress);
    Assert.assertTrue(freezeBalanceV2Contract.getResourceValue() == 0);

    Account account = PublicMethed.queryAccount(frozenBandwidthAddress, blockingStubFull);

    Assert.assertEquals(account.getFrozenV2Count(), 3);
    Assert.assertTrue(account.getFrozenV2(0).getAmount() == freezeBandwidthBalance);
    Assert.assertTrue(account.getFrozenV2(0).getTypeValue() == 0);

    accountResource = PublicMethed
        .getAccountResource(frozenBandwidthAddress, blockingStubFull);
    final Long afterTotalNetWeight = accountResource.getTotalNetWeight();
    final Long afterNetLimit = accountResource.getNetLimit();

    Assert.assertTrue(afterNetLimit > 0);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Freeze balance to get energy")
  public void test02FreezeBalanceV2GetEnergy() throws Exception {
    AccountResourceMessage accountResource = PublicMethed
        .getAccountResource(frozenEnergyAddress, blockingStubFull);
    final Long beforeTotalEnergyWeight = accountResource.getTotalEnergyWeight();

    String txId = PublicMethed.freezeBalanceV2AndGetTxId(frozenEnergyAddress,
            freezeEnergyBalance, 1, frozenEnergyKey, blockingStubFull);
    Assert.assertNotNull(txId);
    PublicMethed.waitProduceNextBlock(blockingStubFull);


    Transaction transaction =
        PublicMethed.getTransactionById(txId, blockingStubFull).get();
    Any any = transaction.getRawData().getContract(0).getParameter();
    FreezeBalanceV2Contract freezeBalanceV2Contract
        = any.unpack(FreezeBalanceV2Contract.class);
    Assert.assertTrue(freezeBalanceV2Contract.getFrozenBalance() == freezeEnergyBalance);
    Assert.assertEquals(
        freezeBalanceV2Contract.getOwnerAddress().toByteArray(), frozenEnergyAddress);
    Assert.assertTrue(freezeBalanceV2Contract.getResourceValue() == 1);


    Account account = PublicMethed.queryAccount(frozenEnergyAddress, blockingStubFull);

    Assert.assertEquals(account.getFrozenV2Count(), 3);
    Assert.assertTrue(account.getFrozenV2(1).getAmount() == freezeEnergyBalance);
    Assert.assertTrue(account.getFrozenV2(1).getTypeValue() == 1);

    accountResource = PublicMethed
        .getAccountResource(frozenEnergyAddress, blockingStubFull);
    final Long afterTotalEnergyWeight = accountResource.getTotalEnergyWeight();
    final Long afterEnergyLimit = accountResource.getEnergyLimit();

    Assert.assertTrue(afterEnergyLimit > 0);
  }



  /**
   * constructor.
   */
  @Test(enabled = true, description = "Unfreeze balance to release bandwidth")
  public void test03UnFreezeBalanceV2ToReleaseBandwidth() throws Exception {
    Account account = PublicMethed.queryAccount(frozenBandwidthAddress, blockingStubFull);
    final Long beforeUnfreezeBalance = account.getBalance();
    AccountResourceMessage accountResource = PublicMethed
        .getAccountResource(frozenBandwidthAddress, blockingStubFull);
    final Long beforeTotalNetWeight = accountResource.getTotalNetWeight();

    String txId = PublicMethed.unFreezeBalanceV2AndGetTxId(frozenBandwidthAddress,
            frozenBandwidthKey, freezeBandwidthBalance, 0, blockingStubFull);
    Assert.assertNotNull(txId);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Transaction transaction =
        PublicMethed.getTransactionById(txId, blockingStubFull).get();
    Any any = transaction.getRawData().getContract(0).getParameter();
    UnfreezeBalanceV2Contract unfreezeBalanceV2Contract
        = any.unpack(UnfreezeBalanceV2Contract.class);
    Assert.assertTrue(unfreezeBalanceV2Contract.getUnfreezeBalance() == freezeBandwidthBalance);
    Assert.assertEquals(unfreezeBalanceV2Contract.getOwnerAddress().toByteArray(),
        frozenBandwidthAddress);
    Assert.assertTrue(unfreezeBalanceV2Contract.getResourceValue() == 0);




    account = PublicMethed.queryAccount(frozenBandwidthAddress, blockingStubFull);

    Assert.assertEquals(account.getFrozenV2Count(), 3);
    Assert.assertTrue(account.getFrozenV2(0).getAmount() == 0);
    Assert.assertTrue(account.getFrozenV2(0).getTypeValue() == 0);

    accountResource = PublicMethed
        .getAccountResource(frozenBandwidthAddress, blockingStubFull);
    final Long afterTotalNetWeight = accountResource.getTotalNetWeight();
    final Long afterNetLimit = accountResource.getNetLimit();
    final Long afterNetUsage = accountResource.getNetUsed();
    final Long afterUnfreezeBalance = account.getBalance();
    final Long afterUnfreezeTimestamp = System.currentTimeMillis();
    Assert.assertTrue(afterNetLimit == 0L);
    Assert.assertTrue(afterNetUsage > 0);
    Assert.assertEquals(beforeUnfreezeBalance, afterUnfreezeBalance);
    Assert.assertTrue(account.getUnfrozenV2Count() == 1);
    Assert.assertTrue(account.getUnfrozenV2(0).getUnfreezeExpireTime() > afterUnfreezeTimestamp
        && account.getUnfrozenV2(0).getUnfreezeExpireTime() <= afterUnfreezeTimestamp + periodTime);
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Freeze balance to release energy")
  public void test04UnFreezeBalanceV2ToReleaseEnergy() throws Exception {
    AccountResourceMessage accountResource = PublicMethed
        .getAccountResource(frozenEnergyAddress, blockingStubFull);
    final Long beforeTotalEnergyWeight = accountResource.getTotalEnergyWeight();

    String txId = PublicMethed.unFreezeBalanceV2AndGetTxId(frozenEnergyAddress,
            frozenEnergyKey, freezeEnergyBalance, 1, blockingStubFull);
    Assert.assertNotNull(txId);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Transaction transaction = PublicMethed.getTransactionById(txId,
        blockingStubFull).get();
    Any any = transaction.getRawData().getContract(0).getParameter();
    UnfreezeBalanceV2Contract unfreezeBalanceV2Contract
        = any.unpack(UnfreezeBalanceV2Contract.class);
    Assert.assertTrue(unfreezeBalanceV2Contract.getUnfreezeBalance() == freezeEnergyBalance);
    Assert.assertEquals(unfreezeBalanceV2Contract.getOwnerAddress().toByteArray(),
        frozenEnergyAddress);
    Assert.assertTrue(unfreezeBalanceV2Contract.getResourceValue() == 1);



    Account account = PublicMethed.queryAccount(frozenEnergyAddress, blockingStubFull);

    Assert.assertEquals(account.getFrozenV2Count(), 3);
    Assert.assertTrue(account.getFrozenV2(1).getAmount() == 0);
    Assert.assertTrue(account.getFrozenV2(1).getTypeValue() == 1);

    accountResource = PublicMethed
        .getAccountResource(frozenEnergyAddress, blockingStubFull);
    final Long afterTotalEnergyWeight = accountResource.getTotalEnergyWeight();
    final Long afterEnergyLimit = accountResource.getEnergyLimit();
    final Long afterUnfreezeTimestamp = System.currentTimeMillis();
    Assert.assertTrue(afterEnergyLimit == 0);
    logger.info(
        "account.getUnfrozenV2(0).getUnfreezeExpireTime():"
            + account.getUnfrozenV2(0).getUnfreezeExpireTime()
    );
    logger.info("afterUnfreezeTimestamp:" + afterUnfreezeTimestamp);
    Assert.assertTrue(account.getUnfrozenV2(0).getUnfreezeExpireTime() > afterUnfreezeTimestamp
        && account.getUnfrozenV2(0).getUnfreezeExpireTime() <= afterUnfreezeTimestamp + periodTime);
  }



  /**
   * constructor.
   */
  @Test(enabled = true, description = "Withdraw expire unfreeze to release balance")
  public void test05WithdrawExpireUnfreezeToReleaseBalance() throws Exception {
    Account account = PublicMethed.queryAccount(frozenBandwidthAddress, blockingStubFull);
    final Long bandwidthAccountBeforeBalance = account.getBalance();

    account = PublicMethed.queryAccount(frozenEnergyAddress, blockingStubFull);
    final Long energyAccountBeforeBalance = account.getBalance();


    if (System.currentTimeMillis() - 2000L < account.getUnfrozenV2(0).getUnfreezeExpireTime()) {
      Assert.assertFalse(PublicMethed.withdrawExpireUnfreeze(frozenEnergyAddress,
          frozenEnergyKey, blockingStubFull));
      logger.info("Check before expire time ,can't withdraw, function pass");
      int retryTimes = 0;
      Long unfreezeExpireTime = account.getUnfrozenV2(0).getUnfreezeExpireTime();
      while (
          retryTimes++ <= periodTime / 6000L && System.currentTimeMillis() < unfreezeExpireTime
      ) {
        PublicMethed.waitProduceNextBlock(blockingStubFull);
      }
    }

    Long canWithdrawUnFreezeAmount = PublicMethed
        .getCanWithdrawUnfreezeAmount(frozenEnergyAddress,
            System.currentTimeMillis(), blockingStubFull).get().getAmount();
    //query solidity
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubFullSolidity);
    Long canWithdrawUnFreezeAmountSolidity = PublicMethed
        .getCanWithdrawUnfreezeAmountSolidity(frozenEnergyAddress,
            System.currentTimeMillis(), blockingStubFullSolidity).get().getAmount();
    Assert.assertEquals(canWithdrawUnFreezeAmount, canWithdrawUnFreezeAmountSolidity);
    //query pbft
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubPbft);
    Long canWithdrawUnFreezeAmountPbft = PublicMethed
        .getCanWithdrawUnfreezeAmountSolidity(frozenEnergyAddress,
            System.currentTimeMillis(), blockingStubPbft).get().getAmount();
    Assert.assertEquals(canWithdrawUnFreezeAmount, canWithdrawUnFreezeAmountPbft);

    String txIdBandwidth = PublicMethed.withdrawExpireUnfreezeAndGetTxId(frozenBandwidthAddress,
            frozenBandwidthKey, blockingStubFull);
    String txIdEnergy = PublicMethed.withdrawExpireUnfreezeAndGetTxId(frozenEnergyAddress,
            frozenEnergyKey, blockingStubFull);
    Assert.assertNotNull(txIdBandwidth);
    Assert.assertNotNull(txIdEnergy);




    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Transaction transaction = PublicMethed.getTransactionById(txIdEnergy,
        blockingStubFull).get();
    Any any = transaction.getRawData().getContract(0).getParameter();
    WithdrawExpireUnfreezeContract withdrawExpireUnfreezeContract
        = any.unpack(WithdrawExpireUnfreezeContract.class);
    Assert.assertEquals(withdrawExpireUnfreezeContract.getOwnerAddress().toByteArray(),
        frozenEnergyAddress);



    account = PublicMethed.queryAccount(frozenBandwidthAddress, blockingStubFull);
    final Long bandwidthAccountAfterBalance = account.getBalance();

    account = PublicMethed.queryAccount(frozenEnergyAddress, blockingStubFull);
    final Long energyAccountAfterBalance = account.getBalance();

    Assert.assertTrue(
        bandwidthAccountAfterBalance - bandwidthAccountBeforeBalance == freezeBandwidthBalance);
    Assert.assertTrue(
        energyAccountAfterBalance - energyAccountBeforeBalance == freezeEnergyBalance);


    TransactionInfo transactionInfo = PublicMethed.getTransactionInfoById(txIdEnergy,
        blockingStubFull).get();
    Assert.assertTrue(transactionInfo.getWithdrawExpireAmount() == freezeEnergyBalance);
    Assert.assertTrue(transactionInfo.getUnfreezeAmount() == 0);
    Assert.assertEquals(freezeEnergyBalance, canWithdrawUnFreezeAmount);
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


