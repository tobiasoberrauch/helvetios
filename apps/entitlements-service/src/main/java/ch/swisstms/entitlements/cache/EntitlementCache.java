package ch.swisstms.entitlements.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Local entitlement cache + Kafka warm-tier publisher (T167).
 *
 * <p>Caches the merged DACS + EMRS permission set per subject for {@code refreshInterval} (default
 * 30 s) and publishes every change to {@code warm.entitlements.limit-update.v1} so other services
 * (OMS, EMS, surveillance) see revocations without polling.
 *
 * <p>FR-021 — entitlement-source unavailability fails closed: if the upstream sources can't refresh
 * the cache, {@link #snapshot(String)} returns an empty set after the entry expires.
 */
@Component
public class EntitlementCache {

  private static final Logger log = LoggerFactory.getLogger(EntitlementCache.class);
  private static final String TOPIC = "warm.entitlements.limit-update.v1";
  private static final Duration TTL = Duration.ofSeconds(30);

  public record CachedEntitlement(Set<String> permissions, Instant expiresAt) {
    public boolean isExpired() {
      return Instant.now().isAfter(expiresAt);
    }
  }

  private final Map<String, CachedEntitlement> cache = new ConcurrentHashMap<>();
  private final KafkaTemplate<String, String> kafka;

  public EntitlementCache(KafkaTemplate<String, String> kafka) {
    this.kafka = kafka;
  }

  public void put(String subjectId, Set<String> permissions) {
    Set<String> previous =
        cache.containsKey(subjectId) ? cache.get(subjectId).permissions() : Set.of();
    cache.put(subjectId, new CachedEntitlement(Set.copyOf(permissions), Instant.now().plus(TTL)));
    if (!previous.equals(permissions)) {
      Set<String> revoked =
          previous.stream()
              .filter(p -> !permissions.contains(p))
              .collect(java.util.stream.Collectors.toSet());
      Set<String> granted =
          permissions.stream()
              .filter(p -> !previous.contains(p))
              .collect(java.util.stream.Collectors.toSet());
      String event =
          "{\"subjectId\":\""
              + subjectId
              + "\",\"granted\":"
              + revoked.size()
              + ",\"revoked\":"
              + revoked.size()
              + ",\"granted_keys\":"
              + asJsonArray(granted)
              + ",\"revoked_keys\":"
              + asJsonArray(revoked)
              + "}";
      kafka.send(TOPIC, subjectId, event);
      log.info(
          "Entitlement diff published for {}: +{} / -{}",
          subjectId,
          granted.size(),
          revoked.size());
    }
  }

  public Set<String> snapshot(String subjectId) {
    CachedEntitlement c = cache.get(subjectId);
    if (c == null || c.isExpired()) {
      return Set.of();
    }
    return c.permissions();
  }

  /** Periodically expire stale cache entries — fail-closed safety net. */
  @Scheduled(fixedDelay = 5_000)
  public void evictStale() {
    cache.entrySet().removeIf(e -> e.getValue().isExpired());
  }

  private static String asJsonArray(Set<String> values) {
    if (values.isEmpty()) {
      return "[]";
    }
    StringBuilder sb = new StringBuilder("[");
    boolean first = true;
    for (String v : values) {
      if (!first) {
        sb.append(',');
      }
      sb.append('"').append(v.replace("\"", "\\\"")).append('"');
      first = false;
    }
    return sb.append(']').toString();
  }
}
