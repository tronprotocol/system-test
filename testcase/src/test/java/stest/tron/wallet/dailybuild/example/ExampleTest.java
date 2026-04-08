package stest.tron.wallet.dailybuild.example;

import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.protos.Protocol.Account;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.RetryUtil;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.TronConstants;
import stest.tron.wallet.common.client.utils.Utils;

/**
 * Example test class demonstrating best practices for TRON system tests.
 *
 * <p>Key patterns shown:
 * <ul>
 *   <li>Extend {@link TronBaseTest} - provides channelFull, blockingStubFull,
 *       foundationKey/Address, witnessKey/Address, maxFeeLimit</li>
 *   <li>Use {@link TronConstants} - eliminates magic numbers</li>
 *   <li>Use {@link RetryUtil} - replaces manual retry loops</li>
 *   <li>Use Helper classes - AccountHelper, ContractHelper, etc.</li>
 *   <li>Use TestNG groups - for selective test execution</li>
 * </ul>
 */
@Slf4j
public class ExampleTest extends TronBaseTest {

  // Test-specific accounts - generated fresh for each test class run
  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] testAddress = ecKey1.getAddress();
  private String testKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  /**
   * Setup: fund the test account from foundation.
   * Note: channelFull and blockingStubFull are already initialized by TronBaseTest.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(testKey);
    Assert.assertTrue(PublicMethod.sendcoin(testAddress, TronConstants.TEN_TRX,
        foundationAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "Verify test account received funds",
      groups = {"daily", "smoke"})
  public void test01AccountBalance() {
    Account account = PublicMethod.queryAccount(testAddress, blockingStubFull);
    Assert.assertTrue(account.getBalance() > 0,
        "Test account should have positive balance after funding");
    logger.info("Test account balance: {} sun", account.getBalance());
  }

  @Test(enabled = true, description = "Transfer TRX and verify with RetryUtil",
      groups = {"daily"})
  public void test02TransferWithRetry() {
    ECKey receiver = new ECKey(Utils.getRandom());
    long amount = TronConstants.ONE_TRX;

    Assert.assertTrue(PublicMethod.sendcoin(receiver.getAddress(), amount,
        testAddress, testKey, blockingStubFull));

    // Use RetryUtil instead of manual while + Thread.sleep loops
    boolean confirmed = RetryUtil.waitUntil(() -> {
      Account account = PublicMethod.queryAccount(receiver.getAddress(), blockingStubFull);
      return account.getBalance() >= amount;
    });

    Assert.assertTrue(confirmed, "Receiver should have received TRX within retry window");
  }

  /**
   * Cleanup: return remaining funds to foundation.
   * Note: channel shutdown is handled automatically by TronBaseTest.closeChannels().
   */
  @AfterClass(enabled = true)
  public void afterClass() {
    PublicMethod.freeResource(testAddress, testKey, foundationAddress, blockingStubFull);
  }
}
