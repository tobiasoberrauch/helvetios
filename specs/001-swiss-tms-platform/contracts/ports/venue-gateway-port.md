# Port: VenueGatewayPort

**Module**: `libs/domain-model/`
**Implemented by**: every `apps/venue-adapter-*` service.
**Consumed by**: `apps/oms-service/`, `apps/ems-service/`.

The single port through which the domain talks to all venues. The contract is intentionally minimal — submit / cancel / replace, an executions stream, a market-data stream, and a health snapshot. Any FIX, SBE, BLPAPI, EMA, Pixie, Puffin, or proxy concern lives **inside** the adapter that implements this interface.

```java
package ch.swisstms.domain.ports;

import ch.swisstms.domain.execution.ExecutionReport;
import ch.swisstms.domain.marketdata.MarketDataTick;
import ch.swisstms.domain.marketdata.SubscriptionRequest;
import ch.swisstms.domain.order.CancelRequest;
import ch.swisstms.domain.order.Order;
import ch.swisstms.domain.order.OrderAck;
import ch.swisstms.domain.order.ReplaceRequest;
import ch.swisstms.domain.health.HealthSnapshot;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public interface VenueGatewayPort {
    /**
     * Submit a new order to the venue.
     * @return CompletionStage that completes with the venue's first acknowledgement
     *         (or fails with a structured VenueRejection).
     */
    CompletionStage<OrderAck> submitOrder(Order order);

    /**
     * Cancel an outstanding order.
     */
    CompletionStage<Void> cancelOrder(CancelRequest req);

    /**
     * Replace (modify) an outstanding order.
     */
    CompletionStage<Void> replaceOrder(ReplaceRequest req);

    /**
     * The continuous stream of execution reports from this venue.
     * Subscribers MUST NOT block on this; it is delivered on the venue I/O thread.
     */
    Flow.Publisher<ExecutionReport> executions();

    /**
     * Subscribe to market data for one or more instruments.
     * @return a stream of ticks, in instrument and source order.
     */
    Flow.Publisher<MarketDataTick> marketData(SubscriptionRequest req);

    /**
     * Snapshot of session, sequence, and connectivity health.
     * Polled by ops dashboards and the health endpoint.
     */
    HealthSnapshot health();

    /**
     * The venue identifier (MIC) this gateway serves.
     */
    String venueMic();

    /**
     * The latency tier this gateway serves on (HOT / WARM / COLD).
     * Used by the OMS / EMS to choose between fan-out paths.
     */
    LatencyTier tier();
}
```

## Semantics

- All methods are **non-blocking** and return either `CompletionStage` or a `Flow.Publisher`.
- `submitOrder`'s `CompletionStage` completes with the **first** venue acknowledgement (e.g., `OrderAck` from FIX `ExecType=NEW`, or a SBE `OrderAck` message). Subsequent execution events arrive via `executions()`.
- `executions()` is a hot publisher; back-pressure is enforced by Aeron / Reactive Streams subscription.
- `marketData()` may return a back-pressured publisher; consumers MUST request demand explicitly.
- `health()` is synchronous and cheap; it MUST NOT perform any I/O.
- The `LatencyTier` returned by `tier()` constrains how the OMS / EMS may use this adapter (for example, a HOT-tier adapter may be invoked from the EMS hot path; a WARM-tier adapter may not).

## Invariants

- The adapter MUST translate venue-specific identifiers (FIX `ExecID`, SBE `ExecutionID`, BLPAPI `EMSX_SEQUENCE`) into domain `ExecutionId` consistently — the same venue identifier MUST always map to the same `ExecutionId`.
- The adapter MUST never expose venue-specific types (`quickfix.Message`, `bloomberg.api.Element`, …) to the caller.
- The adapter MUST surface session disconnects on `health()` immediately and on `executions()` as a documented `SessionLost` event.

## Test contract

Every adapter ships a Pact contract test (`tests/property/java/.../venue/<adapter>/PactVerifyTest.java`) that exercises:
- `submitOrder` happy path (NEW → ACK).
- `submitOrder` rejection.
- `cancelOrder` (cancel acknowledged).
- `cancelOrder` (cancel rejected because order already filled).
- `replaceOrder` (price replace).
- `executions()` partial-fill → full-fill sequence.
- `marketData()` subscribe → first tick → unsubscribe.
- Session disconnect during in-flight order; recovery; reconciliation.
