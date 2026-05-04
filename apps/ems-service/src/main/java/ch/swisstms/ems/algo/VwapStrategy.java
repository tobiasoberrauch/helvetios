package ch.swisstms.ems.algo;

import ch.swisstms.domain.order.Order;
import ch.swisstms.domain.price.Quantity;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * VWAP — Volume-Weighted Average Price algo.
 *
 * <p>Phase 13 — Skeleton mit der einfachsten Slice-Strategie: parent-order über N gleichgewichtete
 * Zeit-Slices, jede mit gleicher Quantity. Phase 16 hardening: historisches Volume-Profil pro
 * Instrument (per QuestDB-Tick-Hot-Tier-Lookup).
 */
public class VwapStrategy implements AlgoStrategy {

  private final int slices;
  private final Duration totalDuration;

  public VwapStrategy(int slices, Duration totalDuration) {
    this.slices = slices;
    this.totalDuration = totalDuration;
  }

  @Override
  public List<ChildSlice> sliceParent(Order parent) {
    BigDecimal sliceQty =
        parent
            .orderQty()
            .toBigDecimal()
            .divide(BigDecimal.valueOf(slices), 8, RoundingMode.HALF_EVEN);
    Duration interval = totalDuration.dividedBy(slices);
    List<ChildSlice> out = new ArrayList<>(slices);
    for (int i = 0; i < slices; i++) {
      out.add(new ChildSlice(Quantity.of(sliceQty), parent.price(), interval.multipliedBy(i)));
    }
    return out;
  }

  @Override
  public String name() {
    return "VWAP";
  }
}
