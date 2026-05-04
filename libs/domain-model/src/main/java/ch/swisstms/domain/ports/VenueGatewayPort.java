package ch.swisstms.domain.ports;

import ch.swisstms.domain.execution.ExecutionReport;
import ch.swisstms.domain.health.HealthSnapshot;
import ch.swisstms.domain.health.LatencyTier;
import ch.swisstms.domain.marketdata.MarketDataTick;
import ch.swisstms.domain.marketdata.SubscriptionRequest;
import ch.swisstms.domain.order.CancelRequest;
import ch.swisstms.domain.order.Order;
import ch.swisstms.domain.order.OrderAck;
import ch.swisstms.domain.order.ReplaceRequest;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

/**
 * Single port through which the domain talks to all venues.
 *
 * <p>Constitution Principle I — every {@code apps/venue-adapter-*} module implements exactly this
 * interface. Domain-side callers (OMS, EMS, market-data-service) never see venue-specific protocol
 * artefacts.
 */
public interface VenueGatewayPort {

  /**
   * Submit a new order. Completes with the venue's first acknowledgement. Subsequent execution
   * events arrive on {@link #executions()}.
   */
  CompletionStage<OrderAck> submitOrder(Order order);

  CompletionStage<Void> cancelOrder(CancelRequest req);

  CompletionStage<Void> replaceOrder(ReplaceRequest req);

  /**
   * Hot publisher of execution reports. Subscribers MUST NOT block; delivery happens on the venue
   * I/O thread or its Aeron-bridged peer.
   */
  Flow.Publisher<ExecutionReport> executions();

  /** Subscribe to market data. Back-pressured per Reactive Streams. */
  Flow.Publisher<MarketDataTick> marketData(SubscriptionRequest req);

  /** Cheap, synchronous, I/O-free health snapshot. */
  HealthSnapshot health();

  /** ISO 10383 MIC of the venue this gateway serves. */
  String venueMic();

  /**
   * The latency tier this gateway serves on. Used by the OMS / EMS to route into the correct path
   * (Constitution Principle II).
   */
  LatencyTier tier();
}
