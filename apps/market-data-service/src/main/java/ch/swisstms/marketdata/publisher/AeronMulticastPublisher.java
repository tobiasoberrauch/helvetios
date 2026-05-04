package ch.swisstms.marketdata.publisher;

import ch.swisstms.domain.marketdata.MarketDataTick;
import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.driver.MediaDriver;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Aeron multicast publisher (T159 / FR-022).
 *
 * <p>Publishes normalised ticks on a multicast channel; all in-process consumers (EMS,
 * surveillance) subscribe to the same stream so the platform fans-out without re-normalising. Wire
 * format is the SBE-encoded {@code marketDataTickV1} schema (Phase 2 codegen) — Phase 8 ships a
 * JSON-shaped frame so unit tests can read it back; Phase 15 swaps in the SBE encoder.
 *
 * <p>Disabled by default in tests via {@code swisstms.marketdata.aeron.enabled=false}; the active
 * profile {@code prod-shadow} flips it on.
 */
@Component
@ConditionalOnProperty(value = "swisstms.marketdata.aeron.enabled", havingValue = "true")
public class AeronMulticastPublisher {

  private static final Logger log = LoggerFactory.getLogger(AeronMulticastPublisher.class);
  private static final int BUFFER_BYTES = 4096;

  private final MediaDriver mediaDriver;
  private final Aeron aeron;
  private final Publication publication;
  private final UnsafeBuffer scratch = new UnsafeBuffer(new byte[BUFFER_BYTES]);
  private final AtomicLong sent = new AtomicLong();

  public AeronMulticastPublisher(
      @Value("${swisstms.marketdata.aeron.channel:aeron:udp?endpoint=224.10.0.1:40123}")
          String channel,
      @Value("${swisstms.marketdata.aeron.streamId:42}") int streamId) {
    this.mediaDriver = MediaDriver.launchEmbedded();
    this.aeron =
        Aeron.connect(new Aeron.Context().aeronDirectoryName(mediaDriver.aeronDirectoryName()));
    this.publication = aeron.addPublication(channel, streamId);
    log.info("Aeron market-data publisher live on {} stream={}", channel, streamId);
  }

  public void publish(MarketDataTick tick) {
    String frame =
        "{\"isin\":\""
            + tick.instrument().isin()
            + "\",\"mic\":\""
            + tick.instrument().mic()
            + "\",\"bid\":"
            + tick.bidPrice().toBigDecimal().toPlainString()
            + ",\"ask\":"
            + tick.askPrice().toBigDecimal().toPlainString()
            + ",\"seq\":"
            + tick.sequenceNumber()
            + "}";
    byte[] bytes = frame.getBytes(StandardCharsets.UTF_8);
    scratch.putBytes(0, bytes);
    long result = publication.offer(scratch, 0, bytes.length);
    if (result < 0) {
      log.debug("Aeron offer back-pressured: {}", result);
    } else {
      sent.incrementAndGet();
    }
  }

  public long sentCount() {
    return sent.get();
  }

  @PreDestroy
  public void shutdown() {
    publication.close();
    aeron.close();
    mediaDriver.close();
  }
}
