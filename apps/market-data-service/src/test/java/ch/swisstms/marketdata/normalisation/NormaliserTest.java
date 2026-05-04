package ch.swisstms.marketdata.normalisation;

import static org.assertj.core.api.Assertions.assertThat;

import ch.swisstms.domain.instrument.InstrumentId;
import ch.swisstms.domain.marketdata.MarketDataTick;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class NormaliserTest {

  private final Normaliser n = new Normaliser();
  private final InstrumentId nestle = new InstrumentId("CH0038863350", "XSWX");

  @Test
  void l1TickPreservesVenueTimestamp() {
    Instant venueTs = Instant.parse("2026-05-04T09:30:00.123456Z");
    MarketDataTick tick =
        n.l1(
            nestle,
            new BigDecimal("105.40"),
            new BigDecimal("100"),
            new BigDecimal("105.42"),
            new BigDecimal("100"),
            venueTs,
            "REFINITIV-EMA");
    assertThat(tick.bizTime()).isEqualTo(venueTs);
    assertThat(tick.source()).isEqualTo("REFINITIV-EMA");
    assertThat(tick.bidPrice().toBigDecimal()).isEqualByComparingTo("105.40");
    assertThat(tick.askPrice().toBigDecimal()).isEqualByComparingTo("105.42");
  }

  @Test
  void sequenceNumbersAreMonotonic() {
    Instant ts = Instant.parse("2026-05-04T09:30:00Z");
    MarketDataTick first =
        n.l1(
            nestle,
            new BigDecimal("100"),
            BigDecimal.ONE,
            new BigDecimal("101"),
            BigDecimal.ONE,
            ts,
            "X");
    MarketDataTick second = n.trade(nestle, new BigDecimal("100.5"), BigDecimal.ONE, ts, "X");
    MarketDataTick third =
        n.l1(
            nestle,
            new BigDecimal("100"),
            BigDecimal.ONE,
            new BigDecimal("101"),
            BigDecimal.ONE,
            ts,
            "X");
    assertThat(second.sequenceNumber()).isGreaterThan(first.sequenceNumber());
    assertThat(third.sequenceNumber()).isGreaterThan(second.sequenceNumber());
  }
}
