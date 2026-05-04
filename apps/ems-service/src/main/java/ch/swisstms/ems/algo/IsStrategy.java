package ch.swisstms.ems.algo;

import ch.swisstms.domain.order.Order;
import ch.swisstms.domain.price.Quantity;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * IS — Implementation Shortfall (T251).
 *
 * <p>Front-loaded slicing: trade aggressively early to minimise the slippage between the decision
 * price and the realised average. We approximate Almgren-Chriss with a simple exponential decay
 * schedule — slice {@code k} carries weight {@code (1-decay)^k}. The schedule sums to the parent
 * quantity.
 */
public class IsStrategy implements AlgoStrategy {

  private final int slices;
  private final BigDecimal decay; // 0 < decay < 1; 0.3 ⇒ first slice ≈ 30 %
  private final Duration totalDuration;

  public IsStrategy(int slices, BigDecimal decay, Duration totalDuration) {
    this.slices = slices;
    this.decay = decay;
    this.totalDuration = totalDuration;
  }

  @Override
  public List<ChildSlice> sliceParent(Order parent) {
    BigDecimal[] weights = new BigDecimal[slices];
    BigDecimal weightSum = BigDecimal.ZERO;
    BigDecimal w = BigDecimal.ONE;
    BigDecimal one = BigDecimal.ONE;
    BigDecimal keep = one.subtract(decay);
    for (int i = 0; i < slices; i++) {
      weights[i] = w;
      weightSum = weightSum.add(w);
      w = w.multiply(keep);
    }
    Duration interval = totalDuration.dividedBy(slices);
    List<ChildSlice> out = new ArrayList<>(slices);
    BigDecimal totalQty = parent.orderQty().toBigDecimal();
    BigDecimal allocated = BigDecimal.ZERO;
    for (int i = 0; i < slices; i++) {
      BigDecimal childQty;
      if (i == slices - 1) {
        childQty = totalQty.subtract(allocated); // last slice absorbs rounding
      } else {
        childQty = totalQty.multiply(weights[i]).divide(weightSum, 8, RoundingMode.HALF_EVEN);
        allocated = allocated.add(childQty);
      }
      out.add(new ChildSlice(Quantity.of(childQty), parent.price(), interval.multipliedBy(i)));
    }
    return out;
  }

  @Override
  public String name() {
    return "IS";
  }
}
