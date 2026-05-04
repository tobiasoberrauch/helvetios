package ch.swisstms.domain.ports;

import ch.swisstms.domain.health.HealthSnapshot;
import ch.swisstms.domain.marketdata.MarketDataTick;
import ch.swisstms.domain.marketdata.SubscriptionRequest;
import java.util.concurrent.Flow;

/**
 * For market-data-only vendors (Refinitiv pure, parts of Bloomberg) — the order-routing portion of
 * {@link VenueGatewayPort} does not apply. Adapters that act as both vendor and venue (Bloomberg
 * EMSX) implement both.
 */
public interface VendorAdapterPort {

  Flow.Publisher<MarketDataTick> marketData(SubscriptionRequest req);

  HealthSnapshot health();

  String vendorId();
}
