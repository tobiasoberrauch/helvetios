package ch.swisstms.pretraderisk;

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
import ch.swisstms.domain.ports.PretradeRiskPort;
import ch.swisstms.domain.price.Price;
import ch.swisstms.domain.price.Quantity;
import ch.swisstms.pretraderisk.cache.RiskProfileCache;
import ch.swisstms.pretraderisk.cache.RiskProfileCache.Profile;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.Test;

/**
 * T238 — Risk evaluator property test.
 *
 * <p>Invariant 1 — no order whose quantity exceeds the fat-finger ceiling is ever approved.
 * Invariant 2 — every order from a client with a tripped kill-switch is rejected.
 */
class RiskEvaluatorPropertyTest {

  private final RiskProfileCache cache = new RiskProfileCache();
  private final RiskEvaluator evaluator = new RiskEvaluator(cache);
  private final ClientId clientId = new ClientId(UUID.randomUUID());
  private final InstrumentId instrument = new InstrumentId("CH0038863350", "XSWX");

  @Test
  void unknownClientIsRejected() {
    Order order = makeOrder(new BigDecimal("100"), new BigDecimal("100.50"));
    PretradeRiskPort.RiskDecision decision =
        evaluator.evaluate(order, new ClientId(UUID.randomUUID()));
    assertThat(decision).isInstanceOf(PretradeRiskPort.RiskDecision.Rejected.class);
  }

  @Property(tries = 200)
  boolean orderOverFatFingerQuantityIsNeverApproved(@ForAll("oversizedQty") BigDecimal qty) {
    cache.upsert(
        clientId, new Profile(new BigDecimal("10000000"), new BigDecimal("1000"), null, false, 1));
    Order order = makeOrder(qty, new BigDecimal("100.50"));
    return evaluator.evaluate(order, clientId) instanceof PretradeRiskPort.RiskDecision.Rejected;
  }

  @Property(tries = 100)
  boolean killSwitchClientIsAlwaysRejected(@ForAll("anyQty") BigDecimal qty) {
    cache.upsert(clientId, new Profile(null, null, null, true, 1));
    Order order = makeOrder(qty, new BigDecimal("100.50"));
    return evaluator.evaluate(order, clientId) instanceof PretradeRiskPort.RiskDecision.Rejected;
  }

  @Provide
  Arbitrary<BigDecimal> oversizedQty() {
    // ceiling 1000 → must always be > 1000. Round to 8 dp so Quantity.of doesn't reject.
    return Arbitraries.bigDecimals()
        .between(new BigDecimal("1000.01"), BigDecimal.valueOf(50_000))
        .map(b -> b.setScale(8, java.math.RoundingMode.HALF_EVEN));
  }

  @Provide
  Arbitrary<BigDecimal> anyQty() {
    return Arbitraries.bigDecimals()
        .between(BigDecimal.ONE, BigDecimal.valueOf(10_000))
        .map(b -> b.setScale(8, java.math.RoundingMode.HALF_EVEN));
  }

  private Order makeOrder(BigDecimal qty, BigDecimal px) {
    return new Order(
        new OrderId(UUID.randomUUID()),
        new ClOrdId("CL-" + UUID.randomUUID().toString().substring(0, 8)),
        clientId,
        Region.ZH,
        instrument,
        Side.BUY,
        OrdType.LIMIT,
        TimeInForce.DAY,
        RoutingMode.DMA,
        Quantity.of(qty),
        Price.of(px),
        Instant.now());
  }
}
