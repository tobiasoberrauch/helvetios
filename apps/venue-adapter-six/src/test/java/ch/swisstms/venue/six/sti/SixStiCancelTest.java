package ch.swisstms.venue.six.sti;

import static org.assertj.core.api.Assertions.assertThat;

import ch.swisstms.domain.client.ClientId;
import ch.swisstms.domain.common.Region;
import ch.swisstms.domain.execution.ExecType;
import ch.swisstms.domain.execution.ExecutionReport;
import ch.swisstms.domain.instrument.InstrumentId;
import ch.swisstms.domain.order.CancelRequest;
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

/** T060 — NewOrderSingle → OrderCancelRequest → ExecutionReport CANCELED. */
class SixStiCancelTest {

  @Test
  void cancelProducesCanceledExecutionReport() throws Exception {
    SixStiAdapter adapter = new SixStiAdapter(false); // do not auto-fill

    List<ExecutionReport> received = new ArrayList<>();
    CountDownLatch latch = new CountDownLatch(2);
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
              public void onError(Throwable t) {}

              @Override
              public void onComplete() {}
            });

    OrderId orderId = new OrderId(UUID.randomUUID());
    ClOrdId clOrdId = new ClOrdId("ALICE-CANCEL-001");
    Order order =
        new Order(
            orderId,
            clOrdId,
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
    adapter.submitOrder(order).toCompletableFuture().get();
    adapter
        .cancelOrder(new CancelRequest(orderId, clOrdId, new ClOrdId("CANCEL-1"), Instant.now()))
        .toCompletableFuture()
        .get();

    assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
    assertThat(received).hasSize(2);
    assertThat(received.get(0).execType()).isEqualTo(ExecType.NEW);
    assertThat(received.get(1).execType()).isEqualTo(ExecType.CANCELED);
  }
}
