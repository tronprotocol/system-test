package stest.tron.wallet.dailybuild.freezeV2;

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
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Account.AccountResource;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Utils;

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

  Long freezeBandwidthBalance = 200000000L;

  Long delegateBandwidthAmount = freezeBandwidthBalance / 2;
  Long freezeEnergyBalance = 300000000L;

  Long delegateEnergyAmount = freezeEnergyBalance / 2;

  Long beforeDelegateBandwidthNetLimit = -1L;

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() throws Exception{
    PublicMethed.printAddress(frozenBandwidthKey);
    PublicMethed.printAddress(frozenEnergyKey);
    PublicMethed.printAddress(receiveBandwidthKey);
    PublicMethed.printAddress(receiveBandwidthKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    if(!PublicMethed.freezeV2ProposalIsOpen(blockingStubFull)) {
      if (channelFull != null) {
        channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
      }
      throw new SkipException("Skipping freezeV2 test case");
    }
    Assert.assertTrue(PublicMethed.sendcoin(frozenBandwidthAddress, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(frozenEnergyAddress, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(receiveBandwidthAddress, 1L,
        foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(receiveEnergyAddress, 1L,
        foundationAddress, foundationKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.freezeBalanceV2(frozenBandwidthAddress,freezeBandwidthBalance,0,frozenBandwidthKey,blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceV2(frozenEnergyAddress,freezeEnergyBalance,1,frozenEnergyKey,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);


  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Delegate resource of bandwidth")
  public void test01DelegateResourceOfBandwidth() {
    Account account = PublicMethed.queryAccount(frozenBandwidthAddress,blockingStubFull);
    AccountResourceMessage accountResource = PublicMethed
        .getAccountResource(frozenBandwidthAddress, blockingStubFull);
    final Long beforeLenderFrozenAmount = account.getFrozenV2(0).getAmount();
    final Long beforeLenderNetLimit = accountResource.getNetLimit();
    beforeDelegateBandwidthNetLimit = beforeLenderNetLimit;


    Assert.assertTrue(PublicMethed.delegateResourceV2(frozenBandwidthAddress,delegateBandwidthAmount,
        0, receiveBandwidthAddress,frozenBandwidthKey,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    account = PublicMethed.queryAccount(frozenBandwidthAddress,blockingStubFull);
    accountResource = PublicMethed
        .getAccountResource(frozenBandwidthAddress, blockingStubFull);
    final Long afterLenderFrozenAmount = account.getFrozenV2(0).getAmount();
    final Long afterLenderNetLimit = accountResource.getNetLimit();

    Assert.assertTrue(beforeLenderFrozenAmount - afterLenderFrozenAmount == delegateBandwidthAmount);
    Assert.assertTrue(account.getDelegatedFrozenBalanceForBandwidth() == delegateBandwidthAmount);

    accountResource = PublicMethed
        .getAccountResource(receiveBandwidthAddress, blockingStubFull);
    final Long afterReceiverNetLimit = accountResource.getNetLimit();
    account = PublicMethed.queryAccount(receiveBandwidthAddress,blockingStubFull);
    final Long receiverAcquiredDelegatedFrozenBalanceForBandwidth = account
        .getAcquiredDelegatedFrozenBalanceForBandwidth();

    Assert.assertTrue(afterLenderNetLimit + afterReceiverNetLimit >= beforeLenderNetLimit - 1
    && afterLenderFrozenAmount + afterReceiverNetLimit <= beforeLenderNetLimit + 1);
    Assert.assertEquals(receiverAcquiredDelegatedFrozenBalanceForBandwidth, delegateBandwidthAmount);
  }



  /**
   * constructor.
   */
  @Test(enabled = true, description = "Undelegate resource of bandwidth")
  public void test02UnDelegateResourceOfBandwidth() {
    Account account = PublicMethed.queryAccount(frozenBandwidthAddress,blockingStubFull);
    AccountResourceMessage accountResource = PublicMethed
        .getAccountResource(frozenBandwidthAddress, blockingStubFull);
    final Long beforeLenderFrozenAmount = account.getFrozenV2(0).getAmount();
    final Long beforeLenderNetLimit = accountResource.getNetLimit();

    accountResource = PublicMethed
        .getAccountResource(receiveBandwidthAddress, blockingStubFull);
    final Long beforeReceiverNetLimit = accountResource.getNetLimit();
    account = PublicMethed.queryAccount(receiveBandwidthAddress,blockingStubFull);
    final Long beforeAcquiredDelegatedFrozenBalanceForBandwidth = account
        .getAcquiredDelegatedFrozenBalanceForBandwidth();


    Assert.assertTrue(PublicMethed.unDelegateResourceV2(frozenBandwidthAddress,delegateBandwidthAmount,
        0, receiveBandwidthAddress,frozenBandwidthKey,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    account = PublicMethed.queryAccount(frozenBandwidthAddress,blockingStubFull);
    accountResource = PublicMethed
        .getAccountResource(frozenBandwidthAddress, blockingStubFull);
    final Long afterLenderFrozenAmount = account.getFrozenV2(0).getAmount();
    final Long afterDelegateResourceAmount = account.getDelegatedFrozenBalanceForBandwidth();
    final Long afterLenderNetLimit = accountResource.getNetLimit();

    Assert.assertTrue(beforeLenderFrozenAmount - afterLenderFrozenAmount == -delegateBandwidthAmount);

    accountResource = PublicMethed
        .getAccountResource(receiveBandwidthAddress, blockingStubFull);
    final Long afterReceiverNetLimit = accountResource.getNetLimit();
    account = PublicMethed.queryAccount(receiveBandwidthAddress,blockingStubFull);
    final Long receiverAcquiredDelegatedFrozenBalanceForBandwidth = account
        .getAcquiredDelegatedFrozenBalanceForBandwidth();

    Assert.assertEquals(afterLenderNetLimit, beforeDelegateBandwidthNetLimit);
    Assert.assertTrue(receiverAcquiredDelegatedFrozenBalanceForBandwidth == 0);
    Assert.assertTrue(afterReceiverNetLimit == 0);
    Assert.assertTrue(afterDelegateResourceAmount == 0);
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


