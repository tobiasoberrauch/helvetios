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
 * EMIR Refit ITS reporting → DTCC GTR (Global Trade Repository).
 *
 * <p>T+1 batch at 00:00:30 UTC. We submit derivative trade reports per ESMA EMIR Refit XSD; DTCC
 * sends back NACK / ACK over the same secure file gateway. Lifecycle event types
 * (NEWT/MODI/CORR/EROR/EARL/TRAD) flow through the same job because all share the schema envelope.
 */
@Component
public class EmirDtccGtrJob {

  private static final Logger log = LoggerFactory.getLogger(EmirDtccGtrJob.class);
  private static final String TR_ID = "DTCC-GTR";

  private final HashChainWriter audit;
  private final SubmissionAckPersister ackPersister;

  public EmirDtccGtrJob(HashChainWriter audit, SubmissionAckPersister ackPersister) {
    this.audit = audit;
    this.ackPersister = ackPersister;
  }

  @Scheduled(cron = "30 0 0 * * *") // T+1 00:00:30 UTC
  public void runDaily() {
    run(LocalDate.now().minusDays(1));
  }

  public void run(LocalDate reportingDate) {
    log.info("EMIR DTCC GTR batch — date={}", reportingDate);
    // Phase 7B will wire the ESMA EMIR Refit XSD generator. Today this audit-logs and persists a
    // synthetic submission acknowledgement so the recon flow can be tested end-to-end.
    String submissionId = "DTCC-" + reportingDate;
    audit.append(
        ActorType.SERVICE,
        "reporting-service",
        "reporting.emir.dtcc.submitted",
        "EmirSubmission",
        submissionId,
        ("{\"date\":\"" + reportingDate + "\"}").getBytes(),
        null);
    ackPersister.recordPending(TR_ID, submissionId, reportingDate);
  }
}
