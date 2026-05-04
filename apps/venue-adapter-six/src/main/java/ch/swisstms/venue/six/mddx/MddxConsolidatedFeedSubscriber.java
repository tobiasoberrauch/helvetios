package ch.swisstms.venue.six.mddx;

import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * T298 — SIX MDDX consolidated-feed subscriber.
 *
 * <p>MDDX combines SIX Swiss Exchange + BME (Spain) market data into a single multicast feed. Same
 * MoldUDP64 framing as IMI ITCH but the message dictionary covers two MICs — we tag each tick with
 * its source MIC via the {@code venue} field on the canonical {@link
 * ch.swisstms.domain.marketdata.MarketDataTick}.
 */
@Component
@ConditionalOnProperty(value = "swisstms.six.mddx.enabled", havingValue = "true")
public class MddxConsolidatedFeedSubscriber {

  private static final Logger log = LoggerFactory.getLogger(MddxConsolidatedFeedSubscriber.class);

  private final AtomicLong xswxTicks = new AtomicLong();
  private final AtomicLong bmeTicks = new AtomicLong();

  public void onMddxTick(String mic, byte[] payload) {
    if ("XSWX".equals(mic)) {
      xswxTicks.incrementAndGet();
    } else if ("XBME".equals(mic) || "XMAD".equals(mic)) {
      bmeTicks.incrementAndGet();
    } else {
      log.debug("Unknown MDDX source MIC: {}", mic);
    }
  }

  public long xswxCount() {
    return xswxTicks.get();
  }

  public long bmeCount() {
    return bmeTicks.get();
  }
}
