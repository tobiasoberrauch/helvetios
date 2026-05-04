package ch.swisstms.ems.algo;

import ch.swisstms.domain.order.Order;
import ch.swisstms.domain.price.Quantity;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * TWAP — Time-Weighted Average Price (T251).
 *
 * <p>Slices the parent order into {@code slices} equal-quantity children, evenly distributed across
 * {@code totalDuration}. Unlike VWAP, TWAP does NOT account for the historical volume profile; it
 * treats every minute of the trading window as equally important. Use TWAP when the parent is too
 * small to dent the book or when liquidity is uniform across the day.
 */
public class TwapStrategy implements AlgoStrategy {

  private final int slices;
  private final Duration totalDuration;

  public TwapStrategy(int slices, Duration totalDuration) {
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
    return "TWAP";
  }
}
