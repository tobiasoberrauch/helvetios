package ch.swisstms.pretraderisk.cache;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * T235 — Kafka consumer of {@code warm.entitlements.limit-update.v1}.
 *
 * <p>Every entitlement diff published by {@code apps/entitlements-service} flips the matching
 * {@link RiskProfileCache} entry within one Kafka poll cycle. The cache is the hot-path lookup
 * during pre-trade risk evaluation; latency-tier discipline (Constitution II) — this consumer runs
 * in the warm tier, refreshing the off-heap cache that the Disruptor reads on the hot path.
 */
@Component
public class EntitlementCacheConsumer {

  private static final Logger log = LoggerFactory.getLogger(EntitlementCacheConsumer.class);
  private static final ObjectMapper JSON = new ObjectMapper();

  private final RiskProfileCache cache;

  public EntitlementCacheConsumer(RiskProfileCache cache) {
    this.cache = cache;
  }

  @KafkaListener(
      topics = "warm.entitlements.limit-update.v1",
      groupId = "pretrade-risk-gateway",
      concurrency = "1") // single-writer to keep cache thread-safe
  public void onEntitlementUpdate(String payload) {
    try {
      JsonNode node = JSON.readTree(payload);
      String subjectId = node.path("subjectId").asText("");
      if (subjectId.isEmpty()) {
        return;
      }
      // Phase 13B will extract concrete limit fields (fat-finger, daily-notional). For now we
      // log the diff and bump the cache entry — the actual cache mutation goes through
      // RiskProfileCache.refresh once the entitlement schema is finalised.
      log.info(
          "Entitlement update consumed for subject={} (granted={}, revoked={})",
          subjectId,
          node.path("granted").asInt(0),
          node.path("revoked").asInt(0));
      cache.markDirty(subjectId);
    } catch (Exception e) {
      log.error("Malformed entitlement update payload: {}", e.getMessage());
    }
  }
}
