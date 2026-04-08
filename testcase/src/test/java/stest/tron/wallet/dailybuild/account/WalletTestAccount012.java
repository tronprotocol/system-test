package stest.tron.wallet.dailybuild.account;

import com.google.protobuf.ByteString;
import java.util.HashMap;
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
import stest.tron.wallet.common.client.utils.Utils;

@Slf4j
public class WalletTestAccount012 extends TronBaseTest {
  private static final long sendAmount = 10000000000L;
  private static final long frozenAmountForTronPower = 3456789L;
  private static final long frozenAmountForNet = 7000000L;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] frozenAddress = ecKey1.getAddress();
  String frozenKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() throws Exception{
    PublicMethod.printAddress(frozenKey);

    if(!PublicMethod.tronPowerProposalIsOpen(blockingStubFull) || !PublicMethod.freezeV2ProposalIsOpen(blockingStubFull)) {
      throw new SkipException("Skipping tronPower or freezeV2 test case");
    }


  }

  @Test(enabled = true, priority = 100,description = "Freeze balance to get tron power", groups = {"daily"})
  public void test01FreezeBalanceGetTronPower() {


    final Long beforeFrozenTime = System.currentTimeMillis();
    Assert.assertTrue(PublicMethod.sendcoin(frozenAddress, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);


    AccountResourceMessage accountResource = PublicMethod
        .getAccountResource(frozenAddress, blockingStubFull);
  final Long beforeTotalTronPowerWeight = accountResource.getTotalTronPowerWeight();
  final Long beforeTronPowerLimit = accountResource.getTronPowerLimit();


    Assert.assertTrue(PublicMethod.freezeBalanceGetTronPower(frozenAddress,frozenAmountForTronPower,
        0,2,null,frozenKey,blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long afterFrozenTime = System.currentTimeMillis();
    Account account = PublicMethod.queryAccount(frozenAddress,blockingStubFull);
    Assert.assertEquals(PublicMethod.freezeV2ProposalIsOpen(blockingStubFull)
  ? account.getFrozenV2(2).getAmount() : account.getTronPower().getFrozenBalance(),frozenAmountForTronPower);
    Assert.assertTrue(PublicMethod.freezeV2ProposalIsOpen(blockingStubFull) ? true : account.getTronPower().getExpireTime() > beforeFrozenTime
        && account.getTronPower().getExpireTime() < afterFrozenTime);
    accountResource = PublicMethod
        .getAccountResource(frozenAddress, blockingStubFull);
  Long afterTotalTronPowerWeight = accountResource.getTotalTronPowerWeight();
  Long afterTronPowerLimit = accountResource.getTronPowerLimit();
  Long afterTronPowerUsed = accountResource.getTronPowerUsed();
    Assert.assertEquals(afterTotalTronPowerWeight - beforeTotalTronPowerWeight,
        frozenAmountForTronPower / 1000000L);

    Assert.assertEquals(afterTronPowerLimit - beforeTronPowerLimit,
        frozenAmountForTronPower / 1000000L);


    Assert.assertTrue(PublicMethod.freezeBalanceGetTronPower(frozenAddress,
        6000000 - frozenAmountForTronPower,
        0,2,null,frozenKey,blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    accountResource = PublicMethod
        .getAccountResource(frozenAddress, blockingStubFull);
    afterTronPowerLimit = accountResource.getTronPowerLimit();

    Assert.assertEquals(afterTronPowerLimit - beforeTronPowerLimit,
        6);


  }


  @Test(enabled = true,priority = 100,description = "Vote witness by tron power", groups = {"daily"})
  public void test02VotePowerOnlyComeFromTronPower() {
    AccountResourceMessage accountResource = PublicMethod
        .getAccountResource(frozenAddress, blockingStubFull);
  final Long beforeTronPowerUsed = accountResource.getTronPowerUsed();


    HashMap<byte[],Long> witnessMap = new HashMap<>();
    witnessMap.put(witnessAddress,frozenAmountForNet / 1000000L);
    Assert.assertFalse(PublicMethod.voteWitness(frozenAddress,frozenKey,witnessMap,
        blockingStubFull));
    witnessMap.put(witnessAddress,frozenAmountForTronPower / 1000000L);
    Assert.assertTrue(PublicMethod.voteWitness(frozenAddress,frozenKey,witnessMap,
        blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    accountResource = PublicMethod
        .getAccountResource(frozenAddress, blockingStubFull);
  Long afterTronPowerUsed = accountResource.getTronPowerUsed();
    Assert.assertEquals(afterTronPowerUsed - beforeTronPowerUsed,
        frozenAmountForTronPower / 1000000L);
  final Long secondBeforeTronPowerUsed = afterTronPowerUsed;
    witnessMap.put(witnessAddress,(frozenAmountForTronPower / 1000000L) - 1);
    Assert.assertTrue(PublicMethod.voteWitness(frozenAddress,frozenKey,witnessMap,
        blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    accountResource = PublicMethod
        .getAccountResource(frozenAddress, blockingStubFull);
    afterTronPowerUsed = accountResource.getTronPowerUsed();
    Assert.assertEquals(secondBeforeTronPowerUsed - afterTronPowerUsed,
        1);


  }

  @Test(enabled = true,priority = 100,description = "Tron power is not allow to others", groups = {"daily"})
  public void test03TronPowerIsNotAllowToOthers() throws Exception{
    Assert.assertFalse(PublicMethod.freezeBalanceGetTronPower(frozenAddress,
        frozenAmountForTronPower, 0,2,
        ByteString.copyFrom(foundationAddress),frozenKey,blockingStubFull));
  }


  @Test(enabled = true,priority = 100,description = "Unfreeze balance for tron power", groups = {"daily"})
  public void test04UnfreezeBalanceForTronPower() {
    AccountResourceMessage accountResource = PublicMethod
        .getAccountResource(foundationAddress, blockingStubFull);
  final Long beforeTotalTronPowerWeight = accountResource.getTotalTronPowerWeight();
  Long canUnfreezeAmount = PublicMethod.getFrozenV2Amount(frozenAddress,2,blockingStubFull);
    Assert.assertTrue(PublicMethod.unFreezeBalanceV2(frozenAddress,frozenKey,canUnfreezeAmount,2,blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    accountResource = PublicMethod
        .getAccountResource(frozenAddress, blockingStubFull);
  Long afterTotalTronPowerWeight = accountResource.getTotalTronPowerWeight();
    Assert.assertTrue(beforeTotalTronPowerWeight - afterTotalTronPowerWeight == canUnfreezeAmount / 1000000);

    Assert.assertEquals(accountResource.getTronPowerLimit(),0L);
    Assert.assertEquals(accountResource.getTronPowerUsed(),0L);

    Account account = PublicMethod.queryAccount(frozenAddress,blockingStubFull);
    Assert.assertTrue(PublicMethod.getFrozenV2Amount(frozenAddress,2,blockingStubFull) == 0);


  }
  

  /**
   * constructor.
   */

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    PublicMethod.unFreezeBalance(frozenAddress, frozenKey, 2, null,
        blockingStubFull);
    PublicMethod.unFreezeBalance(frozenAddress, frozenKey, 0, null,
        blockingStubFull);
    PublicMethod.freeResource(frozenAddress, frozenKey, foundationAddress, blockingStubFull);
  }
}


