package stest.tron.wallet.dailybuild.separateExecution;

import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class FreezeWeightDeflationV2Test extends TronBaseTest {
  private static final long sendAmount = 1000000000L;
  private static final long frozenAmount = 1500000L;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] frozen1Address = ecKey1.getAddress();
  String frozen1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey4 = new ECKey(Utils.getRandom());
  byte[] frozen2Address = ecKey4.getAddress();
  String frozen2Key = ByteArray.toHexString(ecKey4.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] receiver1Address = ecKey2.getAddress();
  String receiver1Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] receiver2Address = ecKey3.getAddress();
  String receiver2Key = ByteArray.toHexString(ecKey3.getPrivKeyBytes());

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() throws Exception {
    PublicMethod.printAddress(frozen1Key);
    PublicMethod.printAddress(frozen2Key);
    PublicMethod.printAddress(receiver1Key);
    PublicMethod.printAddress(receiver2Key);    if (!PublicMethod.freezeV2ProposalIsOpen(blockingStubFull)) {      throw new SkipException("Skipping freezeV2 test case");
    }


    Assert.assertTrue(PublicMethod.sendcoin(frozen1Address, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.sendcoin(frozen2Address, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.sendcoin(receiver1Address, 1L,
        foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.sendcoin(receiver2Address, 1L,
        foundationAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethod.freezeBalanceV2(foundationAddress, frozenAmount * 30, 0,
        foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.freezeBalanceV2(foundationAddress, frozenAmount * 30, 1,
        foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(PublicMethod.delegateResourceV2(foundationAddress, frozenAmount * 8,
        0, receiver1Address, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.delegateResourceV2(foundationAddress, frozenAmount * 8,
        1, receiver1Address, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.delegateResourceV2(foundationAddress, frozenAmount * 8,
        0, receiver2Address, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.delegateResourceV2(foundationAddress, frozenAmount * 8,
        1, receiver2Address, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, priority=3, description = "Freeze net cause weight deflation issue fix test", groups = {"daily", "serial"})
  public void test01FreezeNetCauseWeightDeflationTest() {
    final Long beforeNetWeight = PublicMethod.getAccountResource(frozen1Address, blockingStubFull)
        .getTotalNetWeight();
  final Long beforeEnergyWeight =
        PublicMethod.getAccountResource(frozen1Address, blockingStubFull)
        .getTotalEnergyWeight();
    logger.info("before:" + beforeNetWeight);

    Assert.assertTrue(PublicMethod.freezeBalance(frozen1Address,
        frozenAmount, 0, frozen1Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long afterNetWeight = PublicMethod.getAccountResource(frozen1Address, blockingStubFull)
        .getTotalNetWeight();
    Assert.assertTrue(afterNetWeight - beforeNetWeight == 1);


    Assert.assertTrue(PublicMethod.freezeBalance(frozen1Address,
        frozenAmount, 0, frozen1Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    afterNetWeight = PublicMethod.getAccountResource(frozen1Address, blockingStubFull)
        .getTotalNetWeight();
  Long afterEnergyWeight = PublicMethod.getAccountResource(frozen1Address, blockingStubFull)
        .getTotalEnergyWeight();
    Assert.assertTrue(afterNetWeight - beforeNetWeight == 3);
    Assert.assertTrue(afterEnergyWeight - beforeEnergyWeight == 0);


    Assert.assertTrue(PublicMethod.unFreezeBalanceV2(frozen1Address, frozen1Key,
        frozenAmount * 2, 0, blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long unfreezeTimestamp = PublicMethod.queryAccount(frozen1Address, blockingStubFull)
        .getUnfrozenV2(0).getUnfreezeExpireTime();
  int retryTime = 0;
    while (System.currentTimeMillis() < unfreezeTimestamp && retryTime++ < 100) {
      PublicMethod.waitProduceNextBlock(blockingStubFull);
    }

    Assert.assertTrue(
        PublicMethod.withdrawExpireUnfreeze(frozen1Address, frozen1Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);


    afterNetWeight = PublicMethod.getAccountResource(frozen1Address, blockingStubFull)
        .getTotalNetWeight();
    logger.info("afterNetWeight:" + afterNetWeight);
    afterEnergyWeight = PublicMethod.getAccountResource(frozen1Address, blockingStubFull)
        .getTotalEnergyWeight();
    Assert.assertTrue(afterEnergyWeight - beforeEnergyWeight == 0);
    Assert.assertTrue(afterNetWeight - beforeNetWeight == 0);


  }


  @Test(enabled = true, priority=3 ,description = "Freeze energy cause weight deflation issue fix test", groups = {"daily", "serial"})
  public void test02FreezeEnergyCauseWeightDeflationTest() {
    final Long beforeEnergyWeight =
        PublicMethod.getAccountResource(frozen1Address, blockingStubFull)
        .getTotalEnergyWeight();
  final Long beforeNetWeight = PublicMethod.getAccountResource(frozen1Address, blockingStubFull)
        .getTotalNetWeight();
    Assert.assertTrue(PublicMethod.freezeBalanceGetEnergy(frozen1Address,
        frozenAmount, 0, 1, frozen1Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long afterEnergyWeight = PublicMethod.getAccountResource(frozen1Address, blockingStubFull)
        .getTotalEnergyWeight();
    Assert.assertTrue(afterEnergyWeight - beforeEnergyWeight == 1);
  Long afterNetWeight = PublicMethod.getAccountResource(frozen1Address, blockingStubFull)
        .getTotalNetWeight();
    Assert.assertTrue(afterNetWeight - beforeNetWeight == 0);


    Assert.assertTrue(PublicMethod.freezeBalanceGetEnergy(frozen1Address,
        frozenAmount,  0, 1, frozen1Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);


    afterEnergyWeight = PublicMethod.getAccountResource(frozen1Address, blockingStubFull)
        .getTotalEnergyWeight();
    Assert.assertTrue(afterEnergyWeight - beforeEnergyWeight == 3);
    afterNetWeight = PublicMethod.getAccountResource(frozen1Address, blockingStubFull)
        .getTotalNetWeight();
    Assert.assertTrue(afterNetWeight - beforeNetWeight == 0);


    Assert.assertTrue(PublicMethod.unFreezeBalanceV2(frozen1Address,
        frozen1Key, frozenAmount * 2, 1, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long unfreezeTimestamp = PublicMethod.queryAccount(frozen1Address, blockingStubFull)
        .getUnfrozenV2(0).getUnfreezeExpireTime();
  int retryTime = 0;
    while (System.currentTimeMillis() < unfreezeTimestamp && retryTime++ < 100) {
      PublicMethod.waitProduceNextBlock(blockingStubFull);
    }

    Assert.assertTrue(
        PublicMethod.withdrawExpireUnfreeze(frozen1Address, frozen1Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);


    afterEnergyWeight = PublicMethod.getAccountResource(frozen1Address, blockingStubFull)
        .getTotalEnergyWeight();
    Assert.assertTrue(afterEnergyWeight - beforeEnergyWeight == 0);
    afterNetWeight = PublicMethod.getAccountResource(frozen1Address, blockingStubFull)
        .getTotalNetWeight();
    Assert.assertTrue(afterNetWeight - beforeNetWeight == 0);
  }


  /**
   * constructor.
   */

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(frozen1Address, frozen1Key, foundationAddress, blockingStubFull);
    PublicMethod.freeResource(frozen2Address, frozen2Key, foundationAddress, blockingStubFull);  }
}


