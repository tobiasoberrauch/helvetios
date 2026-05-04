package ch.swisstms.domain.health;

import java.time.Instant;

/** Snapshot of an adapter's connectivity health. Polled by ops dashboards. */
public record HealthSnapshot(
    String venueId,
    Status status,
    Instant lastHeartbeat,
    long lastSenderSeq,
    long lastTargetSeq,
    String details) {
  public enum Status {
    CONNECTED,
    DEGRADED,
    DISCONNECTED
  }
}
