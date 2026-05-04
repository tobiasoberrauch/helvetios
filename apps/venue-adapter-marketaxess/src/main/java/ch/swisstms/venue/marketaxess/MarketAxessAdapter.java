package ch.swisstms.venue.marketaxess;

import ch.swisstms.domain.execution.ExecutionReport;
import ch.swisstms.domain.health.HealthSnapshot;
import ch.swisstms.domain.health.LatencyTier;
import ch.swisstms.domain.marketdata.MarketDataTick;
import ch.swisstms.domain.marketdata.SubscriptionRequest;
import ch.swisstms.domain.order.CancelRequest;
import ch.swisstms.domain.order.Order;
import ch.swisstms.domain.order.OrderAck;
import ch.swisstms.domain.order.ReplaceRequest;
import ch.swisstms.domain.ports.VenueGatewayPort;
import ch.swisstms.time_sync.RegulatoryClock;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import org.springframework.stereotype.Component;

/**
 * MarketAxess Open Trading + Composite+ + Trax APA — Phase 9 Skeleton.
 *
 * <p>Drei Sub-Module:
 *
 * <ul>
 *   <li>Open Trading: All-to-All FIX-Channel
 *   <li>Composite+: Mid-Pricing via FIX MarketDataIncrementalRefresh
 *   <li>Trax APA: TradeCaptureReport(AE) → TradeCaptureReportAck(AR)
 * </ul>
 *
 * <p>Trax APA: tägliches Session-Reset 23:00–23:05 GMT; CSV-SFTP-Fallback bei &gt;3 GB (Error
 * GBX-010).
 */
@Component
public class MarketAxessAdapter implements VenueGatewayPort {

  private static final String VENUE_MIC = "MAEU";
  private final SubmissionPublisher<ExecutionReport> executions = new SubmissionPublisher<>();
  private final SubmissionPublisher<MarketDataTick> marketData = new SubmissionPublisher<>();

  @Override
  public CompletionStage<OrderAck> submitOrder(Order order) {
    return CompletableFuture.completedFuture(
        new OrderAck(order.orderId(), "MAX-" + UUID.randomUUID(), RegulatoryClock.nowBiz()));
  }

  @Override
  public CompletionStage<Void> cancelOrder(CancelRequest req) {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletionStage<Void> replaceOrder(ReplaceRequest req) {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public Flow.Publisher<ExecutionReport> executions() {
    return executions;
  }

  @Override
  public Flow.Publisher<MarketDataTick> marketData(SubscriptionRequest req) {
    return marketData;
  }

  @Override
  public HealthSnapshot health() {
    return new HealthSnapshot(
        VENUE_MIC,
        HealthSnapshot.Status.DISCONNECTED,
        RegulatoryClock.nowBiz(),
        0L,
        0L,
        "MarketAxess adapter — Phase 14 wiring");
  }

  @Override
  public String venueMic() {
    return VENUE_MIC;
  }

  @Override
  public LatencyTier tier() {
    return LatencyTier.WARM;
  }
}
