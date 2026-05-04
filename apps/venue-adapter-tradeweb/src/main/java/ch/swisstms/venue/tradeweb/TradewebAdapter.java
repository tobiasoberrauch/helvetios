package ch.swisstms.venue.tradeweb;

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
 * Tradeweb TradeXpress Adapter — Phase 9 Skeleton.
 *
 * <p>RFQ-Flow: QuoteRequest(R) → Quote(S) × N Dealer → NewOrderSingle(D) → ExecutionReport(8).
 * AiEX-Rules in {@link ch.swisstms.venue.tradeweb.aiex.AiexRuleEngine}.
 *
 * <p>Phase 14 — vollständige TradeXpress-Dialekt-Verkabelung gegen den realen Endpunkt.
 */
@Component
public class TradewebAdapter implements VenueGatewayPort {

  private static final String VENUE_MIC = "TWBT"; // Tradeweb MIC
  private final SubmissionPublisher<ExecutionReport> executions = new SubmissionPublisher<>();
  private final SubmissionPublisher<MarketDataTick> marketData = new SubmissionPublisher<>();

  @Override
  public CompletionStage<OrderAck> submitOrder(Order order) {
    // TODO Phase 14 — RFQ flow
    return CompletableFuture.completedFuture(
        new OrderAck(order.orderId(), "TW-" + UUID.randomUUID(), RegulatoryClock.nowBiz()));
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
        "Tradeweb TradeXpress adapter — Phase 14 wiring");
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
