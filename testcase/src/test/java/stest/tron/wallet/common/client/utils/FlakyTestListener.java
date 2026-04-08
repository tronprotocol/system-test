package stest.tron.wallet.common.client.utils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * TestNG listener that tracks flaky test results separately.
 *
 * <p>When a test annotated with {@link Flaky} fails, it is logged but does not
 * count as an unexpected failure. At the end of the suite, a summary of all
 * flaky failures is printed.
 *
 * <p>Register in testng XML:
 * <pre>
 * &lt;listeners&gt;
 *   &lt;listener class-name="stest.tron.wallet.common.client.utils.FlakyTestListener"/&gt;
 * &lt;/listeners&gt;
 * </pre>
 */
public class FlakyTestListener implements ITestListener {

  private final List<String> flakyFailures = new ArrayList<>();
  private final List<String> flakyPasses = new ArrayList<>();

  @Override
  public void onTestFailure(ITestResult result) {
    Flaky flaky = getFlakyAnnotation(result);
    if (flaky != null) {
      String name = result.getTestClass().getName() + "." + result.getName();
      flakyFailures.add(name + " [reason: " + flaky.reason() + "]");
      System.out.println("[FLAKY-FAIL] " + name + " - " + flaky.reason());
    }
  }

  @Override
  public void onTestSuccess(ITestResult result) {
    Flaky flaky = getFlakyAnnotation(result);
    if (flaky != null) {
      String name = result.getTestClass().getName() + "." + result.getName();
      flakyPasses.add(name);
    }
  }

  @Override
  public void onFinish(ITestContext context) {
    if (flakyFailures.isEmpty() && flakyPasses.isEmpty()) {
      return;
    }
    System.out.println("\n========== Flaky Test Summary ==========");
    System.out.println("Flaky tests that PASSED: " + flakyPasses.size());
    System.out.println("Flaky tests that FAILED: " + flakyFailures.size());
    for (String f : flakyFailures) {
      System.out.println("  FAIL: " + f);
    }
    System.out.println("=========================================\n");
  }

  private Flaky getFlakyAnnotation(ITestResult result) {
    Method method = result.getMethod().getConstructorOrMethod().getMethod();
    if (method == null) {
      return null;
    }
    Flaky flaky = method.getAnnotation(Flaky.class);
    if (flaky != null) {
      return flaky;
    }
    return result.getTestClass().getRealClass().getAnnotation(Flaky.class);
  }

  // TestNG ITestListener default methods (no-op for Java 8 compatibility)
  @Override
  public void onTestStart(ITestResult result) {}

  @Override
  public void onTestSkipped(ITestResult result) {}

  @Override
  public void onTestFailedButWithinSuccessPercentage(ITestResult result) {}

  @Override
  public void onStart(ITestContext context) {}
}
