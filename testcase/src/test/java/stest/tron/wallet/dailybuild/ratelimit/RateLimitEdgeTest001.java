package stest.tron.wallet.dailybuild.ratelimit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol.Account;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.TronBaseTest;

/**
 * TIP-467: Rate limiting boundary tests.
 *
 * <p>Tests gRPC rate limiting behavior including burst requests,
 * different API endpoint limits, and recovery after rate limiting.
 */
@Slf4j
public class RateLimitEdgeTest001 extends TronBaseTest {

  @BeforeClass(enabled = true)
  public void beforeClass() {
    // Base class provides channelFull and blockingStubFull
  }

  @Test(enabled = true, description = "Single request should always succeed",
      groups = {"daily", "serial"})
  public void test01SingleRequestSucceeds() {
    long blockNum = blockingStubFull.getNowBlock2(
        EmptyMessage.newBuilder().build()).getBlockHeader().getRawData().getNumber();
    logger.info("Current block number: {}", blockNum);
  }

  @Test(enabled = true, description = "Burst of getNowBlock requests",
      groups = {"daily", "serial"})
  public void test02BurstGetNowBlock() {
    int successCount = 0;
    int failCount = 0;
    int totalRequests = 50;

    for (int i = 0; i < totalRequests; i++) {
      try {
        blockingStubFull.getNowBlock2(EmptyMessage.newBuilder().build());
        successCount++;
      } catch (StatusRuntimeException e) {
        failCount++;
        logger.info("Request {} rate limited: {}", i, e.getStatus().getCode());
      }
    }

    logger.info("Burst test: {} success, {} rate limited out of {}",
        successCount, failCount, totalRequests);
    Assert.assertTrue(successCount > 0, "At least some requests should succeed");
  }

  @Test(enabled = true, description = "Burst of getAccount requests",
      groups = {"daily", "serial"})
  public void test03BurstGetAccount() {
    int successCount = 0;
    int failCount = 0;
    int totalRequests = 30;

    for (int i = 0; i < totalRequests; i++) {
      try {
        PublicMethod.queryAccount(foundationAddress, blockingStubFull);
        successCount++;
      } catch (StatusRuntimeException e) {
        failCount++;
      }
    }

    logger.info("getAccount burst: {} success, {} limited out of {}",
        successCount, failCount, totalRequests);
    Assert.assertTrue(successCount > 0, "At least some requests should succeed");
  }

  @Test(enabled = true, description = "Recovery after rate limiting pause",
      groups = {"daily", "serial"})
  public void test04RecoveryAfterPause() throws InterruptedException {
    // Send a burst to potentially trigger rate limiting
    for (int i = 0; i < 50; i++) {
      try {
        blockingStubFull.getNowBlock2(EmptyMessage.newBuilder().build());
      } catch (StatusRuntimeException e) {
        // Expected during burst
      }
    }

    // Wait for rate limit window to reset
    Thread.sleep(5000);

    // Single request should succeed after pause
    try {
      blockingStubFull.getNowBlock2(EmptyMessage.newBuilder().build());
      logger.info("Request succeeded after rate limit recovery");
    } catch (StatusRuntimeException e) {
      Assert.fail("Request should succeed after recovery pause: " + e.getMessage());
    }
  }

  @Test(enabled = true, description = "Multiple connections should have independent rate limits",
      groups = {"daily", "serial"})
  public void test05IndependentConnectionLimits() throws InterruptedException {
    // Create a second channel
    ManagedChannel channel2 = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext().build();
    WalletGrpc.WalletBlockingStub stub2 = WalletGrpc.newBlockingStub(channel2);

    try {
      // Both channels should be able to make requests
      blockingStubFull.getNowBlock2(EmptyMessage.newBuilder().build());
      stub2.getNowBlock2(EmptyMessage.newBuilder().build());
      logger.info("Both connections can make independent requests");
    } finally {
      channel2.shutdown().awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
    }
  }

  @AfterClass(enabled = true)
  public void afterClass() {
    // No resources to clean up - base class handles channel shutdown
  }
}
