package ch.swisstms.venue.bidfx;

import ch.swisstms.domain.health.HealthSnapshot;
import ch.swisstms.time_sync.RegulatoryClock;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * BidFX Pixie firm-tradable-quotes adapter (T196a).
 *
 * <p>Pixie carries firm tradable FX quotes (RFS streaming with execution). Each subscription is a
 * subject built by {@link SubjectBuilder}. Real BidFX SDK wiring lives behind the {@code
 * com.bidfx:bidfx-api:2.x} dependency; Phase 9 uses an in-process surface so the EMS can be wired
 * before vendor onboarding.
 */
@Component
public class PixieAdapter {

  private static final Logger log = LoggerFactory.getLogger(PixieAdapter.class);
  private static final String CHANNEL = "BIDFX-PIXIE";

  private final SubjectBuilder subjects;
  private final AtomicLong executions = new AtomicLong();

  public PixieAdapter(SubjectBuilder subjects) {
    this.subjects = subjects;
  }

  public record TradableQuote(String subject, BigDecimal bid, BigDecimal ask, BigDecimal qty) {}

  public CompletionStage<TradableQuote> requestQuote(SubjectBuilder.Subject s) {
    String subj = subjects.toWireString(s);
    log.debug("Pixie requestQuote {}", subj);
    return CompletableFuture.completedFuture(
        new TradableQuote(subj, new BigDecimal("1.0824"), new BigDecimal("1.0826"), s.quantity()));
  }

  public CompletionStage<String> execute(TradableQuote quote, String side) {
    log.info("Pixie execute side={} subject={}", side, quote.subject());
    executions.incrementAndGet();
    return CompletableFuture.completedFuture("PXE-" + UUID.randomUUID());
  }

  public HealthSnapshot health() {
    return new HealthSnapshot(
        CHANNEL,
        HealthSnapshot.Status.CONNECTED,
        RegulatoryClock.nowBiz(),
        executions.get(),
        0L,
        "BidFX Pixie firm-tradable channel");
  }
}
