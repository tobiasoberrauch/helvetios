package ch.swisstms.ems.algo;

import ch.swisstms.domain.order.Order;
import ch.swisstms.domain.price.Quantity;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * POV — Percentage of Volume (T251).
 *
 * <p>Targets a fixed share ({@code participationCap}) of the realised market volume. The Phase 13
 * implementation precomputes static slices assuming a known average daily volume; Phase 16 makes
 * the strategy reactive — it consumes the live tick stream and resizes the next child every second.
 */
public class PovStrategy implements AlgoStrategy {

  private final BigDecimal participationCap; // e.g. 0.10 for 10 %
  private final BigDecimal averageDailyVolume; // shares
  private final Duration sliceInterval;
  private final Duration totalDuration;

  public PovStrategy(
      BigDecimal participationCap,
      BigDecimal averageDailyVolume,
      Duration sliceInterval,
      Duration totalDuration) {
    this.participationCap = participationCap;
    this.averageDailyVolume = averageDailyVolume;
    this.sliceInterval = sliceInterval;
    this.totalDuration = totalDuration;
  }

  @Override
  public List<ChildSlice> sliceParent(Order parent) {
    long sliceCount = Math.max(1, totalDuration.dividedBy(sliceInterval));
    BigDecimal volumePerSlice =
        averageDailyVolume.divide(BigDecimal.valueOf(sliceCount), 0, RoundingMode.DOWN);
    BigDecimal targetSliceQty =
        volumePerSlice.multiply(participationCap).setScale(0, RoundingMode.DOWN);

    BigDecimal remaining = parent.orderQty().toBigDecimal();
    List<ChildSlice> out = new ArrayList<>();
    long offsetMs = 0;
    while (remaining.signum() > 0) {
      BigDecimal childQty = remaining.min(targetSliceQty);
      out.add(new ChildSlice(Quantity.of(childQty), parent.price(), Duration.ofMillis(offsetMs)));
      remaining = remaining.subtract(childQty);
      offsetMs += sliceInterval.toMillis();
    }
    return out;
  }

  @Override
  public String name() {
    return "POV";
  }
}
