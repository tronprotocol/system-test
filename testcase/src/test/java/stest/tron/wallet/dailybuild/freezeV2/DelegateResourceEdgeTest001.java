package stest.tron.wallet.dailybuild.freezeV2;

import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.protos.Protocol.Account;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.TronConstants;
import stest.tron.wallet.common.client.utils.Utils;

/**
 * TIP-157: Resource delegation edge case tests.
 *
 * <p>Tests boundary conditions for delegating bandwidth/energy resources
 * including zero amounts, self-delegation, and insufficient balance.
 */
@Slf4j
public class DelegateResourceEdgeTest001 extends TronBaseTest {

  private ECKey ownerKey = new ECKey(Utils.getRandom());
  private byte[] ownerAddress = ownerKey.getAddress();
  private String ownerKeyStr = ByteArray.toHexString(ownerKey.getPrivKeyBytes());

  private ECKey receiverKey = new ECKey(Utils.getRandom());
  private byte[] receiverAddress = receiverKey.getAddress();
  private String receiverKeyStr = ByteArray.toHexString(receiverKey.getPrivKeyBytes());

  @BeforeClass(enabled = true)
  public void beforeClass() {
    if (!PublicMethod.freezeV2ProposalIsOpen(blockingStubFull)) {
      throw new SkipException("Skipping FreezeV2 delegation test - proposal not open");
    }

    Assert.assertTrue(PublicMethod.sendcoin(ownerAddress, TronConstants.TEN_THOUSAND_TRX,
        foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.sendcoin(receiverAddress, TronConstants.TEN_TRX,
        foundationAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    // Freeze bandwidth for delegation
    Assert.assertTrue(PublicMethod.freezeBalanceV2(ownerAddress, TronConstants.THOUSAND_TRX,
        TronConstants.FREEZE_BANDWIDTH, ownerKeyStr, blockingStubFull));
    // Freeze energy for delegation
    Assert.assertTrue(PublicMethod.freezeBalanceV2(ownerAddress, TronConstants.THOUSAND_TRX,
        TronConstants.FREEZE_ENERGY, ownerKeyStr, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "Delegate bandwidth resource to another account",
      groups = {"daily", "staking"})
  public void test01DelegateBandwidth() {
    long delegateAmount = TronConstants.HUNDRED_TRX;
    Assert.assertTrue(PublicMethod.delegateResourceV2(ownerAddress, delegateAmount,
        TronConstants.FREEZE_BANDWIDTH, receiverAddress, ownerKeyStr, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account receiverAccount = PublicMethod.queryAccount(receiverAddress, blockingStubFull);
    Assert.assertTrue(receiverAccount.getAcquiredDelegatedFrozenV2BalanceForBandwidth() > 0,
        "Receiver should have acquired delegated bandwidth");
    logger.info("Receiver acquired bandwidth: {}",
        receiverAccount.getAcquiredDelegatedFrozenV2BalanceForBandwidth());
  }

  @Test(enabled = true, description = "Delegate energy resource to another account",
      groups = {"daily", "staking"})
  public void test02DelegateEnergy() {
    long delegateAmount = TronConstants.HUNDRED_TRX;
    Assert.assertTrue(PublicMethod.delegateResourceV2(ownerAddress, delegateAmount,
        TronConstants.FREEZE_ENERGY, receiverAddress, ownerKeyStr, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account receiverAccount = PublicMethod.queryAccount(receiverAddress, blockingStubFull);
    Assert.assertTrue(receiverAccount.getAccountResource().getAcquiredDelegatedFrozenV2BalanceForEnergy() > 0,
        "Receiver should have acquired delegated energy");
  }

  @Test(enabled = true, description = "Delegate zero amount should fail",
      groups = {"daily", "staking"})
  public void test03DelegateZeroAmount() {
    Assert.assertFalse(PublicMethod.delegateResourceV2(ownerAddress, 0,
        TronConstants.FREEZE_BANDWIDTH, receiverAddress, ownerKeyStr, blockingStubFull),
        "Delegating zero amount should fail");
  }

  @Test(enabled = true, description = "Delegate to self should fail",
      groups = {"daily", "staking"})
  public void test04DelegateToSelf() {
    Assert.assertFalse(PublicMethod.delegateResourceV2(ownerAddress, TronConstants.ONE_TRX,
        TronConstants.FREEZE_BANDWIDTH, ownerAddress, ownerKeyStr, blockingStubFull),
        "Delegating to self should fail");
  }

  @Test(enabled = true, description = "Delegate more than frozen should fail",
      groups = {"daily", "staking"})
  public void test05DelegateExceedingFrozen() {
    long excessiveAmount = TronConstants.TEN_THOUSAND_TRX;
    Assert.assertFalse(PublicMethod.delegateResourceV2(ownerAddress, excessiveAmount,
        TronConstants.FREEZE_BANDWIDTH, receiverAddress, ownerKeyStr, blockingStubFull),
        "Delegating more than frozen amount should fail");
  }

  @Test(enabled = true, description = "Undelegate resource",
      groups = {"daily", "staking"})
  public void test06UndelegateResource() {
    long undelegateAmount = TronConstants.HUNDRED_TRX;
    Assert.assertTrue(PublicMethod.unDelegateResourceV2(ownerAddress, undelegateAmount,
        TronConstants.FREEZE_BANDWIDTH, receiverAddress, ownerKeyStr, blockingStubFull),
        "Undelegating should succeed");
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @AfterClass(enabled = true)
  public void afterClass() {
    PublicMethod.freeResource(ownerAddress, ownerKeyStr, foundationAddress, blockingStubFull);
    PublicMethod.freeResource(receiverAddress, receiverKeyStr, foundationAddress, blockingStubFull);
  }
}
