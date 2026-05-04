package ch.swisstms.venue.six.sti;

import ch.swisstms.domain.execution.ExecutionReport;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * T097 — Drop-Copy Producer.
 *
 * <p>Subscribiert den Execution-Stream des SIX-STI-Adapters und publiziert jeden Fill auf {@code
 * warm.dropcopy.six.v1}. Auf der echten SIX-Drop- Copy-Session (Phase 14) wäre dies eine separate
 * FIX-Session; im Phase-3-In-process-Mock teilen wir den Stream und markieren die Quelle als
 * "DROP_COPY".
 *
 * <p>Constitution V — der Reconciler verwendet diesen Stream als authoritative Source-of-Truth.
 */
@Component
public class SixStiDropCopyProducer implements Subscriber<ExecutionReport> {

  private static final Logger log = LoggerFactory.getLogger(SixStiDropCopyProducer.class);
  private static final String TOPIC = "warm.dropcopy.six.v1";

  private final KafkaTemplate<String, String> kafka;
  private final SixStiAdapter adapter;
  private Subscription subscription;

  @Autowired
  public SixStiDropCopyProducer(KafkaTemplate<String, String> kafka, SixStiAdapter adapter) {
    this.kafka = kafka;
    this.adapter = adapter;
  }

  @jakarta.annotation.PostConstruct
  public void start() {
    adapter.executions().subscribe(this);
    log.info("Drop-copy producer subscribed to SIX/STI execution stream → {}", TOPIC);
  }

  @Override
  public void onSubscribe(Subscription s) {
    this.subscription = s;
    s.request(Long.MAX_VALUE);
  }

  @Override
  public void onNext(ExecutionReport report) {
    if (report.execType() != ch.swisstms.domain.execution.ExecType.PARTIAL_FILL
        && report.execType() != ch.swisstms.domain.execution.ExecType.FILL) {
      return; // drop-copy only carries fills
    }
    String json =
        String.format(
            "{\"venueExecutionId\":\"%s\",\"venueOrderId\":\"%s\","
                + "\"senderCompId\":\"SIX\",\"clOrdId\":\"%s\","
                + "\"venueId\":\"%s\",\"execType\":\"%s\","
                + "\"quantity\":\"%s\",\"price\":\"%s\","
                + "\"cumQty\":\"%s\",\"leavesQty\":\"%s\","
                + "\"bizTime\":\"%s\"}",
            report.venueExecutionId(),
            report.executionId(),
            report.orderId(),
            report.venueId(),
            report.execType(),
            report.quantity(),
            report.price(),
            report.cumQty(),
            report.leavesQty(),
            report.bizTime());
    kafka.send(TOPIC, report.venueExecutionId(), json);
  }

  @Override
  public void onError(Throwable t) {
    log.error("Drop-copy stream errored", t);
  }

  @Override
  public void onComplete() {
    log.info("Drop-copy stream completed");
  }
}
