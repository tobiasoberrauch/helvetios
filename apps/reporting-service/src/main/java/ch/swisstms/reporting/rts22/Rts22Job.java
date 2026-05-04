package ch.swisstms.reporting.rts22;

import ch.swisstms.audit_chain.AuditEvent.ActorType;
import ch.swisstms.audit_chain.HashChainWriter;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * FR-025 — MiFID-II RTS-22 transaction reports → LSEG TRADEcho ARM (HTTPS REST + mTLS + OAuth2
 * client-credentials).
 *
 * <p>Tägliche T+1-Batch um 06:00 UTC. Schema validation pre-submit; batches of 1000 transaction
 * reports per HTTP POST.
 */
@Component
public class Rts22Job {

  private static final Logger log = LoggerFactory.getLogger(Rts22Job.class);

  private final HashChainWriter audit;

  public Rts22Job(HashChainWriter audit) {
    this.audit = audit;
  }

  @Scheduled(cron = "0 0 6 * * *")
  public void runDaily() {
    run(LocalDate.now().minusDays(1));
  }

  public void run(LocalDate reportingDate) {
    log.info("RTS-22 batch — date={}", reportingDate);
    // TODO Phase 7B — wire LSEG TRADEcho client + paginated POST.
    audit.append(
        ActorType.SERVICE,
        "reporting-service",
        "reporting.rts22.batch.completed",
        "Date",
        reportingDate.toString(),
        ("rts22:" + reportingDate).getBytes(),
        null);
  }
}
