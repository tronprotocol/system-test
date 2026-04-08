package stest.tron.wallet.common.client.utils;

/**
 * Common constants used across TRON system tests.
 * Eliminates magic numbers scattered throughout test classes.
 */
public final class TronConstants {

  private TronConstants() {
    // utility class
  }

  // --------------- Channel / Network ---------------
  /** gRPC channel shutdown timeout in seconds. */
  public static final int CHANNEL_SHUTDOWN_TIMEOUT_SECONDS = 5;

  // --------------- Block Timing ---------------
  /** Standard wait for one block to be produced (ms). */
  public static final long ONE_BLOCK_WAIT_MS = 3_000L;

  /** Standard wait for two blocks (ms). */
  public static final long TWO_BLOCK_WAIT_MS = 6_000L;

  /** Long wait for proposal or multi-block operations (ms). */
  public static final long LONG_WAIT_MS = 30_000L;

  // --------------- Token / Amount Units ---------------
  /** 1 TRX in sun (smallest unit). */
  public static final long ONE_TRX = 1_000_000L;

  /** Common transfer amount used in tests: 10 TRX. */
  public static final long TEN_TRX = 10_000_000L;

  /** Common transfer amount: 100 TRX. */
  public static final long HUNDRED_TRX = 100_000_000L;

  /** Common transfer amount: 1000 TRX. */
  public static final long THOUSAND_TRX = 1_000_000_000L;

  /** Common transfer amount: 10000 TRX. */
  public static final long TEN_THOUSAND_TRX = 10_000_000_000L;

  // --------------- Freeze / Resource ---------------
  /** Freeze type: Bandwidth = 0. */
  public static final int FREEZE_BANDWIDTH = 0;

  /** Freeze type: Energy = 1. */
  public static final int FREEZE_ENERGY = 1;

  /** Freeze type: TronPower = 2. */
  public static final int FREEZE_TRON_POWER = 2;
}
