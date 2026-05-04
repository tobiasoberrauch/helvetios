# Port: VendorAdapterPort

**Module**: `libs/domain-model/`
**Implemented by**: `apps/venue-adapter-bloomberg/`, `apps/venue-adapter-refinitiv/` (and any future market-data-vendor adapters).

For market-data-only vendors (Refinitiv, parts of Bloomberg) the order-routing methods of `VenueGatewayPort` are not applicable; this narrower port surfaces only the subscription side. Adapters that act as both vendor and venue (Bloomberg EMSX) implement both ports.

```java
package ch.swisstms.domain.ports;

public interface VendorAdapterPort {
    Flow.Publisher<MarketDataTick> marketData(SubscriptionRequest req);
    Flow.Publisher<ReferenceDataUpdate> referenceData(ReferenceDataRequest req);
    HealthSnapshot health();
    String vendorId();
}
```

## Semantics

- `marketData` enforces entitlement checks (via `EntitlementPort`) before the first tick is delivered.
- `referenceData` is used by `apps/reference-data-service/` for nightly DL pulls and intraday refreshes.
- The adapter normalises vendor-specific symbology into domain `InstrumentId` (ISIN + MIC).
