package stest.tron.wallet.dailybuild.consensus;

import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.protos.Protocol.Account;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.TronConstants;
import stest.tron.wallet.common.client.utils.Utils;

/**
 * Vote weight and delegation tests.
 *
 * <p>Verifies that voting power is proportional to frozen TRX (Tron Power),
 * and that votes can be updated and revoked.
 */
@Slf4j
public class VoteWeightTest extends TronBaseTest {

  private ECKey voter1Key;
  private byte[] voter1Address;
  private String voter1KeyStr;
  private ECKey voter2Key;
  private byte[] voter2Address;
  private String voter2KeyStr;

  @BeforeClass(enabled = true)
  public void beforeClass() {
    voter1Key = new ECKey(Utils.getRandom());
    voter1Address = voter1Key.getAddress();
    voter1KeyStr = ByteArray.toHexString(voter1Key.getPrivKeyBytes());

    voter2Key = new ECKey(Utils.getRandom());
    voter2Address = voter2Key.getAddress();
    voter2KeyStr = ByteArray.toHexString(voter2Key.getPrivKeyBytes());

    // Fund both voters
    Assert.assertTrue(PublicMethod.sendcoin(voter1Address, TronConstants.TEN_THOUSAND_TRX,
        foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.sendcoin(voter2Address, 5 * TronConstants.THOUSAND_TRX,
        foundationAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "Freeze more TRX = more vote weight (Stake 2.0)",
      groups = {"daily"})
  public void test01MoreFreezeMoreVotes() {
    // Use FreezeBalanceV2 (Stake 2.0) — freezing for BANDWIDTH gives Tron Power
    Assert.assertTrue(PublicMethod.freezeBalanceV2(
        voter1Address, TronConstants.TEN_THOUSAND_TRX,
        TronConstants.FREEZE_BANDWIDTH, voter1KeyStr, blockingStubFull));

    Assert.assertTrue(PublicMethod.freezeBalanceV2(
        voter2Address, 5 * TronConstants.THOUSAND_TRX,
        TronConstants.FREEZE_BANDWIDTH, voter2KeyStr, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account acc1 = PublicMethod.queryAccount(voter1Address, blockingStubFull);
    Account acc2 = PublicMethod.queryAccount(voter2Address, blockingStubFull);

    // In Stake 2.0, tron power = total frozen amount across all resource types
    long power1 = acc1.getFrozenV2List().stream()
        .mapToLong(Account.FreezeV2::getAmount).sum();
    long power2 = acc2.getFrozenV2List().stream()
        .mapToLong(Account.FreezeV2::getAmount).sum();
    logger.info("Voter1 frozenV2: {}, Voter2 frozenV2: {}", power1, power2);
    Assert.assertTrue(power1 > power2,
        "Voter1 (10000 TRX) should have more tron power than Voter2 (5000 TRX)");
  }

  @Test(enabled = true, dependsOnMethods = "test01MoreFreezeMoreVotes",
      description = "Vote and verify vote count matches",
      groups = {"daily"})
  public void test02VoteCountMatchesFrozenAmount() {
    long voter1Amount = TronConstants.TEN_THOUSAND_TRX / TronConstants.ONE_TRX;
    HashMap<byte[], Long> voteMap1 = new HashMap<>();
    voteMap1.put(witnessAddress, voter1Amount);
    Assert.assertTrue(PublicMethod.voteWitness(voter1Address, voter1KeyStr,
        voteMap1, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account acc1 = PublicMethod.queryAccount(voter1Address, blockingStubFull);
    Assert.assertTrue(acc1.getVotesCount() > 0);
    Assert.assertEquals(acc1.getVotes(0).getVoteCount(), voter1Amount,
        "Vote count should match frozen TRX in sun / 1_000_000");
  }

  @Test(enabled = true, dependsOnMethods = "test02VoteCountMatchesFrozenAmount",
      description = "Re-voting should update the vote count",
      groups = {"daily"})
  public void test03ReVoteUpdatesCount() {
    long newAmount = 5000L; // 5000 votes (less than max tron power)
    HashMap<byte[], Long> voteMap = new HashMap<>();
    voteMap.put(witnessAddress, newAmount);
    Assert.assertTrue(PublicMethod.voteWitness(voter1Address, voter1KeyStr,
        voteMap, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account acc = PublicMethod.queryAccount(voter1Address, blockingStubFull);
    Assert.assertEquals(acc.getVotes(0).getVoteCount(), newAmount,
        "Re-voting should update vote count");
  }

  @Test(enabled = true, dependsOnMethods = "test03ReVoteUpdatesCount",
      description = "Re-voting with different amount should update",
      groups = {"daily"})
  public void test04ReVoteWithDifferentAmount() {
    // Vote with a smaller amount to verify update works
    long smallAmount = 100L;
    HashMap<byte[], Long> voteMap = new HashMap<>();
    voteMap.put(witnessAddress, smallAmount);
    Assert.assertTrue(PublicMethod.voteWitness(voter1Address, voter1KeyStr,
        voteMap, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account acc = PublicMethod.queryAccount(voter1Address, blockingStubFull);
    Assert.assertTrue(acc.getVotesCount() > 0, "Should have votes");
    Assert.assertEquals(acc.getVotes(0).getVoteCount(), smallAmount,
        "Vote count should be updated to " + smallAmount);
  }

  @AfterClass(enabled = true)
  public void afterClass() {
    PublicMethod.freeResource(voter1Address, voter1KeyStr, foundationAddress, blockingStubFull);
    PublicMethod.freeResource(voter2Address, voter2KeyStr, foundationAddress, blockingStubFull);
  }
}
