package ch.swisstms.venue.eurex;

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
 * Eurex T7 ETI Adapter — Phase 15 Skeleton.
 *
 * <p>T7 ETI ist ein binäres Protokoll (xsd/c-header-bundle aus Eurex Member Section publiziert).
 * Phase 15 ships die Adapter-Struktur; Phase 16 hardening verkabelt den vollen ETI-Codec und die
 * Aeron- Hot-Path-Integration.
 *
 * <p>FIX-Gateway-Fallback via QuickFIX/J 4.2/4.4 für Notfall-Routing.
 */
@Component
public class EurexT7Adapter implements VenueGatewayPort {

  private static final String VENUE_MIC = "XEUR";
  private final SubmissionPublisher<ExecutionReport> executions = new SubmissionPublisher<>();
  private final SubmissionPublisher<MarketDataTick> marketData = new SubmissionPublisher<>();

  @Override
  public CompletionStage<OrderAck> submitOrder(Order order) {
    return CompletableFuture.completedFuture(
        new OrderAck(order.orderId(), "EUREX-" + UUID.randomUUID(), RegulatoryClock.nowBiz()));
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
        "T7 ETI binary — Phase 16 hardening");
  }

  @Override
  public String venueMic() {
    return VENUE_MIC;
  }

  @Override
  public LatencyTier tier() {
    return LatencyTier.HOT;
  }
}
