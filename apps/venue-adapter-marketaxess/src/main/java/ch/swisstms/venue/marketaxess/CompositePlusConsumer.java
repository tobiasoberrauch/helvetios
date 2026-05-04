package ch.swisstms.venue.marketaxess;

import ch.swisstms.domain.marketdata.MarketDataTick;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Composite+ MarketDataIncrementalRefresh consumer (T193b).
 *
 * <p>Composite+ is MarketAxess' fixed-income mid-pricing feed; quotes arrive as FIX
 * MarketDataIncrementalRefresh (35=X) messages. We parse the relevant entries and republish them as
 * canonical {@link MarketDataTick} on the embedded publisher so the OMS / EMS can consume without
 * knowing FIX.
 */
@Component
public class CompositePlusConsumer {

  private static final Logger log = LoggerFactory.getLogger(CompositePlusConsumer.class);

  private final SubmissionPublisher<MarketDataTick> publisher = new SubmissionPublisher<>();
  private final AtomicLong delivered = new AtomicLong();

  public Flow.Publisher<MarketDataTick> ticks() {
    return publisher;
  }

  /** Test/integration entrypoint — feed a Composite+ snapshot into the publisher. */
  public void onIncrementalRefresh(
      ch.swisstms.domain.instrument.InstrumentId instrument,
      BigDecimal mid,
      BigDecimal qty,
      Instant venueTs) {
    long seq = delivered.incrementAndGet();
    publisher.submit(
        new MarketDataTick(
            instrument,
            ch.swisstms.domain.price.Price.of(mid),
            ch.swisstms.domain.price.Quantity.of(qty),
            ch.swisstms.domain.price.Price.of(mid),
            ch.swisstms.domain.price.Quantity.of(qty),
            ch.swisstms.domain.price.Price.of(mid),
            ch.swisstms.domain.price.Quantity.of(qty),
            venueTs,
            "MAEU-COMPOSITE",
            seq));
    if (seq % 10_000 == 0) {
      log.info("CompositePlus published {} ticks", seq);
    }
  }
}
