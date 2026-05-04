package ch.swisstms.marketdata.publisher;

import ch.swisstms.domain.marketdata.MarketDataTick;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Cold-tier Kafka publisher for normalised ticks (T160).
 *
 * <p>Topic {@code cold.marketdata.l1.v1} keyed by {@code mic:isin}. Surveillance, position-keeping
 * and the analytics warehouse all consume from here. Aeron is the hot path; Kafka is the durable
 * cold path — Constitution Principle II (latency-tier discipline).
 */
@Component
public class KafkaColdTickPublisher {

  private static final Logger log = LoggerFactory.getLogger(KafkaColdTickPublisher.class);
  private static final String TOPIC = "cold.marketdata.l1.v1";

  private final KafkaTemplate<String, String> kafka;
  private final AtomicLong sent = new AtomicLong();

  public KafkaColdTickPublisher(KafkaTemplate<String, String> kafka) {
    this.kafka = kafka;
  }

  public void publish(MarketDataTick tick) {
    String key = tick.instrument().mic() + ":" + tick.instrument().isin();
    String value =
        "{\"seq\":"
            + tick.sequenceNumber()
            + ",\"isin\":\""
            + tick.instrument().isin()
            + "\",\"mic\":\""
            + tick.instrument().mic()
            + "\",\"bid\":"
            + tick.bidPrice().toBigDecimal().toPlainString()
            + ",\"ask\":"
            + tick.askPrice().toBigDecimal().toPlainString()
            + ",\"bizTime\":\""
            + tick.bizTime()
            + "\",\"src\":\""
            + tick.source()
            + "\"}";
    kafka.send(TOPIC, key, value);
    long n = sent.incrementAndGet();
    if (n % 10_000 == 0) {
      log.info("KafkaColdTickPublisher: {} ticks published to {}", n, TOPIC);
    }
  }

  public long sentCount() {
    return sent.get();
  }
}
