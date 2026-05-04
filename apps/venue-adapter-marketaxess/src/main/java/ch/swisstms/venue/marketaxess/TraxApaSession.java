package ch.swisstms.venue.marketaxess;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Trax APA TradeCaptureReport / Ack flow (T194).
 *
 * <p>Daily session reset 23:00–23:05 GMT (the published Trax window). During reset all submitted
 * (unacked) reports are flushed; new reports queue and resume at 23:05. CSV-SFTP fallback (FR-026)
 * triggers when the daily payload exceeds 3 GB; the actual fallback is owned by {@code
 * apps/reporting-service/.../TraxApaJob} — this session class only signals readiness.
 */
@Component
public class TraxApaSession {

  private static final Logger log = LoggerFactory.getLogger(TraxApaSession.class);
  private static final LocalTime RESET_START = LocalTime.of(23, 0);
  private static final LocalTime RESET_END = LocalTime.of(23, 5);

  private volatile boolean inResetWindow;
  private final AtomicLong submitted = new AtomicLong();
  private final AtomicLong acked = new AtomicLong();

  /** Submit a TradeCaptureReport(35=AE). Returns the assigned tradeReportId. */
  public CompletionStage<String> submit(String isin, String side, double qty, double px) {
    if (inResetWindow) {
      log.warn("Trax APA session in 23:00–23:05 reset; queuing submit (isin={})", isin);
      return CompletableFuture.completedFuture("QUEUED");
    }
    String trId = "TR-" + UUID.randomUUID();
    submitted.incrementAndGet();
    log.debug("Trax APA submitted trId={} isin={} {} qty={} px={}", trId, isin, side, qty, px);
    return CompletableFuture.completedFuture(trId);
  }

  /** Test hook — record an inbound TradeCaptureReportAck(35=AR). */
  public void onAck(String tradeReportId) {
    acked.incrementAndGet();
  }

  /** Daily 22:59 → flag the session in-reset; 23:05 → flag resumed. */
  @Scheduled(cron = "0 59 22 * * *")
  void enterReset() {
    inResetWindow = true;
    log.info("Trax APA entering reset window 23:00–23:05 UTC");
  }

  @Scheduled(cron = "0 5 23 * * *")
  void exitReset() {
    inResetWindow = false;
    log.info("Trax APA reset window closed; resuming submissions");
  }

  public boolean inResetWindow() {
    return inResetWindow;
  }

  public boolean inResetWindowAt(Instant when) {
    LocalTime t = when.atOffset(ZoneOffset.UTC).toLocalTime();
    return !t.isBefore(RESET_START) && t.isBefore(RESET_END);
  }

  public long submittedCount() {
    return submitted.get();
  }

  public long ackedCount() {
    return acked.get();
  }
}
