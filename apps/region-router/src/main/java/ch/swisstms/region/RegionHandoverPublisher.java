package ch.swisstms.region;

import ch.swisstms.audit_chain.AuditEvent.ActorType;
import ch.swisstms.audit_chain.HashChainWriter;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Cross-region handover signal publisher (T265).
 *
 * <p>Two Kafka topics drive the follow-the-sun cutover:
 *
 * <ul>
 *   <li>{@code warm.region.handover.signal.v1} — broadcast that a cutover is imminent ({@code
 *       openWindow=60s}), so consumers can drain in-flight orders.
 *   <li>{@code region.handover.cutover.v1} — the actual flip; consumers acknowledge by writing to
 *       the audit chain and stopping order acceptance for the outgoing region.
 * </ul>
 *
 * <p>Constitution Principle V — drop-copy is the source of truth. Reconciliation jobs in the
 * incoming region cross-check fills against the drop-copy stream before declaring the cutover
 * clean.
 */
@Component
public class RegionHandoverPublisher {

  private static final Logger log = LoggerFactory.getLogger(RegionHandoverPublisher.class);
  private static final String SIGNAL_TOPIC = "warm.region.handover.signal.v1";
  private static final String CUTOVER_TOPIC = "region.handover.cutover.v1";

  private final KafkaTemplate<String, String> kafka;
  private final HashChainWriter audit;

  public RegionHandoverPublisher(KafkaTemplate<String, String> kafka, HashChainWriter audit) {
    this.kafka = kafka;
    this.audit = audit;
  }

  public void announceHandover(String fromRegion, String toRegion, long openWindowSeconds) {
    String key = fromRegion + "->" + toRegion;
    String json =
        "{\"from\":\""
            + fromRegion
            + "\",\"to\":\""
            + toRegion
            + "\",\"announcedAt\":\""
            + Instant.now()
            + "\",\"openWindowSeconds\":"
            + openWindowSeconds
            + "}";
    kafka.send(SIGNAL_TOPIC, key, json);
    audit.append(
        ActorType.SERVICE,
        "region-router",
        "region.handover.signal",
        "Handover",
        key,
        json.getBytes(),
        null);
    log.info("Region handover signal {}", key);
  }

  public void executeCutover(String fromRegion, String toRegion) {
    String key = fromRegion + "->" + toRegion;
    String json =
        "{\"from\":\""
            + fromRegion
            + "\",\"to\":\""
            + toRegion
            + "\",\"cutoverAt\":\""
            + Instant.now()
            + "\"}";
    kafka.send(CUTOVER_TOPIC, key, json);
    audit.append(
        ActorType.SERVICE,
        "region-router",
        "region.handover.cutover",
        "Handover",
        key,
        json.getBytes(),
        null);
    log.info("Region cutover executed {}", key);
  }
}
