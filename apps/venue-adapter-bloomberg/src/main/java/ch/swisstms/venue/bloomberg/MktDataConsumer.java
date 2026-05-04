package ch.swisstms.venue.bloomberg;

import ch.swisstms.domain.marketdata.MarketDataTick;
import ch.swisstms.venue.bloomberg.BloombergIdentityCache.CachedIdentity;
import ch.swisstms.venue.bloomberg.BloombergIdentityCache.Identity;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * BLPAPI {@code //blp/mktdata} consumer (T178b).
 *
 * <p>Live market-data subscriptions (BID, ASK, LAST_PRICE, VOLUME). Every subscribe call MUST be
 * accompanied by an Identity that EMRS has approved — see {@link BloombergIdentityCache}. If the
 * identity is missing or expired we refuse the subscription and return an empty publisher so the
 * caller falls into the SubscriptionManager DENIED state.
 */
@Component
public class MktDataConsumer {

  private static final Logger log = LoggerFactory.getLogger(MktDataConsumer.class);

  private final BloombergIdentityCache identityCache;
  private final SubmissionPublisher<MarketDataTick> publisher = new SubmissionPublisher<>();
  private final AtomicLong delivered = new AtomicLong();

  public MktDataConsumer(BloombergIdentityCache identityCache) {
    this.identityCache = identityCache;
  }

  public Flow.Publisher<MarketDataTick> subscribe(Identity identity, String topic) {
    CachedIdentity ci = identityCache.get(identity);
    if (ci == null) {
      log.warn(
          "Bloomberg mktdata subscribe denied — identity uuid={} missing or expired in cache",
          identity.uuid());
      return new SubmissionPublisher<>();
    }
    log.info("Bloomberg mktdata subscribe topic={} for uuid={}", topic, identity.uuid());
    return publisher;
  }

  /** Test/mock hook — feed a tick into the live publisher. */
  public void onTick(MarketDataTick tick) {
    publisher.submit(tick);
    delivered.incrementAndGet();
  }

  public long delivered() {
    return delivered.get();
  }
}
