package ch.swisstms.ems.rfq;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Multi-venue RFQ aggregator (T199 / FR-031).
 *
 * <p>Fans an inbound RFQ out to Tradeweb, MarketAxess and BidFX simultaneously, collects every
 * incoming quote in a per-RFQ bucket, and exposes a {@link CompletionStage} that resolves once
 * either every venue has responded or {@code timeInComp} has elapsed (whichever comes first).
 *
 * <p>The actual venue dispatch is owned by the per-venue adapters; this class is venue-agnostic
 * (Constitution Principle I) and only sees an outbound {@link VenueDispatcher} interface.
 */
@Component
public class MultiVenueRfqAggregator {

  private static final Logger log = LoggerFactory.getLogger(MultiVenueRfqAggregator.class);
  private static final Duration DEFAULT_TIMEOUT = Duration.ofMillis(2_000);

  public record Rfq(
      String rfqId,
      String isin,
      String side,
      BigDecimal qty,
      String currency,
      List<String> venues) {}

  public record AggregatedQuote(
      String rfqId, String venue, String dealerId, BigDecimal price, BigDecimal qty) {}

  public interface VenueDispatcher {
    CompletionStage<List<AggregatedQuote>> dispatch(Rfq rfq, String venueMic);
  }

  private final VenueDispatcher dispatcher;
  private final ConcurrentMap<String, List<AggregatedQuote>> buckets = new ConcurrentHashMap<>();

  public MultiVenueRfqAggregator(VenueDispatcher dispatcher) {
    this.dispatcher = dispatcher;
  }

  /** Submit a fresh RFQ and return aggregated quotes once the time-in-comp expires. */
  public CompletionStage<List<AggregatedQuote>> submit(Rfq rfq, Duration timeInComp) {
    String correlationId =
        rfq.rfqId() == null || rfq.rfqId().isEmpty() ? "RFQ-" + UUID.randomUUID() : rfq.rfqId();
    Instant deadline = Instant.now().plus(timeInComp);
    log.info(
        "RFQ {} fanned to {} venue(s); deadline={}", correlationId, rfq.venues().size(), deadline);
    buckets.put(correlationId, new ArrayList<>());
    List<CompletableFuture<List<AggregatedQuote>>> futures =
        rfq.venues().stream()
            .map(
                v ->
                    dispatcher
                        .dispatch(rfq, v)
                        .toCompletableFuture()
                        .completeOnTimeout(
                            List.of(),
                            timeInComp.toMillis(),
                            java.util.concurrent.TimeUnit.MILLISECONDS))
            .toList();
    return CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]))
        .thenApply(
            v -> {
              List<AggregatedQuote> out = new ArrayList<>();
              for (CompletableFuture<List<AggregatedQuote>> f : futures) {
                out.addAll(f.join());
              }
              buckets.put(correlationId, out);
              log.info("RFQ {} aggregated {} quote(s)", correlationId, out.size());
              return out;
            });
  }

  public CompletionStage<List<AggregatedQuote>> submit(Rfq rfq) {
    return submit(rfq, DEFAULT_TIMEOUT);
  }

  public List<AggregatedQuote> snapshot(String rfqId) {
    return buckets.getOrDefault(rfqId, List.of());
  }
}
