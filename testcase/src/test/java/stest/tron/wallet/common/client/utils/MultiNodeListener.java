package stest.tron.wallet.common.client.utils;

import com.typesafe.config.Config;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IClassListener;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestClass;
import org.testng.SkipException;
import stest.tron.wallet.common.client.Configuration;

/**
 * TestNG listener that detects single-node vs multi-node environments
 * and auto-skips {@link MultiNode @MultiNode} tests when only one node is available.
 *
 * <p>Detection logic (runs once at suite start):
 * <ol>
 *   <li>Read {@code fullnode.ip.list} from testng.conf</li>
 *   <li>If index [0] and [1] have the same host:port → single-node</li>
 *   <li>If index [1] is different but unreachable (TCP probe fails) → single-node</li>
 *   <li>Otherwise → multi-node</li>
 * </ol>
 *
 * <p>Register in TestNG XML:
 * <pre>{@code
 * <listeners>
 *   <listener class-name="stest.tron.wallet.common.client.utils.MultiNodeListener"/>
 * </listeners>
 * }</pre>
 *
 * <p>Or run with system property to force a mode:
 * <pre>
 *   -Dtron.test.multinode=true   (force multi-node, fail instead of skip)
 *   -Dtron.test.multinode=false  (force single-node, skip @MultiNode tests)
 * </pre>
 */
public class MultiNodeListener implements ISuiteListener, IClassListener {

  private static final Logger logger = LoggerFactory.getLogger(MultiNodeListener.class);
  private static final int PROBE_TIMEOUT_MS = 3000;

  /** Shared flag: true if multi-node environment is detected. */
  private static volatile Boolean multiNodeAvailable = null;

  @Override
  public void onStart(ISuite suite) {
    if (multiNodeAvailable != null) {
      return; // Already detected
    }
    multiNodeAvailable = detectMultiNode();
    String mode = multiNodeAvailable ? "MULTI-NODE" : "SINGLE-NODE";
    logger.info("=== Environment: {} ===", mode);
    logger.info("@MultiNode tests will be: {}",
        multiNodeAvailable ? "EXECUTED" : "SKIPPED");
  }

  @Override
  public void onFinish(ISuite suite) {
    // no-op
  }

  @Override
  public void onBeforeClass(ITestClass testClass) {
    if (Boolean.TRUE.equals(multiNodeAvailable)) {
      return; // Multi-node available, run everything
    }

    Class<?> realClass = testClass.getRealClass();
    if (realClass.isAnnotationPresent(MultiNode.class)) {
      MultiNode annotation = realClass.getAnnotation(MultiNode.class);
      String reason = annotation.reason().isEmpty()
          ? "requires multi-node environment"
          : annotation.reason();
      String msg = String.format(
          "SKIP %s — %s (single-node detected)", realClass.getSimpleName(), reason);
      logger.info(msg);
      throw new SkipException(msg);
    }
  }

  @Override
  public void onAfterClass(ITestClass testClass) {
    // no-op
  }

  /**
   * Returns true if a multi-node environment is available.
   * Can be called from test code for conditional logic.
   */
  public static boolean isMultiNodeAvailable() {
    if (multiNodeAvailable == null) {
      multiNodeAvailable = detectMultiNode();
    }
    return multiNodeAvailable;
  }

  private static boolean detectMultiNode() {
    // Check system property override
    String override = System.getProperty("tron.test.multinode");
    if (override != null) {
      boolean forced = Boolean.parseBoolean(override);
      logger.info("Multi-node mode forced via -Dtron.test.multinode={}", forced);
      return forced;
    }

    try {
      Config config = Configuration.getByPath("testng.conf");
      List<String> fullnodes = config.getStringList("fullnode.ip.list");

      if (fullnodes.size() < 2) {
        logger.info("Only {} fullnode(s) configured — single-node", fullnodes.size());
        return false;
      }

      String node0 = fullnodes.get(0).trim();
      String node1 = fullnodes.get(1).trim();

      // Same endpoint = single-node (the common single-node testng.conf pattern)
      if (node0.equals(node1)) {
        logger.info("fullnode.ip.list[0] == [1] ({}) — single-node", node0);
        return false;
      }

      // Different endpoint — probe if it's actually reachable
      logger.info("Probing second fullnode: {} ...", node1);
      if (probeEndpoint(node1)) {
        logger.info("Second fullnode {} is reachable — multi-node", node1);
        return true;
      } else {
        logger.warn("Second fullnode {} is NOT reachable — falling back to single-node", node1);
        return false;
      }

    } catch (Exception e) {
      logger.warn("Failed to detect environment: {} — defaulting to single-node", e.getMessage());
      return false;
    }
  }

  /**
   * TCP connect probe to check if a host:port is reachable.
   */
  private static boolean probeEndpoint(String hostPort) {
    String[] parts = hostPort.split(":");
    if (parts.length != 2) {
      return false;
    }
    String host = parts[0];
    int port;
    try {
      port = Integer.parseInt(parts[1]);
    } catch (NumberFormatException e) {
      return false;
    }

    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(host, port), PROBE_TIMEOUT_MS);
      return true;
    } catch (IOException e) {
      return false;
    }
  }
}
