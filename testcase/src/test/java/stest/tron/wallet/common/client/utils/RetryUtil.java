package stest.tron.wallet.common.client.utils;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility for retry/polling patterns commonly used in system tests.
 *
 * <p>Replaces the scattered {@code while (retryTimes-- > 0)} + {@code Thread.sleep()} patterns
 * with a reusable, configurable helper.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Wait until a condition becomes true
 * boolean ok = RetryUtil.waitUntil(() -> getBalance(addr) > 0, 10, 3000);
 *
 * // Poll and return a result once non-null
 * TransactionInfo info = RetryUtil.pollUntilNonNull(
 *     () -> getTransactionInfoById(txid), 10, 3000);
 * }</pre>
 */
@Slf4j
public final class RetryUtil {

  private RetryUtil() {
    // utility class
  }

  /**
   * Retries the given condition until it returns true or max attempts are exhausted.
   *
   * @param condition the condition to evaluate
   * @param maxAttempts maximum number of attempts
   * @param intervalMs milliseconds to wait between attempts
   * @return true if the condition was met within the allowed attempts
   */
  public static boolean waitUntil(BooleanSupplier condition, int maxAttempts, long intervalMs) {
    for (int i = 0; i < maxAttempts; i++) {
      if (condition.getAsBoolean()) {
        return true;
      }
      try {
        Thread.sleep(intervalMs);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      }
    }
    return condition.getAsBoolean();
  }

  /**
   * Polls the given supplier until it returns a non-null result.
   *
   * @param supplier the supplier to poll
   * @param maxAttempts maximum number of attempts
   * @param intervalMs milliseconds to wait between attempts
   * @param <T> the result type
   * @return the first non-null result, or null if all attempts are exhausted
   */
  public static <T> T pollUntilNonNull(Supplier<T> supplier, int maxAttempts, long intervalMs) {
    for (int i = 0; i < maxAttempts; i++) {
      T result = supplier.get();
      if (result != null) {
        return result;
      }
      try {
        Thread.sleep(intervalMs);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return null;
      }
    }
    return supplier.get();
  }

  /**
   * Convenience: wait until condition is true, using block interval (3s) and 10 attempts.
   */
  public static boolean waitUntil(BooleanSupplier condition) {
    return waitUntil(condition, 10, 3000);
  }
}
