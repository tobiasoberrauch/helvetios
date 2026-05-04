package ch.swisstms.marketdata.normalisation;

import ch.swisstms.domain.instrument.InstrumentId;
import ch.swisstms.domain.marketdata.MarketDataTick;
import ch.swisstms.domain.price.Price;
import ch.swisstms.domain.price.Quantity;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * Vendor-agnostic L1/L2 tick normaliser (T157 / FR-020).
 *
 * <p>Constitution Principle I — vendor-specific shapes (Refinitiv {@code OmmIterable}, Bloomberg
 * {@code Element}) are converted to the canonical {@link MarketDataTick} record before leaving the
 * adapter. The normaliser MUST be vendor-free; vendor SDK types live only in adapters.
 *
 * <p>Constitution Principle IV — venue timestamps stay in {@code bizTime}; the platform timestamp
 * is appended downstream by the publisher (so this normaliser stays free of clock side-effects and
 * is unit-test deterministic).
 */
@Component
public class Normaliser {

  private final AtomicLong seq = new AtomicLong();

  /** Build a canonical L1 top-of-book tick. */
  public MarketDataTick l1(
      InstrumentId instrument,
      BigDecimal bidPx,
      BigDecimal bidQty,
      BigDecimal askPx,
      BigDecimal askQty,
      java.time.Instant bizTime,
      String venueId) {
    return new MarketDataTick(
        instrument,
        Price.of(bidPx),
        Quantity.of(bidQty),
        Price.of(askPx),
        Quantity.of(askQty),
        Price.MARKET,
        Quantity.of(BigDecimal.ZERO),
        bizTime,
        venueId,
        seq.incrementAndGet());
  }

  /** Build a canonical trade-print tick (last-traded price). */
  public MarketDataTick trade(
      InstrumentId instrument,
      BigDecimal lastPx,
      BigDecimal lastQty,
      java.time.Instant bizTime,
      String venueId) {
    return new MarketDataTick(
        instrument,
        Price.MARKET,
        Quantity.of(BigDecimal.ZERO),
        Price.MARKET,
        Quantity.of(BigDecimal.ZERO),
        Price.of(lastPx),
        Quantity.of(lastQty),
        bizTime,
        venueId,
        seq.incrementAndGet());
  }
}
