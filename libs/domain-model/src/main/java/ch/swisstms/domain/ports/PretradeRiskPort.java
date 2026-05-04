package ch.swisstms.domain.ports;

import ch.swisstms.domain.client.ClientId;
import ch.swisstms.domain.order.Order;

/**
 * Pre-trade risk evaluator port. Hot-path; p99 < 50µs (SC-017). Constitution Principle II — must be
 * co-located with the inbound-fix-acceptor over Aeron IPC; calling this over network blows the
 * latency budget.
 */
public interface PretradeRiskPort {

  RiskDecision evaluate(Order order, ClientId clientId);

  sealed interface RiskDecision {
    long evaluationNanos();

    record Approved(long evaluationNanos) implements RiskDecision {}

    record Rejected(RejectReason reason, String detail, long evaluationNanos)
        implements RiskDecision {}
  }

  enum RejectReason {
    FAT_FINGER_NOTIONAL,
    FAT_FINGER_QUANTITY,
    MAX_ORDER_SIZE,
    DAILY_NOTIONAL_LIMIT,
    THROTTLE_PER_SECOND,
    THROTTLE_IN_FLIGHT,
    INSTRUMENT_RESTRICTED,
    KILL_SWITCH_TRIPPED,
    UNKNOWN_CLIENT,
    UNKNOWN_INSTRUMENT
  }
}
