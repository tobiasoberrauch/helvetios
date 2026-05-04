package ch.swisstms.venue.tradeweb.tca;

import ch.swisstms.audit_chain.AuditEvent.ActorType;
import ch.swisstms.audit_chain.HashChainWriter;
import ch.swisstms.venue.tradeweb.aiex.AiexRuleEngine.Quote;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Transaction Cost Analysis emitter (T191).
 *
 * <p>Every winning quote produces one {@code tca.event.v1} record with:
 *
 * <ul>
 *   <li>{@code rfqId} / {@code orderId} — links back to the OMS;
 *   <li>{@code winningPrice}, {@code midOfBest3}, {@code slippageBps};
 *   <li>{@code dealerCount}, {@code timeInCompMs}, {@code aiExFiredRule};
 *   <li>{@code micAtBest} / {@code venueChosen} for routing-quality dashboards.
 * </ul>
 *
 * <p>Constitution Principle VI — every TCA record also generates an audit-chain entry tagged {@code
 * tca.tradeweb.published} so analysts can reconcile TCA against the underlying execution.
 */
@Component
public class TcaEmitter {

  private static final Logger log = LoggerFactory.getLogger(TcaEmitter.class);
  private static final String TOPIC = "tca.event.v1";

  private final KafkaTemplate<String, String> kafka;
  private final HashChainWriter audit;
  private final AtomicLong emitted = new AtomicLong();

  public TcaEmitter(KafkaTemplate<String, String> kafka, HashChainWriter audit) {
    this.kafka = kafka;
    this.audit = audit;
  }

  public TcaRecord emit(
      String rfqId,
      String orderId,
      List<Quote> allQuotes,
      Quote winning,
      String side,
      String firedRuleName,
      long timeInCompMs) {
    BigDecimal midOfBest3 = midOfBest3(allQuotes, side);
    BigDecimal slippageBps = slippageBps(winning.price(), midOfBest3);
    TcaRecord record =
        new TcaRecord(
            rfqId,
            orderId,
            winning.dealerId(),
            winning.price(),
            midOfBest3,
            slippageBps,
            allQuotes.size(),
            timeInCompMs,
            firedRuleName,
            "TWBT",
            Instant.now());
    String json =
        "{\"rfqId\":\""
            + rfqId
            + "\",\"orderId\":\""
            + orderId
            + "\",\"winningDealer\":\""
            + winning.dealerId()
            + "\",\"winningPrice\":"
            + winning.price().toPlainString()
            + ",\"midOfBest3\":"
            + midOfBest3.toPlainString()
            + ",\"slippageBps\":"
            + slippageBps.toPlainString()
            + ",\"dealerCount\":"
            + allQuotes.size()
            + ",\"timeInCompMs\":"
            + timeInCompMs
            + ",\"firedRule\":\""
            + firedRuleName
            + "\",\"venue\":\"TWBT\"}";
    kafka.send(TOPIC, rfqId, json);
    audit.append(
        ActorType.SERVICE,
        "venue-adapter-tradeweb",
        "tca.tradeweb.published",
        "TcaRecord",
        rfqId,
        json.getBytes(),
        null);
    long n = emitted.incrementAndGet();
    if (n % 1_000 == 0) {
      log.info("TCA emitter: {} records published to {}", n, TOPIC);
    }
    return record;
  }

  public long emittedCount() {
    return emitted.get();
  }

  public record TcaRecord(
      String rfqId,
      String orderId,
      String winningDealerId,
      BigDecimal winningPrice,
      BigDecimal midOfBest3,
      BigDecimal slippageBps,
      int dealerCount,
      long timeInCompMs,
      String firedRule,
      String venue,
      Instant publishedAt) {}

  private static BigDecimal midOfBest3(List<Quote> quotes, String side) {
    boolean buy = "BUY".equalsIgnoreCase(side);
    var sorted = quotes.stream().map(Quote::price).sorted().toList();
    int take = Math.min(3, sorted.size());
    BigDecimal sum = BigDecimal.ZERO;
    for (int i = 0; i < take; i++) {
      // For BUY: best (lowest) prices; for SELL: best (highest) prices.
      sum = sum.add(buy ? sorted.get(i) : sorted.get(sorted.size() - 1 - i));
    }
    return take == 0
        ? BigDecimal.ZERO
        : sum.divide(BigDecimal.valueOf(take), 6, java.math.RoundingMode.HALF_UP);
  }

  private static BigDecimal slippageBps(BigDecimal price, BigDecimal benchmark) {
    if (benchmark.signum() == 0) {
      return BigDecimal.ZERO;
    }
    return price
        .subtract(benchmark)
        .multiply(BigDecimal.valueOf(10_000))
        .divide(benchmark, 4, java.math.RoundingMode.HALF_UP);
  }
}
