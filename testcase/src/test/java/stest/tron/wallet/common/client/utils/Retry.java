package stest.tron.wallet.common.client.utils;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * TestNG retry analyzer for flaky tests.
 *
 * <p>Configurable via system properties:
 * <ul>
 *   <li>{@code tron.test.retry.max} - max retry count (default: 2)</li>
 *   <li>{@code tron.test.retry.interval} - sleep between retries in ms (default: 3000)</li>
 * </ul>
 *
 * <p>Example: {@code -Dtron.test.retry.max=3 -Dtron.test.retry.interval=5000}
 */
public class Retry implements IRetryAnalyzer {

  private static final int DEFAULT_MAX_RETRY = 2;
  private static final long DEFAULT_INTERVAL_MS = 3000L;

  private int retryCount = 0;
  private final int maxRetryCount;
  private final long intervalMs;

  public Retry() {
    this.maxRetryCount = Integer.getInteger("tron.test.retry.max", DEFAULT_MAX_RETRY);
    this.intervalMs = Long.getLong("tron.test.retry.interval", DEFAULT_INTERVAL_MS);
  }

  @Override
  public boolean retry(ITestResult result) {
    if (retryCount < maxRetryCount) {
      retryCount++;
      System.out.println("Retrying test " + result.getName() + " ["
          + getResultStatusName(result.getStatus()) + "] attempt " + retryCount
          + "/" + maxRetryCount);
      try {
        Thread.sleep(intervalMs);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      return true;
    }
    return false;
  }

  private static String getResultStatusName(int status) {
    switch (status) {
      case ITestResult.SUCCESS: return "SUCCESS";
      case ITestResult.FAILURE: return "FAILURE";
      case ITestResult.SKIP: return "SKIP";
      default: return "UNKNOWN";
    }
  }
}
