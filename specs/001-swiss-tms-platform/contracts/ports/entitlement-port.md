# Port: EntitlementPort

**Module**: `libs/security/` (interface) + `apps/entitlements-service/` (default implementation).
**Consumed by**: `apps/market-data-service/`, `apps/inbound-fix-acceptor/` (for client onboarding lookups), `apps/oms-service/` (for trader role / kill-switch checks).

```java
package ch.swisstms.security.entitlements;

public interface EntitlementPort {
    /** Synchronous check (cached, hot path). */
    EntitlementDecision checkMarketData(SubjectId subject, InstrumentId instrument, Level level);

    /** Synchronous check for order entry. */
    EntitlementDecision checkOrderEntry(SubjectId subject, InstrumentId instrument, AssetClass assetClass);

    /** Snapshot of the kill-switch state for a scope. */
    KillSwitchState killSwitchFor(KillScope scope);

    /** Reactive stream of kill-switch trip events. */
    Flow.Publisher<KillSwitchEvent> killSwitchEvents();

    /** Trip the kill-switch (4-eyes enforced server-side). */
    CompletionStage<Void> tripKillSwitch(KillScope scope, UserId tripper, String reason);

    /** Reset (4-eyes: must be a different user from the tripper). */
    CompletionStage<Void> resetKillSwitch(KillScope scope, UserId resetter);
}

public sealed interface EntitlementDecision {
    record Granted() implements EntitlementDecision {}
    record Denied(DenyReason reason) implements EntitlementDecision {}
}
```

## Semantics

- `checkMarketData` and `checkOrderEntry` MUST return in < 1µs (cached lookup); a cache miss falls through to the upstream entitlement source (DACS / Bloomberg EMRS / internal).
- Cache refresh cadence default: 24 hours (configurable).
- On entitlement source unavailability, the platform fails closed (FR-021's documented invariant).
- Kill-switch reset enforces a different `UserId` from the tripper (4-eyes).
