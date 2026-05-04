package ch.swisstms.venue.six.imi;

import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * T297 — SIX IMI ITCH/MoldUDP64 multicast subscriber + TCP gap-fill.
 *
 * <p>IMI (Instinet/Bear Stearns derived) MultiTrack ITCH delivers public market-data via MoldUDP64
 * multicast — the same framing Nasdaq uses. We subscribe via UDP, track the per-stream sequence
 * number, and request gap-fills over a separate TCP channel when we miss packets.
 *
 * <p>Phase 16 wires the actual UDP/TCP I/O via Aeron Archive's MoldUDP64 support; Phase 15 ships
 * the API + sequence-tracking surface.
 */
@Component
@ConditionalOnProperty(value = "swisstms.six.imi.enabled", havingValue = "true")
public class ItchMoldUdp64Subscriber {

  private static final Logger log = LoggerFactory.getLogger(ItchMoldUdp64Subscriber.class);
  private final AtomicLong receivedSeq = new AtomicLong();
  private final AtomicLong gapsRequested = new AtomicLong();

  public void onMoldUdp64Datagram(long sessionSeq, byte[] payload) {
    long expected = receivedSeq.get() + 1;
    if (sessionSeq == expected) {
      receivedSeq.set(sessionSeq);
    } else if (sessionSeq > expected) {
      requestGapFill(expected, sessionSeq - 1);
      receivedSeq.set(sessionSeq);
    }
    // if sessionSeq < expected → duplicate, drop silently.
  }

  void requestGapFill(long firstMissing, long lastMissing) {
    gapsRequested.incrementAndGet();
    log.warn("ITCH gap-fill requested: {}..{}", firstMissing, lastMissing);
    // Phase 16 — fan-out a TCP rewind request to the Mold gap-fill server.
  }

  public long receivedSeq() {
    return receivedSeq.get();
  }

  public long gapsRequested() {
    return gapsRequested.get();
  }
}
