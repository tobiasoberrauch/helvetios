package ch.swisstms.venue.six.oti;

import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * T295 — SIX OUCH over SoupBinTCP.
 *
 * <p>OUCH is SIX's binary order-entry protocol for the OTI (Order Trading Interface); SoupBinTCP is
 * the framing layer that handles session-level reliability (sequence numbers, gap-fill,
 * heartbeats). The wire encoding is fixed-width binary; we model the dispatch surface here and
 * defer the actual byte layout to Phase 16 generation.
 *
 * <p>Sequence numbers persist in Redis so a session restart can request a gap-fill from the last
 * acknowledged sequence rather than starting from zero (which OUCH treats as a fatal error).
 */
@Component
public class OuchSoupBinTcpClient {

  private static final Logger log = LoggerFactory.getLogger(OuchSoupBinTcpClient.class);
  private final AtomicLong sequence = new AtomicLong();

  public long nextSequence() {
    return sequence.incrementAndGet();
  }

  public void primeFromRedis(long lastAcknowledged) {
    sequence.set(lastAcknowledged);
    log.info("OUCH session sequence primed to {} from Redis", lastAcknowledged);
  }

  public record OuchEnterOrder(
      long token,
      char side, // 'B' or 'S'
      long quantity,
      long stockId,
      long price,
      char timeInForce) {}

  public byte[] encodeEnterOrder(OuchEnterOrder ord) {
    // Fixed-width 47-byte OUCH "Enter Order" message; production codec generated from the
    // SIX-published schema. Phase 15 returns a length-only stub for unit-testability.
    byte[] frame = new byte[47];
    frame[0] = 'O'; // message type
    return frame;
  }
}
