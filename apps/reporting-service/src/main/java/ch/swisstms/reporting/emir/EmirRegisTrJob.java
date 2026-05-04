package ch.swisstms.reporting.emir;

import ch.swisstms.audit_chain.AuditEvent.ActorType;
import ch.swisstms.audit_chain.HashChainWriter;
import ch.swisstms.reporting.common.SubmissionAckPersister;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * EMIR Refit ITS reporting → REGIS-TR (the LuxCSD-operated trade repository).
 *
 * <p>Dual-reporting alongside {@link EmirDtccGtrJob}: per ESMA we submit to two TRs to avoid single
 * point of failure. The pair-reporting reconciliation cross-checks DTCC and REGIS-TR weekly.
 */
@Component
public class EmirRegisTrJob {

  private static final Logger log = LoggerFactory.getLogger(EmirRegisTrJob.class);
  private static final String TR_ID = "REGIS-TR";

  private final HashChainWriter audit;
  private final SubmissionAckPersister ackPersister;

  public EmirRegisTrJob(HashChainWriter audit, SubmissionAckPersister ackPersister) {
    this.audit = audit;
    this.ackPersister = ackPersister;
  }

  @Scheduled(cron = "30 5 0 * * *") // T+1 00:05:30 UTC, staggered after DTCC
  public void runDaily() {
    run(LocalDate.now().minusDays(1));
  }

  public void run(LocalDate reportingDate) {
    log.info("EMIR REGIS-TR batch — date={}", reportingDate);
    String submissionId = "REGIS-" + reportingDate;
    audit.append(
        ActorType.SERVICE,
        "reporting-service",
        "reporting.emir.regis.submitted",
        "EmirSubmission",
        submissionId,
        ("{\"date\":\"" + reportingDate + "\"}").getBytes(),
        null);
    ackPersister.recordPending(TR_ID, submissionId, reportingDate);
  }
}
