package ch.swisstms.venue.tradeweb.aiex;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AiEX (Automated Intelligent Execution) Rule Engine — Tradeweb-spezifisch.
 *
 * <p>Konfigurations-DSL in YAML (`aiex/rules.yaml` resource):
 *
 * <ul>
 *   <li>Mindestanzahl Dealer (z.B. 3)
 *   <li>Preistoleranz vs. Composite+ Mid (in basis points)
 *   <li>Time-in-Comp (Sekunden)
 *   <li>Fallback (manual / decline)
 * </ul>
 *
 * <p>TCA-Hook publiziert nach Kafka {@code tca.event.v1}.
 */
@Component
public class AiexRuleEngine {

  private static final Logger log = LoggerFactory.getLogger(AiexRuleEngine.class);

  private final int minDealers;
  private final BigDecimal priceToleranceBps;
  private final int timeInCompSec;

  public AiexRuleEngine(
      @Value("${swisstms.aiex.min-dealers:3}") int minDealers,
      @Value("${swisstms.aiex.price-tolerance-bps:10}") BigDecimal priceToleranceBps,
      @Value("${swisstms.aiex.time-in-comp-sec:30}") int timeInCompSec) {
    this.minDealers = minDealers;
    this.priceToleranceBps = priceToleranceBps;
    this.timeInCompSec = timeInCompSec;
  }

  public Optional<Quote> selectWinner(List<Quote> quotes, BigDecimal compositeMid) {
    if (quotes.size() < minDealers) {
      log.info("AiEX fallback: only {} quotes received, minimum {}", quotes.size(), minDealers);
      return Optional.empty();
    }
    Quote best = quotes.stream().min((a, b) -> a.price().compareTo(b.price())).orElseThrow();
    BigDecimal toleranceFactor =
        BigDecimal.ONE.add(priceToleranceBps.divide(BigDecimal.valueOf(10000)));
    BigDecimal threshold = compositeMid.multiply(toleranceFactor);
    if (best.price().compareTo(threshold) > 0) {
      log.info("AiEX fallback: best price {} exceeds threshold {}", best.price(), threshold);
      return Optional.empty();
    }
    return Optional.of(best);
  }

  public record Quote(String dealerId, BigDecimal price, BigDecimal qty) {}
}
