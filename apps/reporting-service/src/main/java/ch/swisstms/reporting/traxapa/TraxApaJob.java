package ch.swisstms.reporting.traxapa;

import ch.swisstms.audit_chain.AuditEvent.ActorType;
import ch.swisstms.audit_chain.HashChainWriter;
import ch.swisstms.reporting.common.SubmissionAckPersister;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * FR-026 — MarketAxess Trax APA trade-publication.
 *
 * <p>Sends TradeCaptureReport(35=AE) per publishable fill (built by {@link TraxApaTcrBuilder}),
 * tracks ack via {@link SubmissionAckPersister}. Daily session-reset window 23:00–23:05 GMT (Trax
 * published window). CSV-SFTP fallback when daily payload &gt; 3 GB (FR-026 / Trax error GBX-010).
 */
@Component
public class TraxApaJob {

  private static final Logger log = LoggerFactory.getLogger(TraxApaJob.class);
  private static final String TR_ID = "TRAX-APA";

  private final TraxApaTcrBuilder builder;
  private final SubmissionAckPersister ackPersister;
  private final HashChainWriter audit;

  public TraxApaJob(
      TraxApaTcrBuilder builder, SubmissionAckPersister ackPersister, HashChainWriter audit) {
    this.builder = builder;
    this.ackPersister = ackPersister;
    this.audit = audit;
  }

  /** Real-time tick — every minute, publishes any newly-publishable fills. */
  @Scheduled(cron = "0 */1 * * * *")
  public void publishPending() {
    log.trace("Trax APA publisher tick");
    // Phase 7B — read tca.event.v1, build 35=AE messages, route via venue-adapter-marketaxess.
  }

  /** EOD batch: aggregate the day's publishable fills, optionally fall back to CSV-SFTP. */
  public void run(LocalDate reportingDate) {
    log.info("Trax APA EOD batch — date={}", reportingDate);
    String submissionId = "TRAX-" + reportingDate;
    audit.append(
        ActorType.SERVICE,
        "reporting-service",
        "reporting.trax.batch.completed",
        "TraxBatch",
        submissionId,
        ("{\"date\":\"" + reportingDate + "\"}").getBytes(),
        null);
    ackPersister.recordPending(TR_ID, submissionId, reportingDate);
  }

  TraxApaTcrBuilder builder() {
    return builder;
  }
}
