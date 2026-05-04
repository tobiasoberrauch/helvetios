package ch.swisstms.ems.rfq;

import static org.assertj.core.api.Assertions.assertThat;

import ch.swisstms.ems.rfq.MultiVenueRfqAggregator.AggregatedQuote;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class QuoteComparatorTest {

  private final QuoteComparator cmp = new QuoteComparator();

  @Test
  void buyerPicksLowestAsk() {
    var quotes =
        List.of(
            new AggregatedQuote(
                "R", "TWBT", "DLR-A", new BigDecimal("100.05"), new BigDecimal("1000")),
            new AggregatedQuote(
                "R", "MAEU", "DLR-B", new BigDecimal("100.03"), new BigDecimal("1000")),
            new AggregatedQuote(
                "R", "BIDFX", "DLR-C", new BigDecimal("100.10"), new BigDecimal("1000")));
    var winner = cmp.pickWinner(quotes, "BUY", new BigDecimal("1000"));
    assertThat(winner).isPresent();
    assertThat(winner.get().dealerId()).isEqualTo("DLR-B");
  }

  @Test
  void sellerPicksHighestBid() {
    var quotes =
        List.of(
            new AggregatedQuote(
                "R", "TWBT", "DLR-A", new BigDecimal("99.95"), new BigDecimal("1000")),
            new AggregatedQuote(
                "R", "MAEU", "DLR-B", new BigDecimal("99.97"), new BigDecimal("1000")),
            new AggregatedQuote(
                "R", "BIDFX", "DLR-C", new BigDecimal("99.93"), new BigDecimal("1000")));
    var winner = cmp.pickWinner(quotes, "SELL", new BigDecimal("1000"));
    assertThat(winner).isPresent();
    assertThat(winner.get().dealerId()).isEqualTo("DLR-B");
  }

  @Test
  void filtersInsufficientQuantity() {
    var quotes =
        List.of(
            new AggregatedQuote(
                "R", "TWBT", "DLR-A", new BigDecimal("100.00"), new BigDecimal("500")),
            new AggregatedQuote(
                "R", "MAEU", "DLR-B", new BigDecimal("100.05"), new BigDecimal("1000")));
    var winner = cmp.pickWinner(quotes, "BUY", new BigDecimal("1000"));
    assertThat(winner).isPresent();
    assertThat(winner.get().dealerId()).isEqualTo("DLR-B");
  }

  @Test
  void filtersOutliers() {
    var quotes =
        List.of(
            new AggregatedQuote(
                "R", "TWBT", "DLR-A", new BigDecimal("100.00"), new BigDecimal("1000")),
            new AggregatedQuote(
                "R", "MAEU", "DLR-B", new BigDecimal("100.05"), new BigDecimal("1000")),
            new AggregatedQuote(
                "R", "BIDFX", "FAT-FINGER", new BigDecimal("50.00"), new BigDecimal("1000")));
    var winner = cmp.pickWinner(quotes, "BUY", new BigDecimal("1000"));
    assertThat(winner).isPresent();
    assertThat(winner.get().dealerId()).isEqualTo("DLR-A");
  }

  @Test
  void emptyListReturnsEmpty() {
    assertThat(cmp.pickWinner(List.of(), "BUY", BigDecimal.ONE)).isEmpty();
  }
}
