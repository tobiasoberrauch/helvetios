package ch.swisstms.venue.bloomberg;

import ch.swisstms.domain.health.HealthSnapshot;
import ch.swisstms.domain.marketdata.MarketDataTick;
import ch.swisstms.domain.marketdata.SubscriptionRequest;
import ch.swisstms.domain.ports.VendorAdapterPort;
import ch.swisstms.time_sync.RegulatoryClock;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import org.springframework.stereotype.Component;

/**
 * Bloomberg-Adapter — Phase 8 Skeleton.
 *
 * <p>Echte Implementierung verlangt das proprietäre BLPAPI v3 JAR aus dem Bloomberg-Member-Portal.
 * Drei Deployment-Flavours:
 *
 * <ul>
 *   <li>Desktop API ({@code localhost:8194}) — Dev only.
 *   <li>SAPI — UAT.
 *   <li>B-PIPE — Production mit EMRS-Entitlement-Sync.
 * </ul>
 *
 * <p>EMSX-API: Service {@code //blp/emapisvc} (prod) / {@code //blp/emapisvc_beta} (UAT). Requests:
 * CreateOrderAndRouteEx, RouteEx, ModifyRouteEx, CancelRouteEx, AssignTrader. Subscriptions:
 * OrderSubscription, RouteSubscription.
 *
 * <p>Phase 14 vendoring durch {@code infra/maven-mirror/}.
 */
@Component
public class BloombergVendorAdapter implements VendorAdapterPort {

  private static final String VENDOR_ID = "BLOOMBERG";
  private final SubmissionPublisher<MarketDataTick> publisher = new SubmissionPublisher<>();

  @Override
  public Flow.Publisher<MarketDataTick> marketData(SubscriptionRequest req) {
    // TODO Phase 14 — wire BLPAPI Subscription against //blp/mktdata
    return publisher;
  }

  @Override
  public HealthSnapshot health() {
    return new HealthSnapshot(
        VENDOR_ID,
        HealthSnapshot.Status.DISCONNECTED,
        RegulatoryClock.nowBiz(),
        0L,
        0L,
        "BLPAPI not yet wired (Phase 14)");
  }

  @Override
  public String vendorId() {
    return VENDOR_ID;
  }
}
