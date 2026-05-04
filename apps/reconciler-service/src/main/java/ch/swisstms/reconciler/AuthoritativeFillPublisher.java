package ch.swisstms.reconciler;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Publisher für `cold.exec.fill.v1`. Drop-Copy = Source-of-Truth (Constitution Principle V). */
@Component
public class AuthoritativeFillPublisher {

  private final KafkaTemplate<String, String> kafka;

  public AuthoritativeFillPublisher(KafkaTemplate<String, String> kafka) {
    this.kafka = kafka;
  }

  public void publish(JoinKey key, String dropcopyPayload) {
    kafka.send(ReconcilerTopology.TOPIC_AUTHORITATIVE_FILL, key.asString(), dropcopyPayload);
  }
}
