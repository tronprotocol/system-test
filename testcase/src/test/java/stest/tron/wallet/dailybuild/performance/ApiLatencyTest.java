package stest.tron.wallet.dailybuild.performance;

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
 * API response latency measurement.
 *
 * <p>Measures gRPC API call latencies for common operations.
 * This is a smoke benchmark to ensure API response times are reasonable.
 */
@Slf4j
public class ApiLatencyTest extends TronBaseTest {

  private static final int ITERATIONS = 20;
  private static final long MAX_AVG_LATENCY_MS = 5000; // 5 seconds max avg

  @Test(enabled = true, description = "GetNowBlock API latency",
      groups = {"daily"})
  public void test01GetNowBlockLatency() {
    long totalMs = 0;
    for (int i = 0; i < ITERATIONS; i++) {
      long start = System.currentTimeMillis();
      Block block = PublicMethod.getBlock(-1, blockingStubFull);
      long elapsed = System.currentTimeMillis() - start;
      totalMs += elapsed;
      Assert.assertNotNull(block, "GetNowBlock should return a block");
    }
    double avgMs = (double) totalMs / ITERATIONS;
    logger.info("GetNowBlock avg latency: {:.1f} ms over {} calls", avgMs, ITERATIONS);
    Assert.assertTrue(avgMs < MAX_AVG_LATENCY_MS,
        "GetNowBlock avg latency should be < " + MAX_AVG_LATENCY_MS + " ms, got " + avgMs);
  }

  @Test(enabled = true, description = "ListWitnesses API latency",
      groups = {"daily"})
  public void test02ListWitnessesLatency() {
    long totalMs = 0;
    for (int i = 0; i < ITERATIONS; i++) {
      long start = System.currentTimeMillis();
      Optional<WitnessList> result = PublicMethod.listWitnesses(blockingStubFull);
      long elapsed = System.currentTimeMillis() - start;
      totalMs += elapsed;
      Assert.assertTrue(result.isPresent(), "ListWitnesses should return data");
    }
    double avgMs = (double) totalMs / ITERATIONS;
    logger.info("ListWitnesses avg latency: {:.1f} ms over {} calls", avgMs, ITERATIONS);
    Assert.assertTrue(avgMs < MAX_AVG_LATENCY_MS,
        "ListWitnesses avg latency should be < " + MAX_AVG_LATENCY_MS + " ms, got " + avgMs);
  }

  @Test(enabled = true, description = "GetBlockByNum API latency",
      groups = {"daily"})
  public void test03GetBlockByNumLatency() {
    Block latest = PublicMethod.getBlock(-1, blockingStubFull);
    long targetNum = latest.getBlockHeader().getRawData().getNumber() - 10;

    long totalMs = 0;
    for (int i = 0; i < ITERATIONS; i++) {
      long start = System.currentTimeMillis();
      Block block = PublicMethod.getBlock(targetNum, blockingStubFull);
      long elapsed = System.currentTimeMillis() - start;
      totalMs += elapsed;
      Assert.assertNotNull(block, "GetBlockByNum should return a block");
    }
    double avgMs = (double) totalMs / ITERATIONS;
    logger.info("GetBlockByNum avg latency: {:.1f} ms over {} calls", avgMs, ITERATIONS);
    Assert.assertTrue(avgMs < MAX_AVG_LATENCY_MS,
        "GetBlockByNum avg latency should be < " + MAX_AVG_LATENCY_MS + " ms, got " + avgMs);
  }

  @Test(enabled = true, description = "QueryAccount API latency",
      groups = {"daily"})
  public void test04QueryAccountLatency() {
    long totalMs = 0;
    for (int i = 0; i < ITERATIONS; i++) {
      long start = System.currentTimeMillis();
      PublicMethod.queryAccount(foundationAddress, blockingStubFull);
      long elapsed = System.currentTimeMillis() - start;
      totalMs += elapsed;
    }
    double avgMs = (double) totalMs / ITERATIONS;
    logger.info("QueryAccount avg latency: {:.1f} ms over {} calls", avgMs, ITERATIONS);
    Assert.assertTrue(avgMs < MAX_AVG_LATENCY_MS,
        "QueryAccount avg latency should be < " + MAX_AVG_LATENCY_MS + " ms, got " + avgMs);
  }

  @Test(enabled = true, description = "GetNextMaintenanceTime API latency",
      groups = {"daily"})
  public void test05GetNextMaintenanceTimeLatency() {
    long totalMs = 0;
    for (int i = 0; i < ITERATIONS; i++) {
      long start = System.currentTimeMillis();
      NumberMessage result = blockingStubFull.getNextMaintenanceTime(
          EmptyMessage.newBuilder().build());
      long elapsed = System.currentTimeMillis() - start;
      totalMs += elapsed;
      Assert.assertTrue(result.getNum() > 0);
    }
    double avgMs = (double) totalMs / ITERATIONS;
    logger.info("GetNextMaintenanceTime avg latency: {:.1f} ms over {} calls", avgMs, ITERATIONS);
    Assert.assertTrue(avgMs < MAX_AVG_LATENCY_MS,
        "GetNextMaintenanceTime avg latency should be < " + MAX_AVG_LATENCY_MS + " ms");
  }
}
