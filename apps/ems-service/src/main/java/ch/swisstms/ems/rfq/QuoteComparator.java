package ch.swisstms.ems.rfq;

import ch.swisstms.ems.rfq.MultiVenueRfqAggregator.AggregatedQuote;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Quote comparator + winner-picker (T200).
 *
 * <p>Ranking algorithm:
 *
 * <ol>
 *   <li>filter by quoted-quantity ≥ requested qty (no partial fills);
 *   <li>filter outliers ≥ 200 bps from the median (likely fat-finger or stale);
 *   <li>pick the best price for the side ({@code BUY} → lowest ask, {@code SELL} → highest bid).
 * </ol>
 *
 * <p>If no quote survives the filters we return empty; the caller's AiEX rule decides whether to
 * retry, escalate to a sales trader, or aggress on a fallback liquidity pool.
 */
@Component
public class QuoteComparator {

  private static final Logger log = LoggerFactory.getLogger(QuoteComparator.class);
  private static final BigDecimal OUTLIER_TOLERANCE_BPS = new BigDecimal("200");

  public Optional<AggregatedQuote> pickWinner(
      List<AggregatedQuote> quotes, String side, BigDecimal requestedQty) {
    if (quotes.isEmpty()) {
      return Optional.empty();
    }
    var feasible = quotes.stream().filter(q -> q.qty().compareTo(requestedQty) >= 0).toList();
    if (feasible.isEmpty()) {
      log.debug("No quote met requested qty {}", requestedQty);
      return Optional.empty();
    }
    BigDecimal median = median(feasible.stream().map(AggregatedQuote::price).sorted().toList());
    var clean =
        feasible.stream().filter(q -> withinBps(q.price(), median, OUTLIER_TOLERANCE_BPS)).toList();
    if (clean.isEmpty()) {
      return Optional.empty();
    }
    Comparator<AggregatedQuote> cmp =
        "BUY".equalsIgnoreCase(side)
            ? Comparator.comparing(AggregatedQuote::price)
            : Comparator.comparing(AggregatedQuote::price).reversed();
    return clean.stream().min(cmp);
  }

  static BigDecimal median(List<BigDecimal> sortedPrices) {
    int n = sortedPrices.size();
    if (n == 0) {
      return BigDecimal.ZERO;
    }
    if (n % 2 == 1) {
      return sortedPrices.get(n / 2);
    }
    return sortedPrices
        .get(n / 2 - 1)
        .add(sortedPrices.get(n / 2))
        .divide(BigDecimal.valueOf(2), 6, java.math.RoundingMode.HALF_UP);
  }

  static boolean withinBps(BigDecimal value, BigDecimal benchmark, BigDecimal toleranceBps) {
    if (benchmark.signum() == 0) {
      return false;
    }
    BigDecimal diffBps =
        value
            .subtract(benchmark)
            .abs()
            .multiply(BigDecimal.valueOf(10_000))
            .divide(benchmark, 4, java.math.RoundingMode.HALF_UP);
    return diffBps.compareTo(toleranceBps) <= 0;
  }
}
