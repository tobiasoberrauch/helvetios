package ch.swisstms.time_sync;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/**
 * RTS-25-konforme Quelle für regulatorische Zeitstempel.
 *
 * <p>Constitution Principle IV — Domain-Code MUSS diese Klasse verwenden, nicht {@code
 * System.currentTimeMillis()}, {@code Instant.now()} oder andere Wall-Clock-Funktionen für
 * Zeitstempel, die in Audit-Chains, Regulator-Reports oder Surveillance-Events fließen.
 *
 * <p>Implementierungen lesen, wo verfügbar, die PTP-Hardware-Clock (PHC) der NIC. In lokalen
 * Dev-/CI-Umgebungen wird die System-UTC-Clock benutzt.
 */
public final class RegulatoryClock {

  private static volatile Clock delegate = Clock.systemUTC();

  private RegulatoryClock() {}

  /**
   * Erstellt einen Zeitstempel mit Mikrosekunden-Präzision (RTS-25 verlangt 1µs Granularität für
   * Trading-Server).
   */
  public static Instant nowBiz() {
    // time-sync-exempt — explicit allow-list for the regulatory clock
    // implementation itself. Constitution Principle IV applies to the
    // *callers*, not to libs/time-sync/.
    return delegate.instant();
  }

  public static long nowMicros() {
    Instant now = nowBiz();
    return now.getEpochSecond() * 1_000_000L + now.getNano() / 1_000L;
  }

  public static long nowNanos() {
    Instant now = nowBiz();
    return now.getEpochSecond() * 1_000_000_000L + now.getNano();
  }

  /** Test-only: replace the underlying clock (e.g., {@code Clock.fixed(...)}). */
  public static void setDelegateForTest(Clock testClock) {
    delegate = testClock.withZone(ZoneOffset.UTC);
  }

  public static void resetForTest() {
    delegate = Clock.systemUTC();
  }
}
