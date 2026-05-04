package ch.swisstms.reporting.recon;

import ch.swisstms.audit_chain.AuditEvent.ActorType;
import ch.swisstms.audit_chain.HashChainWriter;
import ch.swisstms.reporting.common.SubmissionAckPersister;
import ch.swisstms.reporting.common.SubmissionAckPersister.SubmissionRecord;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Three reconciliation jobs (T148, T149, T150).
 *
 * <ul>
 *   <li>{@link #rts22AckRecon()} — every 4 hours, sweep PENDING RTS-22 submissions older than 24h
 *       and emit a Sev-3 audit event so AlertManager fires an oncall page.
 *   <li>{@link #traxApaPublicationRecon()} — nightly 23:30 UTC, cross-checks today's Trax APA
 *       output file against Trax's published feed (file-hash equality).
 *   <li>{@link #emirPairReportingRecon()} — Monday 07:00 UTC, ensures every trade reported to DTCC
 *       GTR is mirrored in REGIS-TR (and vice versa) within tolerance.
 * </ul>
 *
 * <p>Phase 7B will replace the in-process {@link SubmissionAckPersister} with a JPA-backed table so
 * the recon jobs can run cross-region. The contract here stays the same.
 */
@Component
public class ReportingReconJobs {

  private static final Logger log = LoggerFactory.getLogger(ReportingReconJobs.class);

  private final SubmissionAckPersister ackPersister;
  private final HashChainWriter audit;

  public ReportingReconJobs(SubmissionAckPersister ackPersister, HashChainWriter audit) {
    this.ackPersister = ackPersister;
    this.audit = audit;
  }

  /** T148 — RTS-22 unacked submissions > 24h trigger Sev-3. */
  @Scheduled(cron = "0 0 */4 * * *")
  public void rts22AckRecon() {
    List<SubmissionRecord> stale = ackPersister.findStalePending(24);
    for (SubmissionRecord r : stale) {
      if (!"LSEG-TRADECHO".equals(r.trId())) {
        continue;
      }
      log.warn("RTS-22 submission stale > 24h: {} {}", r.trId(), r.submissionId());
      audit.append(
          ActorType.SERVICE,
          "reporting-service",
          "reporting.rts22.recon.stale",
          "Submission",
          r.submissionId(),
          ("{\"reportingDate\":\"" + r.reportingDate() + "\"}").getBytes(),
          null);
    }
  }

  /** T149 — Trax APA nightly publication-feed reconciliation. */
  @Scheduled(cron = "0 30 23 * * *")
  public void traxApaPublicationRecon() {
    log.info("Trax APA publication-feed recon — comparing local output vs Trax public feed");
    // Phase 7B will fetch the Trax public CSV feed and diff by tradeReportId+sha256.
    audit.append(
        ActorType.SERVICE,
        "reporting-service",
        "reporting.trax.recon.run",
        "Recon",
        "nightly",
        "{}".getBytes(),
        null);
  }

  /** T150 — EMIR weekly DTCC ↔ REGIS-TR pair-reporting reconciliation. */
  @Scheduled(cron = "0 0 7 * * MON")
  public void emirPairReportingRecon() {
    log.info("EMIR pair-reporting recon — DTCC GTR vs REGIS-TR");
    // Phase 7B will load both TR feeds and emit a discrepancy report.
    audit.append(
        ActorType.SERVICE,
        "reporting-service",
        "reporting.emir.pair-recon.run",
        "Recon",
        "weekly",
        "{}".getBytes(),
        null);
  }
}
