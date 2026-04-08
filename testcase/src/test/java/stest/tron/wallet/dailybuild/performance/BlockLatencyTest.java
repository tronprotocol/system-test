package stest.tron.wallet.dailybuild.performance;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.tron.protos.Protocol.Block;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.TronBaseTest;

/**
 * Block latency measurement.
 *
 * <p>Measures actual block production intervals by observing consecutive
 * blocks and computing statistics. TRON targets 3-second block intervals.
 */
@Slf4j
public class BlockLatencyTest extends TronBaseTest {

  private static final int SAMPLE_SIZE = 10;

  @Test(enabled = true, description = "Measure block production interval statistics",
      groups = {"daily"})
  public void test01BlockIntervalStatistics() {
    // Wait for enough blocks to exist
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Block latest = blockingStubFull.getNowBlock(org.tron.api.GrpcAPI.EmptyMessage.newBuilder().build());
    long currentNum = latest.getBlockHeader().getRawData().getNumber();
    Assert.assertTrue(currentNum > SAMPLE_SIZE,
        "Need at least " + SAMPLE_SIZE + " blocks, got " + currentNum);

    long startNum = currentNum - SAMPLE_SIZE;
    List<Long> intervals = new ArrayList<>();

    long prevTimestamp = -1;
    for (long i = startNum; i <= currentNum; i++) {
      Block block = PublicMethod.getBlock(i, blockingStubFull);
      if (block == null || block.getBlockHeader().getRawData().getNumber() == 0) {
        continue;
      }
      long ts = block.getBlockHeader().getRawData().getTimestamp();
      if (ts <= 0) {
        continue;
      }
      if (prevTimestamp > 0) {
        intervals.add(ts - prevTimestamp);
      }
      prevTimestamp = ts;
    }

    Assert.assertFalse(intervals.isEmpty(), "Should have collected interval data");

    long sum = intervals.stream().mapToLong(Long::longValue).sum();
    double avgMs = (double) sum / intervals.size();
    long minMs = intervals.stream().mapToLong(Long::longValue).min().orElse(0);
    long maxMs = intervals.stream().mapToLong(Long::longValue).max().orElse(0);

    logger.info("Block interval stats over {} samples:", intervals.size());
    logger.info("  Average: {:.1f} ms", avgMs);
    logger.info("  Min: {} ms, Max: {} ms", minMs, maxMs);

    // TRON targets 3000ms blocks; allow 2000-6000ms average
    Assert.assertTrue(avgMs >= 2000 && avgMs <= 6000,
        "Average block interval should be 2-6 seconds, got " + avgMs + " ms");
  }

  @Test(enabled = true, description = "Block numbers should be strictly sequential",
      groups = {"daily"})
  public void test02BlockNumberSequential() {
    Block latest = blockingStubFull.getNowBlock(org.tron.api.GrpcAPI.EmptyMessage.newBuilder().build());
    long currentNum = latest.getBlockHeader().getRawData().getNumber();

    long startNum = Math.max(1, currentNum - 20);
    long prevNum = startNum - 1;
    for (long i = startNum; i <= currentNum; i++) {
      Block block = PublicMethod.getBlock(i, blockingStubFull);
      Assert.assertNotNull(block, "Block " + i + " should exist");
      long num = block.getBlockHeader().getRawData().getNumber();
      Assert.assertEquals(num, prevNum + 1,
          "Block numbers should be sequential");
      prevNum = num;
    }
    logger.info("Verified {} sequential blocks ({} to {})",
        currentNum - startNum + 1, startNum, currentNum);
  }
}
