# Port: PretradeRiskPort

**Module**: `libs/pretrade-risk/`
**Implemented by**: `apps/pretrade-risk-gateway/` (canonical) and the SOR for outbound checks.
**Consumed by**: `apps/inbound-fix-acceptor/` (via Aeron IPC).

```java
package ch.swisstms.pretraderisk;

public interface PretradeRiskPort {
    /** Evaluate the risk profile against the given order; non-blocking, sub-50µs p99. */
    RiskDecision evaluate(Order order, ClientId clientId);
}

public sealed interface RiskDecision {
    record Approved(long evaluationNanos) implements RiskDecision {}
    record Rejected(RejectReason reason, String detail, long evaluationNanos) implements RiskDecision {}
}

public enum RejectReason {
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
```

## Semantics

- `evaluate` is the single hot-path entry point; all checks must complete in < 50µs p99 (SC-017).
- The implementation is single-writer (Disruptor) with off-heap Agrona maps for state.
- `Approved` includes the elapsed nanoseconds for OTel span attributes.
- `Rejected` carries a structured reason that the inbound-FIX-acceptor maps to the appropriate FIX `Reject(35=3)` or `BusinessMessageReject(35=j)`.

## Configuration / refresh

Risk profiles are loaded from the entitlements service at startup and refreshed via Kafka topic `warm.entitlements.limit-update.v1` with monotonic version numbers (last-version-wins on refresh).
