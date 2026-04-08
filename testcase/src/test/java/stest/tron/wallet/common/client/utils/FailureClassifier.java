package stest.tron.wallet.common.client.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.SkipException;

/**
 * TestNG listener that classifies test failures by root cause.
 *
 * <p>At the end of each suite, prints a summary that separates environment
 * issues from real bugs, so developers can focus on actionable failures.
 *
 * <p>Classification categories:
 * <ul>
 *   <li>{@code ENV_NODE_DOWN} — gRPC connection refused or UNAVAILABLE</li>
 *   <li>{@code ENV_SINGLE_NODE} — @MultiNode test skipped on single-node</li>
 *   <li>{@code ENV_NO_MONGO} — MongoDB-related failure</li>
 *   <li>{@code FLAKY} — Test marked with @Flaky annotation</li>
 *   <li>{@code TIMEOUT} — Execution timeout</li>
 *   <li>{@code BUG} — Likely a real code bug (assertion failure, NPE, etc.)</li>
 * </ul>
 *
 * <p>Register in testng XML:
 * <pre>
 * &lt;listeners&gt;
 *   &lt;listener class-name="stest.tron.wallet.common.client.utils.FailureClassifier"/&gt;
 * &lt;/listeners&gt;
 * </pre>
 */
public class FailureClassifier implements ITestListener {

  private final Map<String, List<String>> classified = new LinkedHashMap<>();
  private int totalFailures = 0;
  private int totalSkips = 0;

  public FailureClassifier() {
    classified.put("ENV_NODE_DOWN", new ArrayList<String>());
    classified.put("ENV_SINGLE_NODE", new ArrayList<String>());
    classified.put("ENV_NO_MONGO", new ArrayList<String>());
    classified.put("FLAKY", new ArrayList<String>());
    classified.put("TIMEOUT", new ArrayList<String>());
    classified.put("BUG", new ArrayList<String>());
  }

  @Override
  public void onTestFailure(ITestResult result) {
    totalFailures++;
    String testName = result.getTestClass().getRealClass().getSimpleName()
        + "." + result.getName();
    Throwable cause = result.getThrowable();
    String category = classify(result, cause);
    classified.get(category).add(testName);
  }

  @Override
  public void onTestSkipped(ITestResult result) {
    totalSkips++;
    Throwable cause = result.getThrowable();
    if (cause instanceof SkipException) {
      String msg = cause.getMessage();
      if (msg != null && (msg.contains("Multi-node") || msg.contains("single-node")
          || msg.contains("fullnode"))) {
        String testName = result.getTestClass().getRealClass().getSimpleName()
            + "." + result.getName();
        classified.get("ENV_SINGLE_NODE").add(testName);
      }
    }
  }

  @Override
  public void onFinish(ITestContext context) {
    int classifiedTotal = 0;
    for (List<String> list : classified.values()) {
      classifiedTotal += list.size();
    }
    if (classifiedTotal == 0) {
      return;
    }

    int bugCount = classified.get("BUG").size();

    System.out.println();
    System.out.println("=== Failure Classification Summary ===");

    for (Map.Entry<String, List<String>> entry : classified.entrySet()) {
      List<String> tests = entry.getValue();
      if (!tests.isEmpty()) {
        String sample = tests.get(0);
        if (tests.size() > 1) {
          sample += " ...";
        }
        System.out.printf("  %-18s %3d  (%s)%n", entry.getKey() + ":", tests.size(), sample);
      }
    }

    System.out.println("  ────────────────────────");
    System.out.printf("  Total Failures:    %3d%n", totalFailures);
    System.out.printf("  Total Skips:       %3d%n", totalSkips);
    System.out.printf("  Actionable Bugs:   %3d  <<< focus here%n", bugCount);
    System.out.println("=======================================");
    System.out.println();
  }

  private String classify(ITestResult result, Throwable cause) {
    // Check @Flaky annotation
    if (isFlaky(result)) {
      return "FLAKY";
    }

    if (cause == null) {
      return "BUG";
    }

    String message = getFullMessage(cause);

    // ENV_NODE_DOWN: gRPC connection issues
    if (message.contains("UNAVAILABLE")
        || message.contains("Connection refused")
        || message.contains("connect failed")
        || message.contains("Channel shutdown")
        || message.contains("ManagedChannel")
        || message.contains("StatusRuntimeException")) {
      return "ENV_NODE_DOWN";
    }

    // ENV_SINGLE_NODE: multi-node required
    if (message.contains("Multi-node")
        || message.contains("single-node")
        || message.contains("fullnode.ip.list")) {
      return "ENV_SINGLE_NODE";
    }

    // ENV_NO_MONGO: MongoDB issues
    if (message.contains("Mongo")
        || message.contains("mongo")
        || message.contains("MongoClient")
        || message.contains("27017")) {
      return "ENV_NO_MONGO";
    }

    // TIMEOUT: execution timeouts
    if (message.contains("TimeoutException")
        || message.contains("timed out")
        || message.contains("Timeout")
        || message.contains("CPU timeout")) {
      return "TIMEOUT";
    }

    return "BUG";
  }

  private boolean isFlaky(ITestResult result) {
    Class<?> testClass = result.getTestClass().getRealClass();
    if (testClass.getAnnotation(Flaky.class) != null) {
      return true;
    }
    java.lang.reflect.Method method =
        result.getMethod().getConstructorOrMethod().getMethod();
    return method != null && method.getAnnotation(Flaky.class) != null;
  }

  private String getFullMessage(Throwable cause) {
    StringBuilder sb = new StringBuilder();
    Throwable current = cause;
    while (current != null) {
      sb.append(current.getClass().getSimpleName()).append(": ");
      if (current.getMessage() != null) {
        sb.append(current.getMessage());
      }
      sb.append(" | ");
      current = current.getCause();
    }
    return sb.toString();
  }

  // TestNG ITestListener default methods (Java 8 compatibility)
  @Override
  public void onTestStart(ITestResult result) {}

  @Override
  public void onTestSuccess(ITestResult result) {}

  @Override
  public void onTestFailedButWithinSuccessPercentage(ITestResult result) {}

  @Override
  public void onStart(ITestContext context) {}
}
