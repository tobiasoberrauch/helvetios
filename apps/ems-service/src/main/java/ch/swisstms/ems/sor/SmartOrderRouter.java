package ch.swisstms.ems.sor;

import ch.swisstms.domain.order.Order;
import ch.swisstms.domain.ports.VenueGatewayPort;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Smart Order Router (T252).
 *
 * <p>Picks the best venue for a child order based on:
 *
 * <ol>
 *   <li>health — routers in {@link VenueGatewayPort#health()} status DEGRADED / DISCONNECTED are
 *       skipped;
 *   <li>routing-mode preference — DMA → primary venue, ALGO_WHEEL → first healthy candidate, CARE →
 *       no SOR (the human picks);
 *   <li>preferred-venue hint on the order ({@link Order#preferredVenue()}).
 * </ol>
 *
 * <p>Phase 13 ships the picker; the actual venue ranking lives in Phase 9 ({@link
 * ch.swisstms.ems.rfq.QuoteComparator}). This class is pure logic — it does not maintain state,
 * which keeps it testable without Spring.
 */
@Component
public class SmartOrderRouter {

  private static final Logger log = LoggerFactory.getLogger(SmartOrderRouter.class);

  public Optional<VenueGatewayPort> route(Order order, Map<String, VenueGatewayPort> available) {
    if (available.isEmpty()) {
      return Optional.empty();
    }
    // Priority 1 — instrument's primary MIC if it's online.
    String preferred = order.instrument().mic();
    VenueGatewayPort match = available.get(preferred);
    if (match != null && isHealthy(match)) {
      return Optional.of(match);
    }
    // Priority 2 — first healthy adapter (deterministic order).
    return available.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(Map.Entry::getValue)
        .filter(this::isHealthy)
        .findFirst();
  }

  /** Variant for parent orders that pre-resolved a candidate list. */
  public Optional<VenueGatewayPort> route(Order order, List<VenueGatewayPort> candidates) {
    return candidates.stream().filter(this::isHealthy).findFirst();
  }

  private boolean isHealthy(VenueGatewayPort v) {
    try {
      var status = v.health().status();
      boolean ok =
          status == ch.swisstms.domain.health.HealthSnapshot.Status.CONNECTED
              || status == ch.swisstms.domain.health.HealthSnapshot.Status.DEGRADED;
      if (!ok) {
        log.debug("SOR skipping {} — status {}", v.venueMic(), status);
      }
      return ok;
    } catch (Exception e) {
      log.warn("SOR health probe failed for {}", v.venueMic(), e);
      return false;
    }
  }
}
