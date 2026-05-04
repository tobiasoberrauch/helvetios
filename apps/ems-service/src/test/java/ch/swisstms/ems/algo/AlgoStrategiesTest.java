package ch.swisstms.ems.algo;

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
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AlgoStrategiesTest {

  private final Order parent = makeOrder(new BigDecimal("10000"));

  @Test
  void twapProducesEqualSlicesAtEqualIntervals() {
    var twap = new TwapStrategy(10, Duration.ofMinutes(60));
    var slices = twap.sliceParent(parent);
    assertThat(slices).hasSize(10);
    assertThat(slices.get(0).quantity().toBigDecimal()).isEqualByComparingTo("1000");
    assertThat(slices.get(9).quantity().toBigDecimal()).isEqualByComparingTo("1000");
  }

  @Test
  void povRespectsParticipationCap() {
    // 10% of 1M shares ADV → 100k per slice; window 60min × 1min slices → 60 slices possible.
    var pov =
        new PovStrategy(
            new BigDecimal("0.10"),
            new BigDecimal("1000000"),
            Duration.ofMinutes(1),
            Duration.ofMinutes(60));
    var slices = pov.sliceParent(makeOrder(new BigDecimal("250000")));
    BigDecimal sum =
        slices.stream()
            .map(s -> s.quantity().toBigDecimal())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(sum).isEqualByComparingTo("250000");
    // No slice should exceed the 100k cap.
    assertThat(slices)
        .allSatisfy(
            s ->
                assertThat(s.quantity().toBigDecimal())
                    .isLessThanOrEqualTo(new BigDecimal("100000")));
  }

  @Test
  void implementationShortfallFrontLoads() {
    var is = new IsStrategy(5, new BigDecimal("0.30"), Duration.ofMinutes(30));
    var slices = is.sliceParent(parent);
    assertThat(slices).hasSize(5);
    BigDecimal first = slices.get(0).quantity().toBigDecimal();
    BigDecimal last = slices.get(4).quantity().toBigDecimal();
    assertThat(first).isGreaterThan(last);
    BigDecimal sum =
        slices.stream()
            .map(s -> s.quantity().toBigDecimal())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(sum).isEqualByComparingTo("10000");
  }

  private static Order makeOrder(BigDecimal qty) {
    return new Order(
        new OrderId(UUID.randomUUID()),
        new ClOrdId("CL-" + UUID.randomUUID().toString().substring(0, 8)),
        new ClientId(UUID.randomUUID()),
        Region.ZH,
        new InstrumentId("CH0038863350", "XSWX"),
        Side.BUY,
        OrdType.LIMIT,
        TimeInForce.DAY,
        RoutingMode.ALGO_WHEEL,
        Quantity.of(qty),
        Price.of(new BigDecimal("100.50")),
        Instant.now());
  }
}
