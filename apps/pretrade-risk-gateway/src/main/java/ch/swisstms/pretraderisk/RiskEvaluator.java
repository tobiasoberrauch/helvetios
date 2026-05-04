package ch.swisstms.pretraderisk;

import ch.swisstms.domain.client.ClientId;
import ch.swisstms.domain.order.Order;
import ch.swisstms.domain.ports.PretradeRiskPort;
import ch.swisstms.pretraderisk.cache.RiskProfileCache;
import ch.swisstms.time_sync.MonotonicClock;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Hot-path single-writer Risk Evaluator.
 *
 * <p>Constitution Principle II — wird per Aeron-IPC vom inbound-fix- acceptor aufgerufen. p99 <
 * 50µs (SC-017).
 *
 * <p>Phase 13 — Skeleton mit echten Limit-Checks. Phase 16 hardening + JMH-Benchmarks gegen
 * JEP-Backed-Disruptor.
 */
@Component
public class RiskEvaluator implements PretradeRiskPort {

  private final RiskProfileCache cache;

  public RiskEvaluator(RiskProfileCache cache) {
    this.cache = cache;
  }

  @Override
  public RiskDecision evaluate(Order order, ClientId clientId) {
    long start = MonotonicClock.nanos();
    var profile = cache.lookup(clientId);
    if (profile == null) {
      return new RiskDecision.Rejected(
          RejectReason.UNKNOWN_CLIENT, "client not in cache", MonotonicClock.durationNanos(start));
    }
    if (profile.killSwitchTripped()) {
      return new RiskDecision.Rejected(
          RejectReason.KILL_SWITCH_TRIPPED,
          "kill-switch active for client " + clientId,
          MonotonicClock.durationNanos(start));
    }
    BigDecimal qty = order.orderQty().toBigDecimal();
    if (profile.fatFingerQuantity() != null && qty.compareTo(profile.fatFingerQuantity()) > 0) {
      return new RiskDecision.Rejected(
          RejectReason.FAT_FINGER_QUANTITY,
          "qty " + qty + " > " + profile.fatFingerQuantity(),
          MonotonicClock.durationNanos(start));
    }
    if (!order.price().isMarket() && profile.fatFingerNotional() != null) {
      BigDecimal notional = qty.multiply(order.price().toBigDecimal());
      if (notional.compareTo(profile.fatFingerNotional()) > 0) {
        return new RiskDecision.Rejected(
            RejectReason.FAT_FINGER_NOTIONAL,
            "notional " + notional + " > " + profile.fatFingerNotional(),
            MonotonicClock.durationNanos(start));
      }
    }
    // Phase 13 — minimal subset; remaining limits (throttle, daily-notional,
    // restricted-instruments) added after the JMH-bench shows the budget.
    return new RiskDecision.Approved(MonotonicClock.durationNanos(start));
  }
}
