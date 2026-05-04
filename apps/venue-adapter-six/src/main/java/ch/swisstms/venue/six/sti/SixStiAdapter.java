package ch.swisstms.venue.six.sti;

import ch.swisstms.domain.execution.ExecType;
import ch.swisstms.domain.execution.ExecutionId;
import ch.swisstms.domain.execution.LiquidityIndicator;
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
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * SIX Swiss Exchange STI venue adapter.
 *
 * <p>Phase 3 (US1) ships an in-process variant that talks directly to {@code mocks/six-mts-stub/}
 * via the FIX session port the mock exposes. This delivers the end-to-end roundtrip needed for
 * SC-001 (60-second `tilt up` to first roundtrip) without requiring a Bahnhof Co-Lo connection.
 *
 * <p>Phase 14 (Multi-Region) replaces the in-process flow with the production-shadow QuickFIX/J
 * initiator session against the real STI endpoint at SIX, with sequence-number persistence in the
 * `fix_session_state` Postgres table (libs/fix-codec).
 *
 * <p>Constitution Principle I — alle FIX-Tags und SIX-Eigenheiten leben in {@link
 * SixStiMessageMapper}; diese Klasse exponiert nur Domain-Typen.
 */
@Component
public class SixStiAdapter implements VenueGatewayPort {

  private static final Logger log = LoggerFactory.getLogger(SixStiAdapter.class);
  private static final String VENUE_MIC = "XSWX";

  private final SubmissionPublisher<ch.swisstms.domain.execution.ExecutionReport>
      executionsPublisher = new SubmissionPublisher<>();
  private final SubmissionPublisher<MarketDataTick> marketDataPublisher =
      new SubmissionPublisher<>();
  private final ConcurrentHashMap<UUID, Order> outstanding = new ConcurrentHashMap<>();
  private final AtomicLong execSeq = new AtomicLong();
  private final boolean simulateFills;

  public SixStiAdapter(@Value("${swisstms.six.simulate-fills:true}") boolean simulateFills) {
    this.simulateFills = simulateFills;
  }

  @Override
  public CompletionStage<OrderAck> submitOrder(Order order) {
    log.info(
        "SIX/STI submit: orderId={} clOrdId={} qty={} px={}",
        order.orderId(),
        order.clOrdId(),
        order.orderQty(),
        order.price());
    outstanding.put(order.orderId().value(), order);
    Instant now = RegulatoryClock.nowBiz();
    OrderAck ack = new OrderAck(order.orderId(), "SIX-" + execSeq.incrementAndGet(), now);

    // Hot venue would publish over Aeron; the in-process mock variant
    // emits a NEW ExecutionReport synchronously, then a FILL after a
    // simulated 100ms.
    executionsPublisher.submit(
        buildReport(
            order,
            ExecType.NEW,
            ch.swisstms.domain.price.Quantity.ZERO,
            order.price(),
            order.orderQty(),
            now));

    if (simulateFills) {
      // Simulate one partial fill, then full fill (matches SC-001 demo).
      new Thread(
              () -> {
                try {
                  Thread.sleep(50);
                  publishPartial(order);
                  Thread.sleep(50);
                  publishFinal(order);
                } catch (InterruptedException ignore) {
                  Thread.currentThread().interrupt();
                }
              },
              "six-mts-stub-filler-" + order.orderId().value())
          .start();
    }

    return CompletableFuture.completedFuture(ack);
  }

  private void publishPartial(Order order) {
    Instant now = RegulatoryClock.nowBiz();
    ch.swisstms.domain.price.Quantity halfQty =
        ch.swisstms.domain.price.Quantity.of(
            order
                .orderQty()
                .toBigDecimal()
                .divide(java.math.BigDecimal.valueOf(2), java.math.RoundingMode.HALF_EVEN));
    executionsPublisher.submit(
        buildReport(
            order,
            ExecType.PARTIAL_FILL,
            halfQty,
            order.price(),
            order.orderQty().minus(halfQty),
            now));
  }

  private void publishFinal(Order order) {
    Instant now = RegulatoryClock.nowBiz();
    ch.swisstms.domain.price.Quantity halfQty =
        ch.swisstms.domain.price.Quantity.of(
            order
                .orderQty()
                .toBigDecimal()
                .divide(java.math.BigDecimal.valueOf(2), java.math.RoundingMode.HALF_EVEN));
    executionsPublisher.submit(
        buildReport(
            order,
            ExecType.FILL,
            halfQty,
            order.price(),
            ch.swisstms.domain.price.Quantity.ZERO,
            now));
  }

  private ch.swisstms.domain.execution.ExecutionReport buildReport(
      Order order,
      ExecType execType,
      ch.swisstms.domain.price.Quantity qty,
      ch.swisstms.domain.price.Price px,
      ch.swisstms.domain.price.Quantity leaves,
      Instant bizTime) {
    long seq = execSeq.incrementAndGet();
    return new ch.swisstms.domain.execution.ExecutionReport(
        new ExecutionId(UUID.randomUUID()),
        "SIX-EXEC-" + seq,
        order.orderId(),
        execType,
        qty,
        px == null ? ch.swisstms.domain.price.Price.MARKET : px,
        order.orderQty().minus(leaves),
        leaves,
        px == null ? ch.swisstms.domain.price.Price.MARKET : px,
        LiquidityIndicator.ADD,
        VENUE_MIC,
        bizTime,
        bizTime);
  }

  @Override
  public CompletionStage<Void> cancelOrder(CancelRequest req) {
    log.info("SIX/STI cancel: orderId={}", req.orderId());
    Order order = outstanding.get(req.orderId().value());
    if (order != null) {
      executionsPublisher.submit(
          buildReport(
              order,
              ExecType.CANCELED,
              ch.swisstms.domain.price.Quantity.ZERO,
              order.price(),
              order.orderQty(),
              RegulatoryClock.nowBiz()));
    }
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletionStage<Void> replaceOrder(ReplaceRequest req) {
    log.info("SIX/STI replace: orderId={}", req.orderId());
    Order order = outstanding.get(req.orderId().value());
    if (order != null) {
      executionsPublisher.submit(
          buildReport(
              order,
              ExecType.REPLACED,
              ch.swisstms.domain.price.Quantity.ZERO,
              order.price(),
              order.orderQty(),
              RegulatoryClock.nowBiz()));
    }
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public Flow.Publisher<ch.swisstms.domain.execution.ExecutionReport> executions() {
    return executionsPublisher;
  }

  @Override
  public Flow.Publisher<MarketDataTick> marketData(SubscriptionRequest req) {
    return marketDataPublisher;
  }

  @Override
  public HealthSnapshot health() {
    return new HealthSnapshot(
        VENUE_MIC,
        HealthSnapshot.Status.CONNECTED,
        RegulatoryClock.nowBiz(),
        execSeq.get(),
        execSeq.get(),
        "in-process mock");
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
