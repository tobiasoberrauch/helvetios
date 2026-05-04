package ch.swisstms.reconciler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publisher für `warm.recon.mismatch.v1`. Triggert AlertManager-Regel `recon-mismatch.yml` (PR-100
 * / Phase 4 T100).
 */
@Component
public class MismatchPublisher {

  private static final Logger log = LoggerFactory.getLogger(MismatchPublisher.class);
  private final KafkaTemplate<String, String> kafka;

  public MismatchPublisher(KafkaTemplate<String, String> kafka) {
    this.kafka = kafka;
  }

  public void publish(ReconciliationDecision decision) {
    String payload;
    switch (decision) {
      case ReconciliationDecision.DropCopyOnly d ->
          payload = mismatchJson(d.key(), "DROPCOPY_ONLY", "drop-copy authoritative");
      case ReconciliationDecision.OmsOnly o ->
          payload = mismatchJson(o.key(), "OMS_ONLY", "investigate phantom OMS event");
      case ReconciliationDecision.FieldMismatch f ->
          payload =
              mismatchJson(
                  f.key(),
                  "FIELD_MISMATCH",
                  f.field() + "=" + f.dropCopyValue() + " (was " + f.omsValue() + ")");
      case ReconciliationDecision.Match ignored -> {
        return; // matches go to cold.exec.fill.v1, not mismatch
      }
    }
    log.warn("Reconciliation mismatch: {}", payload);
    kafka.send(ReconcilerTopology.TOPIC_RECON_MISMATCH, decision.key().asString(), payload);
  }

  private static String mismatchJson(JoinKey key, String type, String detail) {
    return String.format(
        "{\"key\":\"%s\",\"type\":\"%s\",\"detail\":\"%s\"}",
        key.asString(), type, detail.replace("\"", "\\\""));
  }
}
