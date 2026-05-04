package ch.swisstms.venue.refinitiv;

import ch.swisstms.domain.health.HealthSnapshot;
import ch.swisstms.domain.marketdata.MarketDataTick;
import ch.swisstms.domain.marketdata.SubscriptionRequest;
import ch.swisstms.domain.ports.VendorAdapterPort;
import ch.swisstms.time_sync.RegulatoryClock;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import org.springframework.stereotype.Component;

/**
 * Refinitiv (LSEG) Adapter — Phase 8 Skeleton.
 *
 * <p>Echte Implementierung verlangt das EMA RTSDK 3.7.x JAR und einen DACS-Permission-Server.
 * OmmConsumer für L1/L2-Subscriptions (MarketPrice, MarketByOrder); ETA / RSSL für
 * Low-Level-Throughput.
 *
 * <p>Wichtig: jede AppId pro Konsument einzigartig (Constitution V).
 */
@Component
public class RefinitivVendorAdapter implements VendorAdapterPort {

  private static final String VENDOR_ID = "REFINITIV";
  private final SubmissionPublisher<MarketDataTick> publisher = new SubmissionPublisher<>();

  @Override
  public Flow.Publisher<MarketDataTick> marketData(SubscriptionRequest req) {
    // TODO Phase 14 — wire OmmConsumer
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
        "EMA not yet wired (Phase 14)");
  }

  @Override
  public String vendorId() {
    return VENDOR_ID;
  }
}
