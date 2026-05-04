package ch.swisstms.time_sync;

/**
 * Monotonic clock for measuring durations. Wraps {@code System.nanoTime()} but is calibrated
 * against the PHC where available. Suitable for latency measurement, NOT for regulator timestamps
 * (use {@link RegulatoryClock} for those — Constitution Principle IV).
 */
public final class MonotonicClock {

  private MonotonicClock() {}

  @SuppressWarnings("UseOfSystemOutOrSystemErr") // time-sync-exempt
  public static long nanos() {
    return System.nanoTime(); // time-sync-exempt — monotonic, not regulatory
  }

  public static long durationNanos(long startNanos) {
    return nanos() - startNanos;
  }
}
