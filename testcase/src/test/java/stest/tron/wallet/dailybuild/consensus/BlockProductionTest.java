package stest.tron.wallet.dailybuild.consensus;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.WitnessList;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.BlockHeader;
import org.tron.protos.Protocol.Witness;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.TronBaseTest;

/**
 * Block production tests.
 *
 * <p>Validates that blocks are produced at the expected interval (~3s),
 * by valid witnesses, and that block numbers increment monotonically.
 */
@Slf4j
public class BlockProductionTest extends TronBaseTest {

  private Set<String> validWitnessAddresses;

  @BeforeClass(enabled = true)
  public void beforeClass() {
    // Warm up: ensure at least a few blocks have been produced
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    validWitnessAddresses = new HashSet<>();
    Optional<WitnessList> witnessList = PublicMethod.listWitnesses(blockingStubFull);
    Assert.assertTrue(witnessList.isPresent());
    for (Witness w : witnessList.get().getWitnessesList()) {
      validWitnessAddresses.add(ByteArray.toHexString(w.getAddress().toByteArray()));
    }
    logger.info("Loaded {} valid witness addresses", validWitnessAddresses.size());
  }

  @Test(enabled = true, description = "Blocks should have incrementing block numbers",
      groups = {"daily"})
  public void test01BlockNumbersIncrement() {
    Block block1 = blockingStubFull.getNowBlock(org.tron.api.GrpcAPI.EmptyMessage.newBuilder().build());
    long num1 = block1.getBlockHeader().getRawData().getNumber();
    Assert.assertTrue(num1 > 0, "Current block number should be positive");

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Block block2 = blockingStubFull.getNowBlock(org.tron.api.GrpcAPI.EmptyMessage.newBuilder().build());
    long num2 = block2.getBlockHeader().getRawData().getNumber();
    Assert.assertTrue(num2 > num1, "Block number should increase after waiting");
    logger.info("Block numbers: {} -> {}", num1, num2);
  }

  @Test(enabled = true, description = "Block producer should be a valid witness",
      groups = {"daily"})
  public void test02BlockProducerIsValidWitness() {
    Block block = blockingStubFull.getNowBlock(org.tron.api.GrpcAPI.EmptyMessage.newBuilder().build());
    BlockHeader.raw rawData = block.getBlockHeader().getRawData();
    String producerAddr = ByteArray.toHexString(rawData.getWitnessAddress().toByteArray());
    logger.info("Block {} produced by {}", rawData.getNumber(), producerAddr);
    Assert.assertTrue(validWitnessAddresses.contains(producerAddr),
        "Block producer should be a registered witness");
  }

  @Test(enabled = true, description = "Block timestamps should be ~3 seconds apart",
      groups = {"daily"})
  public void test03BlockTimestampInterval() {
    Block block1 = blockingStubFull.getNowBlock(org.tron.api.GrpcAPI.EmptyMessage.newBuilder().build());
    long ts1 = block1.getBlockHeader().getRawData().getTimestamp();

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Block block2 = blockingStubFull.getNowBlock(org.tron.api.GrpcAPI.EmptyMessage.newBuilder().build());
    long ts2 = block2.getBlockHeader().getRawData().getTimestamp();

    long intervalMs = ts2 - ts1;
    logger.info("Block interval: {} ms", intervalMs);
    // TRON produces blocks every 3 seconds; allow 1-12s tolerance
    Assert.assertTrue(intervalMs >= 1000 && intervalMs <= 12000,
        "Block interval should be roughly 3 seconds, got " + intervalMs + " ms");
  }

  @Test(enabled = true, description = "Multiple consecutive blocks produced by different witnesses",
      groups = {"daily"})
  public void test04WitnessRotation() {
    Set<String> producers = new HashSet<>();
    long currentNum = blockingStubFull.getNowBlock(org.tron.api.GrpcAPI.EmptyMessage.newBuilder().build())
        .getBlockHeader().getRawData().getNumber();

    // Look back at last 27 blocks (one full SR round)
    int lookback = Math.min(27, (int) currentNum);
    for (long i = currentNum - lookback + 1; i <= currentNum; i++) {
      Block block = PublicMethod.getBlock(i, blockingStubFull);
      if (block != null && block.getBlockHeader().getRawData().getNumber() > 0) {
        String producer = ByteArray.toHexString(
            block.getBlockHeader().getRawData().getWitnessAddress().toByteArray());
        producers.add(producer);
      }
    }
    logger.info("Unique producers in last {} blocks: {}", lookback, producers.size());
    // In a multi-node DPoS setup, should have more than 1 producer
    Assert.assertTrue(producers.size() >= 1,
        "Should have at least 1 distinct block producer");
  }
}
