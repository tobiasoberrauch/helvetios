package ch.swisstms.venue.marketaxess;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * MarketAxess Open Trading FIX channel (T193a / FR-031).
 *
 * <p>"All-to-All" liquidity pool: any participant can be a price-maker on any RFQ. Real session is
 * FIX 5.0 SP2 over QuickFIX/J on the standard {@code openttrading.marketaxess.com} endpoint. Phase
 * 9 ships the request surface — Phase 14 binds it to a real session.
 */
@Component
public class OpenTradingChannel {

  private static final Logger log = LoggerFactory.getLogger(OpenTradingChannel.class);

  public record OpenTradingRfq(
      String rfqId, String isin, String side, BigDecimal qty, String currency, int dealerCount) {}

  public record OpenTradingQuote(
      String rfqId, String dealerCompId, BigDecimal price, BigDecimal qty, long latencyMicros) {}

  public CompletionStage<String> sendRfq(OpenTradingRfq rfq) {
    String corrId = "OT-" + UUID.randomUUID();
    log.info(
        "MarketAxess Open Trading RFQ {} isin={} side={} qty={} dealers={}",
        corrId,
        rfq.isin(),
        rfq.side(),
        rfq.qty(),
        rfq.dealerCount());
    return CompletableFuture.completedFuture(corrId);
  }
}
