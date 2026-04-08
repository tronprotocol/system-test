package stest.tron.wallet.common.client.utils;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a test class as requiring a multi-node environment.
 *
 * <p>Tests annotated with {@code @MultiNode} need at least two java-tron nodes running
 * (e.g., separate FullNode + Solidity, or two SR witnesses). When only a single node is
 * available, these tests are automatically skipped by {@link MultiNodeListener}.
 *
 * <p>Usage:
 * <pre>
 * {@literal @}MultiNode(reason = "Needs second FullNode for cross-node broadcast verification")
 * public class WalletTestTransfer003 extends TronBaseTest {
 *     ...
 * }
 * </pre>
 *
 * <p>Detection: At suite start, {@link MultiNodeListener} probes
 * {@code fullnode.ip.list[1]} from testng.conf. If it is the same as index [0] or
 * unreachable, the environment is classified as single-node and all {@code @MultiNode}
 * classes are skipped with a clear message.
 *
 * @see MultiNodeListener
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MultiNode {

  /**
   * Why this test needs multiple nodes.
   * Examples: "cross-node sync", "solidity query", "PBFT finality".
   */
  String reason() default "";
}
