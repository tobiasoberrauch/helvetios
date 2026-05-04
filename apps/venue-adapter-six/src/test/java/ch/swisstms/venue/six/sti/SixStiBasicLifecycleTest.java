package ch.swisstms.venue.six.sti;

import static org.assertj.core.api.Assertions.assertThat;

import ch.swisstms.domain.client.ClientId;
import ch.swisstms.domain.common.Region;
import ch.swisstms.domain.execution.ExecType;
import ch.swisstms.domain.execution.ExecutionReport;
import ch.swisstms.domain.instrument.InstrumentId;
import ch.swisstms.domain.order.ClOrdId;
import ch.swisstms.domain.order.OrdType;
import ch.swisstms.domain.order.Order;
import ch.swisstms.domain.order.OrderId;
import ch.swisstms.domain.order.RoutingMode;
import ch.swisstms.domain.order.Side;
import ch.swisstms.domain.order.TimeInForce;
import ch.swisstms.domain.price.Price;
import ch.swisstms.domain.price.Quantity;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * T059 — Conformance test: NewOrderSingle → ExecutionReport NEW → ExecutionReport PARTIAL_FILL →
 * ExecutionReport FILLED.
 *
 * <p>Constitution Principle VII (NICHT-VERHANDELBAR) — diese Conformance- Suite läuft täglich in CI
 * gegen den simulierten Mock und (sobald Phase 14 lebt) gegen den realen SIX Test-Endpunkt.
 */
class SixStiBasicLifecycleTest {

  @Test
  void newOrderProducesNewPartialFillFilled() throws Exception {
    SixStiAdapter adapter = new SixStiAdapter(true);

    List<ExecutionReport> received = new ArrayList<>();
    CountDownLatch latch = new CountDownLatch(3);

    adapter
        .executions()
        .subscribe(
            new Subscriber<>() {
              @Override
              public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
              }

              @Override
              public void onNext(ExecutionReport e) {
                received.add(e);
                latch.countDown();
              }

              @Override
              public void onError(Throwable t) {
                latch.countDown();
              }

              @Override
              public void onComplete() {}
            });

    Order order =
        new Order(
            new OrderId(UUID.randomUUID()),
            new ClOrdId("ALICE-DEMO-001"),
            new ClientId(UUID.randomUUID()),
            Region.ZH,
            new InstrumentId("CH0038863350", "XSWX"),
            Side.BUY,
            OrdType.LIMIT,
            TimeInForce.DAY,
            RoutingMode.DMA,
            Quantity.of(100),
            Price.of("99.50"),
            Instant.now());
    adapter.submitOrder(order).toCompletableFuture().get(2, TimeUnit.SECONDS);

    assertThat(latch.await(5, TimeUnit.SECONDS)).as("All 3 ExecutionReports arrived").isTrue();
    assertThat(received).hasSize(3);
    assertThat(received.get(0).execType()).isEqualTo(ExecType.NEW);
    assertThat(received.get(1).execType()).isEqualTo(ExecType.PARTIAL_FILL);
    assertThat(received.get(2).execType()).isEqualTo(ExecType.FILL);

    // Cumulative invariants
    assertThat(received.get(2).cumQty()).isEqualTo(order.orderQty());
    assertThat(received.get(2).leavesQty()).isEqualTo(Quantity.ZERO);
  }
}
