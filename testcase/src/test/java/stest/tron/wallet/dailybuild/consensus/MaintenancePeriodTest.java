package stest.tron.wallet.dailybuild.consensus;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.GrpcAPI.WitnessList;
import org.tron.protos.Protocol.Block;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.TronBaseTest;

/**
 * Maintenance period tests.
 *
 * <p>Validates that the maintenance period mechanism is active and that
 * the next maintenance time is in the future relative to the latest block.
 */
@Slf4j
public class MaintenancePeriodTest extends TronBaseTest {

  @Test(enabled = true, description = "Next maintenance time should be in the future",
      groups = {"daily"})
  public void test01NextMaintenanceTimeInFuture() {
    NumberMessage nextMaintenance = blockingStubFull.getNextMaintenanceTime(
        EmptyMessage.newBuilder().build());
    long nextMaintenanceMs = nextMaintenance.getNum();
    logger.info("Next maintenance time: {}", nextMaintenanceMs);

    Block currentBlock = PublicMethod.getBlock(-1, blockingStubFull);
    long blockTimestamp = currentBlock.getBlockHeader().getRawData().getTimestamp();
    logger.info("Current block timestamp: {}", blockTimestamp);

    Assert.assertTrue(nextMaintenanceMs > 0,
        "Next maintenance time should be positive");
    Assert.assertTrue(nextMaintenanceMs >= blockTimestamp,
        "Next maintenance time should be >= current block timestamp");
  }

  @Test(enabled = true, description = "Witness list should be available and non-empty after maintenance",
      groups = {"daily"})
  public void test02WitnessListAfterMaintenance() {
    Optional<WitnessList> witnessList = PublicMethod.listWitnesses(blockingStubFull);
    Assert.assertTrue(witnessList.isPresent(), "Witness list should be available");
    int witnessCount = witnessList.get().getWitnessesCount();
    Assert.assertTrue(witnessCount > 0, "Should have at least one active witness");
    logger.info("Active witnesses after maintenance: {}", witnessCount);
  }

  @Test(enabled = true, description = "Total vote count across all witnesses should be positive",
      groups = {"daily"})
  public void test03TotalVoteCountPositive() {
    Optional<WitnessList> witnessList = PublicMethod.listWitnesses(blockingStubFull);
    Assert.assertTrue(witnessList.isPresent());
    long totalVotes = witnessList.get().getWitnessesList().stream()
        .mapToLong(w -> w.getVoteCount())
        .sum();
    logger.info("Total votes across all witnesses: {}", totalVotes);
    Assert.assertTrue(totalVotes > 0, "Total vote count should be positive");
  }
}
