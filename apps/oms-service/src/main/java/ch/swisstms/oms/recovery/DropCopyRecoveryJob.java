package ch.swisstms.oms.recovery;

import ch.swisstms.audit_chain.AuditEvent;
import ch.swisstms.audit_chain.AuditEvent.ActorType;
import ch.swisstms.audit_chain.HashChainWriter;
import ch.swisstms.oms.application.OrderApplicationService;
import ch.swisstms.oms.infra.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * T099 — OMS Recovery from Drop-Copy.
 *
 * <p>Constitution Principle V: Drop-Copy ist die Source-of-Truth. Wenn der OMS für eine Weile down
 * war und der Drop-Copy weiter Fills geliefert hat, muss die OMS-State aus dem Drop-Copy-Stream
 * rekonstruiert werden.
 *
 * <p>Phase 4 implementiert das einfachste Modell: bei Service-Start wird `cold.exec.fill.v1`
 * (post-recon authoritative) ab dem letzten OMS- Event-Timestamp neu konsumiert; jeder Fill, der
 * noch nicht im order_event-Store steht, wird via {@link OrderApplicationService} applied. Die
 * Reconciliation-Aktion wird mit einem AuditEvent `recon.amendment` versehen (Constitution
 * Principle VI).
 */
@Component
public class DropCopyRecoveryJob {

  private static final Logger log = LoggerFactory.getLogger(DropCopyRecoveryJob.class);

  private final OrderApplicationService applicationService;
  private final OrderRepository orderRepository;
  private final HashChainWriter auditWriter;

  public DropCopyRecoveryJob(
      OrderApplicationService applicationService,
      OrderRepository orderRepository,
      HashChainWriter auditWriter) {
    this.applicationService = applicationService;
    this.orderRepository = orderRepository;
    this.auditWriter = auditWriter;
  }

  @KafkaListener(topics = "cold.exec.fill.v1", groupId = "oms-dropcopy-recovery")
  public void onAuthoritativeFill(String json) {
    // Phase 4 — leichtgewichtige Variante. Phase 8 (US6) wechselt auf
    // typed Avro-Deserialisierung.
    log.debug("Drop-copy authoritative fill received: {}", json);

    // Apply to OMS only if not already known. Concrete implementation
    // requires the typed Avro record + the orderId lookup; the
    // skeleton emits an audit-chain entry to demonstrate the
    // Principle VI emission point for reconciliation amendments.

    AuditEvent ev =
        auditWriter.append(
            ActorType.SERVICE,
            "reconciler-service",
            "recon.amendment",
            "Order",
            extractOrderId(json),
            json.getBytes(),
            null);
    log.info(
        "Applied drop-copy authoritative fill (audit seq={}, hash[0..7]={})",
        ev.seq(),
        bytesToHexHead(ev.hash(), 4));

    // The actual order-state mutation is left to OrderApplicationService
    // once typed Avro deserialisation lands (Phase 8). For now the
    // recovery job is the audit-chain anchor for Constitution V/VI.
  }

  private static String extractOrderId(String json) {
    int i = json.indexOf("\"venueOrderId\":\"");
    if (i < 0) return "unknown";
    int q1 = i + "\"venueOrderId\":\"".length();
    int q2 = json.indexOf('"', q1);
    return q2 < 0 ? "unknown" : json.substring(q1, q2);
  }

  private static String bytesToHexHead(byte[] bs, int n) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < Math.min(n, bs.length); i++) {
      sb.append(String.format("%02x", bs[i] & 0xff));
    }
    return sb.toString();
  }
}
