package ch.swisstms.venue.bloomberg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * BLPAPI {@code //blp/refdata} consumer (T178a).
 *
 * <p>Reference-data only — name/ticker/CUSIP/ISIN, ratings, fundamentals, calendars. The interface
 * stays vendor-agnostic so the OMS / reference-data-service can call it without depending on BLPAPI
 * types directly.
 *
 * <p>Phase 14 wires the real {@code Service /blp/refdata} subscription; Phase 8 returns a fixture
 * map so the wider platform can be tested against a Bloomberg-shaped contract.
 */
@Component
public class RefDataConsumer {

  private static final Logger log = LoggerFactory.getLogger(RefDataConsumer.class);

  public CompletionStage<Map<String, Map<String, String>>> request(
      List<String> tickers, List<String> fields) {
    log.info(
        "Bloomberg //blp/refdata request: {} tickers × {} fields", tickers.size(), fields.size());
    Map<String, Map<String, String>> out = new HashMap<>();
    for (String ticker : tickers) {
      Map<String, String> row = new HashMap<>();
      for (String field : fields) {
        row.put(field, "STUB-" + ticker + "-" + field);
      }
      out.put(ticker, row);
    }
    return CompletableFuture.completedFuture(out);
  }
}
