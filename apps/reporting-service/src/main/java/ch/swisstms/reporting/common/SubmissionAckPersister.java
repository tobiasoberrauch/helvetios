package ch.swisstms.reporting.common;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

/**
 * Tracks regulator-side acknowledgement state for every report submission (T140 / FR-029).
 *
 * <p>Phase 7 stores in-process so the unit + integration tests are self-contained. Phase 14
 * (Multi-Region) replaces this with a JPA-backed table (`reporting.submission_ack`) so cross-region
 * reconciliation jobs see the same state.
 *
 * <p>Status values: {@code PENDING} (sent, no ack yet), {@code ACK} (TR accepted), {@code NACK} (TR
 * rejected — manual intervention required), {@code TIMEOUT} (no ack within 24h — Sev-3 alert).
 */
@Component
public class SubmissionAckPersister {

  public enum Status {
    PENDING,
    ACK,
    NACK,
    TIMEOUT
  }

  public record SubmissionRecord(
      String trId,
      String submissionId,
      LocalDate reportingDate,
      Status status,
      Instant lastUpdate,
      String reason) {}

  private final ConcurrentMap<String, SubmissionRecord> records = new ConcurrentHashMap<>();

  public void recordPending(String trId, String submissionId, LocalDate reportingDate) {
    records.put(
        key(trId, submissionId),
        new SubmissionRecord(
            trId, submissionId, reportingDate, Status.PENDING, Instant.now(), null));
  }

  public void recordAck(String trId, String submissionId) {
    records.computeIfPresent(
        key(trId, submissionId),
        (k, v) ->
            new SubmissionRecord(
                v.trId(), v.submissionId(), v.reportingDate(), Status.ACK, Instant.now(), null));
  }

  public void recordNack(String trId, String submissionId, String reason) {
    records.computeIfPresent(
        key(trId, submissionId),
        (k, v) ->
            new SubmissionRecord(
                v.trId(), v.submissionId(), v.reportingDate(), Status.NACK, Instant.now(), reason));
  }

  /**
   * Returns submissions whose status is still PENDING and that were sent more than {@code hours}
   * hours ago. Used by the recon jobs to fire Sev-3 alerts.
   */
  public List<SubmissionRecord> findStalePending(long hours) {
    Instant cutoff = Instant.now().minusSeconds(hours * 3600);
    return records.values().stream()
        .filter(r -> r.status() == Status.PENDING && r.lastUpdate().isBefore(cutoff))
        .toList();
  }

  public Map<String, SubmissionRecord> snapshot() {
    return Map.copyOf(records);
  }

  private static String key(String trId, String submissionId) {
    return trId + "|" + submissionId;
  }
}
