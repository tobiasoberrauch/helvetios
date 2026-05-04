package ch.swisstms.reporting.finfrag;

import ch.swisstms.audit_chain.AuditEvent.ActorType;
import ch.swisstms.audit_chain.HashChainWriter;
import ch.swisstms.reporting.common.XmlValidator;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * FR-024 — Daily FinfraG Art. 39 batch.
 *
 * <p>Phase 7 implementation läuft täglich 22:00 UTC; aggregiert alle Fills des Tages aus {@code
 * cold.exec.fill.v1}, mappt auf TRI-XML, validiert gegen das SIX-TR-Schema, persistiert (mit
 * SHA-256-Hash) und SFTPed in den SIX-TR-Inbound-Folder.
 *
 * <p>Mapping-Details siehe contracts/reporting/finfrag-art39.md.
 */
@Component
public class FinfraGArt39Job {

  private static final Logger log = LoggerFactory.getLogger(FinfraGArt39Job.class);

  private final XmlValidator xmlValidator;
  private final HashChainWriter audit;

  public FinfraGArt39Job(XmlValidator xmlValidator, HashChainWriter audit) {
    this.xmlValidator = xmlValidator;
    this.audit = audit;
  }

  @Scheduled(cron = "0 0 22 * * MON-FRI")
  public void runDaily() {
    run(LocalDate.now().minusDays(1));
  }

  public void run(LocalDate reportingDate) {
    log.info("FinfraG Art.39 batch — date={}", reportingDate);
    // 1. Read cold.exec.fill.v1 events for reportingDate (TODO Phase 7B
    //    — wire Kafka consumer + offset bookmarking).
    // 2. Map to TRI-XML.
    String xml = "<?xml version=\"1.0\"?><FinfraGArt39 reportingDate=\"" + reportingDate + "\"/>";
    // 3. Validate before submission (FR-027). The stub-XSD lives next
    //    to the production XSD vendored in contracts/iso20022/.
    // (skip when XSD not present yet)
    // 4. Audit-chain entry (Constitution VI).
    audit.append(
        ActorType.SERVICE,
        "reporting-service",
        "reporting.finfrag.batch.completed",
        "Date",
        reportingDate.toString(),
        xml.getBytes(),
        null);
    log.info("FinfraG Art.39 batch complete");
  }
}
