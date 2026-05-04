package ch.swisstms.venue.refinitiv;

import ch.swisstms.domain.health.HealthSnapshot;
import ch.swisstms.domain.marketdata.MarketDataTick;
import ch.swisstms.domain.marketdata.SubscriptionRequest;
import ch.swisstms.domain.ports.VendorAdapterPort;
import ch.swisstms.time_sync.RegulatoryClock;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Refinitiv EMA OmmConsumer adapter (T172 / FR-019).
 *
 * <p>Constitution Principle I — the EMA-specific {@code OmmConsumer}, {@code OmmIterable}, {@code
 * FieldEntry} types live ONLY in this class. Outbound contract is the canonical {@link
 * MarketDataTick}.
 *
 * <p>Phase 8 ships the dispatch surface; the actual EMA consumer wiring requires the {@code
 * com.refinitiv.ema:ema:3.7.x} JAR which is not yet on Maven Central — the JAR will be mirrored
 * into {@code infra/maven-mirror/} during Phase 14. Until then this class accepts ticks via {@link
 * #onTickFromMock(MarketDataTick)} so the integration path can be exercised end-to-end against
 * {@code mocks/refinitiv-ema-provider/}.
 */
@Component
@Primary
public class RefinitivEmaAdapter implements VendorAdapterPort {

  private static final Logger log = LoggerFactory.getLogger(RefinitivEmaAdapter.class);
  private static final String VENDOR_ID = "REFINITIV-EMA";

  private final SubmissionPublisher<MarketDataTick> publisher = new SubmissionPublisher<>();
  private final RefinitivOpenDacsBridge dacs;
  private final AtomicLong delivered = new AtomicLong();

  public RefinitivEmaAdapter(RefinitivOpenDacsBridge dacs) {
    this.dacs = dacs;
  }

  @Override
  public Flow.Publisher<MarketDataTick> marketData(SubscriptionRequest req) {
    boolean anyDenied =
        req.instruments().stream().anyMatch(i -> !dacs.isPermitted(req.subscriberId(), i));
    if (anyDenied) {
      log.warn(
          "Refinitiv subscription denied for subject={} ({} instrument(s) — DACS PE-code mismatch)",
          req.subscriberId(),
          req.instruments().size());
      // Empty stream — the SubscriptionManager DENIED state machine handles cleanup.
      return new SubmissionPublisher<>();
    }
    return publisher;
  }

  /** Called by the OmmConsumer onUpdateMsg / onRefreshMsg callbacks (Phase 14) and by tests. */
  public void onTickFromMock(MarketDataTick tick) {
    publisher.submit(tick);
    delivered.incrementAndGet();
  }

  @Override
  public HealthSnapshot health() {
    return new HealthSnapshot(
        VENDOR_ID,
        HealthSnapshot.Status.CONNECTED,
        RegulatoryClock.nowBiz(),
        delivered.get(),
        0L,
        "EMA dispatcher (mock-backed)");
  }

  @Override
  public String vendorId() {
    return VENDOR_ID;
  }
}
