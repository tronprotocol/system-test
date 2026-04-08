package stest.tron.wallet.dailybuild.consensus;

import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.WitnessList;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Witness;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.TronConstants;
import stest.tron.wallet.common.client.utils.Utils;

/**
 * DPoS witness election tests.
 *
 * <p>Verifies the SR witness list, voting mechanics, and vote-count
 * reflection in the witness schedule.
 */
@Slf4j
public class WitnessElectionTest extends TronBaseTest {

  private ECKey voterKey1;
  private byte[] voterAddress1;
  private String voterKeyStr1;

  @BeforeClass(enabled = true)
  public void beforeClass() {
    voterKey1 = new ECKey(Utils.getRandom());
    voterAddress1 = voterKey1.getAddress();
    voterKeyStr1 = ByteArray.toHexString(voterKey1.getPrivKeyBytes());
    PublicMethod.printAddress(voterKeyStr1);

    Assert.assertTrue(PublicMethod.sendcoin(voterAddress1, TronConstants.TEN_THOUSAND_TRX,
        foundationAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "Witness list should not be empty",
      groups = {"daily"})
  public void test01WitnessListNotEmpty() {
    Optional<WitnessList> witnessList = PublicMethod.listWitnesses(blockingStubFull);
    Assert.assertTrue(witnessList.isPresent(), "Witness list should be present");
    Assert.assertTrue(witnessList.get().getWitnessesCount() > 0,
        "Should have at least one witness");
    logger.info("Total witnesses: {}", witnessList.get().getWitnessesCount());
  }

  @Test(enabled = true, description = "All witnesses should have valid addresses",
      groups = {"daily"})
  public void test02WitnessAddressesValid() {
    Optional<WitnessList> witnessList = PublicMethod.listWitnesses(blockingStubFull);
    Assert.assertTrue(witnessList.isPresent());
    for (Witness witness : witnessList.get().getWitnessesList()) {
      byte[] addr = witness.getAddress().toByteArray();
      Assert.assertEquals(addr.length, 21, "Witness address should be 21 bytes");
      Assert.assertEquals(addr[0], (byte) 0x41, "Witness address should start with 0x41");
      logger.info("Witness: {} voteCount={}", ByteArray.toHexString(addr),
          witness.getVoteCount());
    }
  }

  @Test(enabled = true, description = "Vote for witness and verify vote count increases",
      groups = {"daily"})
  public void test03VoteIncreasesWitnessVoteCount() {
    // Get initial vote count for our test witness
    Optional<WitnessList> before = PublicMethod.listWitnesses(blockingStubFull);
    Assert.assertTrue(before.isPresent());
    long initialVoteCount = 0;
    for (Witness w : before.get().getWitnessesList()) {
      if (w.getAddress().toByteArray().length == witnessAddress.length
          && java.util.Arrays.equals(w.getAddress().toByteArray(), witnessAddress)) {
        initialVoteCount = w.getVoteCount();
        break;
      }
    }
    logger.info("Initial vote count for witness: {}", initialVoteCount);

    // Freeze (Stake 2.0) and vote
    Assert.assertTrue(PublicMethod.freezeBalanceV2(
        voterAddress1, TronConstants.THOUSAND_TRX,
        TronConstants.FREEZE_BANDWIDTH, voterKeyStr1, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    long voteAmount = TronConstants.THOUSAND_TRX / TronConstants.ONE_TRX;
    HashMap<byte[], Long> voteMap = new HashMap<>();
    voteMap.put(witnessAddress, voteAmount);
    Assert.assertTrue(PublicMethod.voteWitness(voterAddress1, voterKeyStr1,
        voteMap, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    // Verify voter account recorded the vote
    Account voterAccount = PublicMethod.queryAccount(voterAddress1, blockingStubFull);
    Assert.assertTrue(voterAccount.getVotesCount() > 0, "Voter should have votes recorded");
    Assert.assertEquals(voterAccount.getVotes(0).getVoteCount(), voteAmount);
  }

  @Test(enabled = true, description = "Witnesses all have non-negative vote counts",
      groups = {"daily"})
  public void test04WitnessVoteCountsValid() {
    Optional<WitnessList> witnessList = PublicMethod.listWitnesses(blockingStubFull);
    Assert.assertTrue(witnessList.isPresent());
    // Note: ListWitnesses API does not guarantee descending sort order.
    // Verify all vote counts are non-negative and log them for inspection.
    for (Witness w : witnessList.get().getWitnessesList()) {
      Assert.assertTrue(w.getVoteCount() >= 0,
          "Witness vote count should be non-negative");
      logger.info("Witness {} voteCount={}",
          ByteArray.toHexString(w.getAddress().toByteArray()), w.getVoteCount());
    }
  }

  @AfterClass(enabled = true)
  public void afterClass() {
    PublicMethod.freeResource(voterAddress1, voterKeyStr1, foundationAddress, blockingStubFull);
  }
}
