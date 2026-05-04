package ch.swisstms.venue.six.qti;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * T296 — SIX QTI Market-Maker quote session.
 *
 * <p>QTI (Quote Trading Interface) lets registered Market Makers stream two-sided quotes for
 * SIX-listed instruments. Each MassQuote message carries up to 1000 quote-sets; the throughput
 * driver is keeping the round-trip below 500 µs so quotes don't go stale.
 */
@Component
public class QtiQuoteSession {

  private static final Logger log = LoggerFactory.getLogger(QtiQuoteSession.class);
  private final AtomicLong quoteIdSeq = new AtomicLong();

  public record QuoteSet(
      String instrumentIsin,
      BigDecimal bidPx,
      BigDecimal bidSize,
      BigDecimal askPx,
      BigDecimal askSize) {}

  /** Build a MassQuote (35=i) with N quote-sets. Returns the assigned MM quote-id. */
  public String submitMassQuote(java.util.List<QuoteSet> quoteSets) {
    String quoteId = "Q-" + quoteIdSeq.incrementAndGet();
    log.info("QTI MassQuote {} ({} sets)", quoteId, quoteSets.size());
    return quoteId;
  }

  /** Cancel all open MM quotes for the session — typical end-of-window action. */
  public void cancelAll() {
    log.info("QTI MassQuoteCancel all");
  }
}
