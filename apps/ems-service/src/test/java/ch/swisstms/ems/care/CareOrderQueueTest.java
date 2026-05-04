package ch.swisstms.ems.care;

import static org.assertj.core.api.Assertions.assertThat;

import ch.swisstms.domain.client.ClientId;
import ch.swisstms.domain.common.Region;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CareOrderQueueTest {

  private final CareOrderQueue queue = new CareOrderQueue();

  @Test
  void enqueueClaimAndExecuteFlow() {
    var entry = queue.enqueue(makeOrder(), "trader-alice");
    assertThat(entry.status()).isEqualTo(CareOrderQueue.Status.PENDING);
    var claimed = queue.claim(entry.entryId(), "trader-alice");
    assertThat(claimed.status()).isEqualTo(CareOrderQueue.Status.CLAIMED);
    var executed = queue.mark(entry.entryId(), CareOrderQueue.Status.EXECUTED, "filled @ 105.42");
    assertThat(executed.status()).isEqualTo(CareOrderQueue.Status.EXECUTED);
  }

  @Test
  void queueForReturnsOnlyTradersOwnEntries() {
    queue.enqueue(makeOrder(), "alice");
    queue.enqueue(makeOrder(), "alice");
    queue.enqueue(makeOrder(), "bob");
    assertThat(queue.queueFor("alice")).hasSize(2);
    assertThat(queue.queueFor("bob")).hasSize(1);
  }

  private static Order makeOrder() {
    return new Order(
        new OrderId(UUID.randomUUID()),
        new ClOrdId("CL-" + UUID.randomUUID().toString().substring(0, 8)),
        new ClientId(UUID.randomUUID()),
        Region.ZH,
        new InstrumentId("CH0038863350", "XSWX"),
        Side.BUY,
        OrdType.LIMIT,
        TimeInForce.DAY,
        RoutingMode.CARE,
        Quantity.of(new BigDecimal("250000")),
        Price.of(new BigDecimal("105.40")),
        Instant.now());
  }
}
