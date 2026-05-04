package ch.swisstms.venue.bidfx;

import ch.swisstms.domain.health.HealthSnapshot;
import ch.swisstms.time_sync.RegulatoryClock;
import java.math.BigDecimal;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * BidFX Puffin shared-streaming adapter (T196b).
 *
 * <p>Puffin streams indicative (non-firm) quotes to many consumers — used by the OMS for view-only
 * pricing on monitor screens. Subscribe with a wildcard subject and consume from the embedded
 * publisher. Constitution Principle II — Puffin is warm-tier (≤ 5 ms), Pixie is hot-tier.
 */
@Component
public class PuffinAdapter {

  private static final Logger log = LoggerFactory.getLogger(PuffinAdapter.class);
  private static final String CHANNEL = "BIDFX-PUFFIN";

  private final SubjectBuilder subjects;
  private final SubmissionPublisher<IndicativeQuote> publisher = new SubmissionPublisher<>();
  private final AtomicLong delivered = new AtomicLong();

  public PuffinAdapter(SubjectBuilder subjects) {
    this.subjects = subjects;
  }

  public record IndicativeQuote(String subject, BigDecimal bid, BigDecimal ask) {}

  public Flow.Publisher<IndicativeQuote> subscribe(SubjectBuilder.Subject s) {
    String subj = subjects.toWireString(s);
    log.debug("Puffin subscribe {}", subj);
    return publisher;
  }

  /** Test/integration hook to push a tick downstream. */
  public void onTick(IndicativeQuote q) {
    publisher.submit(q);
    delivered.incrementAndGet();
  }

  public HealthSnapshot health() {
    return new HealthSnapshot(
        CHANNEL,
        HealthSnapshot.Status.CONNECTED,
        RegulatoryClock.nowBiz(),
        delivered.get(),
        0L,
        "BidFX Puffin shared-streaming channel");
  }
}
