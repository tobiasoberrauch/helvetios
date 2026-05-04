package ch.swisstms.marketdata.subscription;

import ch.swisstms.domain.instrument.InstrumentId;
import ch.swisstms.domain.marketdata.SubscriptionRequest.Level;
import ch.swisstms.domain.ports.EntitlementPort;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * FR-021 — Entitlement-Check vor Tick-Delivery.
 *
 * <p>State machine: REQUESTED → ENTITLED → STREAMING → STOPPED / DENIED. Bei
 * Entitlement-Source-Unavailability fail closed (Constitution).
 */
@Component
public class SubscriptionManager {

  private static final Logger log = LoggerFactory.getLogger(SubscriptionManager.class);

  private final EntitlementPort entitlements;
  private final ConcurrentHashMap<UUID, SubscriptionState> subscriptions =
      new ConcurrentHashMap<>();

  public SubscriptionManager(EntitlementPort entitlements) {
    this.entitlements = entitlements;
  }

  public UUID subscribe(String subjectId, InstrumentId instrument, Level level) {
    UUID id = UUID.randomUUID();
    try {
      EntitlementPort.EntitlementDecision decision =
          entitlements.checkMarketData(subjectId, instrument, level);
      switch (decision) {
        case EntitlementPort.EntitlementDecision.Granted ignored -> {
          subscriptions.put(id, new SubscriptionState(subjectId, instrument, State.STREAMING));
          log.info(
              "Subscription {} STREAMING ({} -> {}/{})",
              id,
              subjectId,
              instrument.isin(),
              instrument.mic());
        }
        case EntitlementPort.EntitlementDecision.Denied d -> {
          subscriptions.put(id, new SubscriptionState(subjectId, instrument, State.DENIED));
          log.warn("Subscription {} DENIED — {}", id, d.reason());
        }
      }
    } catch (Exception e) {
      // FR-021 — fail closed on entitlement-source unavailability.
      subscriptions.put(id, new SubscriptionState(subjectId, instrument, State.DENIED));
      log.error("Entitlement source unavailable, denying subscription {}", id, e);
    }
    return id;
  }

  public State stateOf(UUID id) {
    SubscriptionState s = subscriptions.get(id);
    return s == null ? null : s.state();
  }

  public void stop(UUID id) {
    SubscriptionState existing = subscriptions.get(id);
    if (existing != null) {
      subscriptions.put(
          id, new SubscriptionState(existing.subjectId(), existing.instrument(), State.STOPPED));
    }
  }

  /** Re-evaluate every active subscription against current entitlements. */
  public void refresh() {
    subscriptions.replaceAll(
        (id, s) -> {
          if (s.state() != State.STREAMING) return s;
          EntitlementPort.EntitlementDecision d =
              entitlements.checkMarketData(s.subjectId(), s.instrument(), Level.L1_TOP_OF_BOOK);
          return d instanceof EntitlementPort.EntitlementDecision.Denied
              ? new SubscriptionState(s.subjectId(), s.instrument(), State.STOPPED)
              : s;
        });
  }

  public enum State {
    REQUESTED,
    ENTITLED,
    STREAMING,
    STOPPED,
    DENIED
  }

  private record SubscriptionState(String subjectId, InstrumentId instrument, State state) {}
}
