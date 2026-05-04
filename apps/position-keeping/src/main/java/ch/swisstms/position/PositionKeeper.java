package ch.swisstms.position;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * T304 — Position keeper.
 *
 * <p>Consumes {@code cold.exec.fill.v1} and maintains per-{client, instrument} positions.
 * Idempotent on {@code executionId} so a retried fill does not double-count.
 *
 * <p>Phase 16 swaps the in-memory store for a Postgres-backed table with optimistic locking; the
 * API surface ({@link #position}, {@link #applyFill}) stays unchanged.
 */
@Component
public class PositionKeeper {

  private static final Logger log = LoggerFactory.getLogger(PositionKeeper.class);
  private static final ObjectMapper JSON = new ObjectMapper();

  public record PositionKey(String clientId, String isin, String mic) {}

  public record Position(BigDecimal quantity, BigDecimal averagePriceCcyAmount) {}

  private final Map<PositionKey, Position> positions = new ConcurrentHashMap<>();
  private final Set<String> seenExecutions = ConcurrentHashMap.newKeySet();

  @KafkaListener(topics = "cold.exec.fill.v1", groupId = "position-keeping")
  public void onFill(String payload) {
    try {
      JsonNode n = JSON.readTree(payload);
      String execId = n.path("executionId").asText("");
      if (execId.isEmpty() || !seenExecutions.add(execId)) {
        return;
      }
      String clientId = n.path("clientId").asText("");
      String isin = n.path("isin").asText("");
      String mic = n.path("mic").asText("");
      String side = n.path("side").asText("BUY");
      BigDecimal qty = new BigDecimal(n.path("qty").asText("0"));
      BigDecimal price = new BigDecimal(n.path("price").asText("0"));
      applyFill(new PositionKey(clientId, isin, mic), side, qty, price);
    } catch (Exception e) {
      log.error("PositionKeeper failed to handle fill: {}", e.getMessage());
    }
  }

  /**
   * Apply a single fill and recompute the running average price. Idempotency is the caller's job.
   */
  public Position applyFill(PositionKey key, String side, BigDecimal qty, BigDecimal price) {
    BigDecimal signed = "SELL".equalsIgnoreCase(side) ? qty.negate() : qty;
    return positions.compute(
        key,
        (k, existing) -> {
          if (existing == null) {
            return new Position(signed, price);
          }
          BigDecimal newQty = existing.quantity().add(signed);
          BigDecimal newAvg;
          if (newQty.signum() == 0) {
            newAvg = BigDecimal.ZERO;
          } else if (existing.quantity().signum() == 0
              || (existing.quantity().signum() != newQty.signum())) {
            // Side reversal — reset the running average to the latest fill price.
            newAvg = price;
          } else {
            BigDecimal weightedExisting =
                existing.quantity().abs().multiply(existing.averagePriceCcyAmount());
            BigDecimal weightedNew = qty.abs().multiply(price);
            newAvg =
                weightedExisting
                    .add(weightedNew)
                    .divide(newQty.abs(), 6, java.math.RoundingMode.HALF_UP);
          }
          return new Position(newQty, newAvg);
        });
  }

  public Position position(PositionKey key) {
    return positions.getOrDefault(key, new Position(BigDecimal.ZERO, BigDecimal.ZERO));
  }

  public Map<PositionKey, Position> snapshot() {
    return Collections.unmodifiableMap(positions);
  }
}
