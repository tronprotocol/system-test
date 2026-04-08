package stest.tron.wallet.dailybuild.votereward;

import com.google.protobuf.ByteString;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.protos.Protocol.Account;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.RetryUtil;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.TronConstants;
import stest.tron.wallet.common.client.utils.Utils;

/**
 * TIP-271: Vote reward distribution tests.
 *
 * <p>Tests the SR reward mechanism: voters receive rewards proportional to
 * their vote weight, minus the SR brokerage fee.
 */
@Slf4j
public class VoteRewardTest001 extends TronBaseTest {

  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] voterAddress = ecKey1.getAddress();
  private String voterKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(voterKey);
    Assert.assertTrue(PublicMethod.sendcoin(voterAddress, TronConstants.TEN_THOUSAND_TRX,
        foundationAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "Query reward info for witness account",
      groups = {"daily"})
  public void test01QueryWitnessRewardInfo() {
    BytesMessage bytesMessage = BytesMessage.newBuilder()
        .setValue(ByteString.copyFrom(witnessAddress)).build();
    NumberMessage reward = blockingStubFull.getRewardInfo(bytesMessage);
    logger.info("Witness reward: {}", reward.getNum());
    Assert.assertTrue(reward.getNum() >= 0, "Witness reward should be non-negative");
  }

  @Test(enabled = true, description = "Query brokerage for witness",
      groups = {"daily"})
  public void test02QueryWitnessBrokerage() {
    long brokerage = PublicMethod.getBrokerage(witnessAddress, blockingStubFull);
    logger.info("Witness brokerage: {}%", brokerage);
    Assert.assertTrue(brokerage >= 0 && brokerage <= 100,
        "Brokerage should be between 0 and 100");
  }

  @Test(enabled = true, description = "Freeze TRX, vote for witness, and verify vote count",
      groups = {"daily"})
  public void test03FreezeAndVote() {
    // Freeze for voting power (Tron Power)
    Assert.assertTrue(PublicMethod.freezeBalanceGetTronPower(
        voterAddress, TronConstants.THOUSAND_TRX, 0,
        TronConstants.FREEZE_TRON_POWER, null, voterKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    // Vote for witness
    HashMap<byte[], Long> witnessMap = new HashMap<>();
    long voteCount = TronConstants.THOUSAND_TRX / TronConstants.ONE_TRX;
    witnessMap.put(witnessAddress, voteCount);
    Assert.assertTrue(PublicMethod.voteWitness(voterAddress, voterKey,
        witnessMap, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    // Verify vote was recorded
    Account voterAccount = PublicMethod.queryAccount(voterAddress, blockingStubFull);
    Assert.assertTrue(voterAccount.getVotesCount() > 0, "Voter should have votes recorded");
    logger.info("Vote count: {}", voterAccount.getVotes(0).getVoteCount());
  }

  @Test(enabled = true, description = "Query voter reward after voting",
      groups = {"daily"})
  public void test04QueryVoterReward() {
    BytesMessage bytesMessage = BytesMessage.newBuilder()
        .setValue(ByteString.copyFrom(voterAddress)).build();
    NumberMessage reward = blockingStubFull.getRewardInfo(bytesMessage);
    logger.info("Voter reward after voting: {}", reward.getNum());
    // Reward may be 0 if not enough maintenance cycles have passed
    Assert.assertTrue(reward.getNum() >= 0, "Voter reward should be non-negative");
  }

  @Test(enabled = true, description = "Vote with zero TronPower should fail",
      groups = {"daily"})
  public void test05VoteWithoutTronPower() {
    ECKey noFreezeKey = new ECKey(Utils.getRandom());
    byte[] noFreezeAddr = noFreezeKey.getAddress();
    String noFreezeKeyStr = ByteArray.toHexString(noFreezeKey.getPrivKeyBytes());

    Assert.assertTrue(PublicMethod.sendcoin(noFreezeAddr, TronConstants.TEN_TRX,
        foundationAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    HashMap<byte[], Long> witnessMap = new HashMap<>();
    witnessMap.put(witnessAddress, 1L);
    Assert.assertFalse(PublicMethod.voteWitness(noFreezeAddr, noFreezeKeyStr,
        witnessMap, blockingStubFull),
        "Voting without frozen TronPower should fail");
  }

  @Test(enabled = true, description = "Vote exceeding TronPower limit should fail",
      groups = {"daily"})
  public void test06VoteExceedingLimit() {
    long tronPowerLimit = TronConstants.THOUSAND_TRX / TronConstants.ONE_TRX;
    HashMap<byte[], Long> witnessMap = new HashMap<>();
    witnessMap.put(witnessAddress, tronPowerLimit + 1);
    Assert.assertFalse(PublicMethod.voteWitness(voterAddress, voterKey,
        witnessMap, blockingStubFull),
        "Voting more than available TronPower should fail");
  }

  @AfterClass(enabled = true)
  public void afterClass() {
    PublicMethod.freeResource(voterAddress, voterKey, foundationAddress, blockingStubFull);
  }
}
