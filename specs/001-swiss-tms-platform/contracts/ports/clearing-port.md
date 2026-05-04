# Port: ClearingPort

**Module**: `libs/domain-model/`
**Implemented by**: `apps/clearing-adapter-eurex/`, `apps/clearing-adapter-six/`, `apps/clearing-adapter-otcc/`.
**Consumed by**: `apps/oms-service/` (post-trade processing), `apps/reporting-service/` (regulatory reports), `apps/position-keeping/`.

```java
package ch.swisstms.domain.ports;

public interface ClearingPort {
    /** Submit a fill for clearing/novation; returns when the CCP acknowledges capture. */
    CompletionStage<ClearingTradeAck> submitForClearing(Fill fill);

    /** Stream of clearing-trade lifecycle events (PENDING_NOVATION → NOVATED, …). */
    Flow.Publisher<ClearingTradeEvent> clearingEvents();

    /** Stream of margin calls from this CCP. */
    Flow.Publisher<MarginCall> marginCalls();

    /** Pull the daily Common Report Engine reports (Eurex CRE) or equivalent. */
    CompletionStage<List<ClearingReport>> pullDailyReports(LocalDate date);

    HealthSnapshot health();
    String ccpId();
}
```

## Semantics

- Eurex implementation transports FIXML over AMQP 1.0 (Apache Qpid JMS) with Spring `CachingConnectionFactory` and per-thread JMS sessions.
- SIX x-clear implementation uses ISO 20022 (sese.023, sese.025) over SECOM.
- OTCC implementation uses the OTCC-SHCH proxy interface for Swap Connect Northbound.
- All implementations write the immutable confirmation (FIXML, FpML, ISO 20022) to S3 WORM as the regulatory record.
