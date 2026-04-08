package stest.tron.wallet.common.client.utils;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a test method or class as flaky.
 *
 * <p>Flaky tests are those that fail intermittently due to timing, environment,
 * or external service dependencies rather than actual code bugs.
 *
 * <p>Usage:
 * <pre>
 * {@literal @}Flaky(reason = "Depends on MongoDB availability",
 *        since = "2026-04-03",
 *        issue = "https://github.com/tronprotocol/system-test/issues/XX")
 * {@literal @}Test(groups = {"daily"})
 * public void testMongoEventQuery() { ... }
 * </pre>
 *
 * <p>Governance rules:
 * <ul>
 *   <li>Every {@code @Flaky} annotation must include a {@code reason}</li>
 *   <li>Tests marked flaky for more than 30 days without a fix should be reviewed for removal</li>
 *   <li>Flaky tests still run in CI but failures are tracked separately</li>
 * </ul>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Flaky {

  /** Why this test is flaky. Required. */
  String reason();

  /** Date when the annotation was added (ISO-8601, e.g. "2026-04-03"). */
  String since() default "";

  /** Link to the tracking issue, if any. */
  String issue() default "";
}
