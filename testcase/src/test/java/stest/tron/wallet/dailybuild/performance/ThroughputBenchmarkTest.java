package stest.tron.wallet.dailybuild.performance;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.TronConstants;
import stest.tron.wallet.common.client.utils.Utils;

/**
 * Transaction throughput benchmark.
 *
 * <p>Measures how many TRX transfer transactions can be submitted and
 * confirmed within a given time window. This is a smoke benchmark,
 * not a full stress test.
 */
@Slf4j
public class ThroughputBenchmarkTest extends TronBaseTest {

  private static final int BATCH_SIZE = 50;
  private ECKey senderKey;
  private byte[] senderAddress;
  private String senderKeyStr;
  private List<byte[]> receiverAddresses = new ArrayList<>();

  @BeforeClass(enabled = true)
  public void beforeClass() {
    senderKey = new ECKey(Utils.getRandom());
    senderAddress = senderKey.getAddress();
    senderKeyStr = ByteArray.toHexString(senderKey.getPrivKeyBytes());

    // Fund sender with enough TRX for batch transfers + fees
    Assert.assertTrue(PublicMethod.sendcoin(senderAddress,
        TronConstants.TEN_THOUSAND_TRX * 10,
        foundationAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    // Pre-generate receiver addresses
    for (int i = 0; i < BATCH_SIZE; i++) {
      ECKey key = new ECKey(Utils.getRandom());
      receiverAddresses.add(key.getAddress());
    }
  }

  @Test(enabled = true, description = "Batch submit TRX transfers and measure throughput",
      groups = {"daily"})
  public void test01BatchTransferThroughput() {
    long sendAmount = 100_000L; // 0.1 TRX each
    int successCount = 0;

    long startTime = System.currentTimeMillis();
    for (int i = 0; i < BATCH_SIZE; i++) {
      boolean result = PublicMethod.sendcoin(receiverAddresses.get(i), sendAmount,
          senderAddress, senderKeyStr, blockingStubFull);
      if (result) {
        successCount++;
      }
    }
    long elapsed = System.currentTimeMillis() - startTime;

    logger.info("Submitted {} transactions in {} ms ({} success)",
        BATCH_SIZE, elapsed, successCount);
    double tps = (successCount * 1000.0) / elapsed;
    logger.info("Submission TPS: {:.2f}", tps);

    Assert.assertTrue(successCount > BATCH_SIZE * 0.8,
        "At least 80% of transactions should be accepted, got " + successCount);

    // Wait for confirmation
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    // Verify some transactions were confirmed
    int confirmedCount = 0;
    for (int i = 0; i < Math.min(10, BATCH_SIZE); i++) {
      org.tron.protos.Protocol.Account acc = PublicMethod.queryAccount(
          receiverAddresses.get(i), blockingStubFull);
      if (acc.getBalance() == sendAmount) {
        confirmedCount++;
      }
    }
    logger.info("Confirmed {}/10 sampled transactions", confirmedCount);
    Assert.assertTrue(confirmedCount > 0, "At least some transactions should be confirmed");
  }

  @AfterClass(enabled = true)
  public void afterClass() {
    PublicMethod.freeResource(senderAddress, senderKeyStr, foundationAddress, blockingStubFull);
  }
}
