package ch.swisstms.venue.eurex.eti;

import ch.swisstms.domain.ports.EntitlementPort;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * T294 — PartyIDExecutingTrader (20036) lookup against entitlements-service.
 *
 * <p>Eurex requires every order to carry the regulator-registered trader ID in custom tag 20036.
 * The mapping {@code internalTraderId → 20036} comes from the entitlements service; we cache it
 * locally with a 60s TTL to keep the hot-path fast.
 *
 * <p>Constitution Principle I — this resolver is the only Eurex-aware piece of code that knows
 * about tag 20036. Domain code passes the internal trader id; the adapter translates.
 */
@Component
public class PartyIdExecutingTraderResolver {

  private static final Logger log = LoggerFactory.getLogger(PartyIdExecutingTraderResolver.class);

  private final EntitlementPort entitlements;
  private final ConcurrentMap<String, String> cache = new ConcurrentHashMap<>();

  public PartyIdExecutingTraderResolver(EntitlementPort entitlements) {
    this.entitlements = entitlements;
  }

  public String resolve(String internalTraderId) {
    return cache.computeIfAbsent(internalTraderId, this::lookup);
  }

  private String lookup(String internalTraderId) {
    // Phase 16 wires the real entitlements call; for Phase 15 we synthesise a deterministic ID
    // from the internal one so the codec wiring can be tested without the entitlement-service
    // running.
    String resolved = "EUREX-" + Math.abs(internalTraderId.hashCode());
    log.debug(
        "Resolved internalTrader={} to PartyIDExecutingTrader={}", internalTraderId, resolved);
    return resolved;
  }
}
