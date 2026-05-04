package ch.swisstms.clearing.six;

import ch.swisstms.domain.health.HealthSnapshot;
import ch.swisstms.domain.ports.ClearingPort;
import ch.swisstms.time_sync.RegulatoryClock;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import org.springframework.stereotype.Component;

/** SIX x-clear / SECOM Clearing Adapter — ISO 20022 sese.023, sese.025 Settlement-Instructions. */
@Component
public class SixSecomClearingAdapter implements ClearingPort {
  private static final String CCP_ID = "SIX-X-CLEAR";
  private final SubmissionPublisher<ClearingTradeEvent> events = new SubmissionPublisher<>();
  private final SubmissionPublisher<MarginCall> margins = new SubmissionPublisher<>();

  @Override
  public CompletionStage<ClearingTradeAck> submitForClearing(ClearingFill fill) {
    return CompletableFuture.completedFuture(
        new ClearingTradeAck(
            "SECOM-" + java.util.UUID.randomUUID(),
            ClearingStatus.PENDING_NOVATION,
            RegulatoryClock.nowBiz(),
            CCP_ID));
  }

  @Override
  public Flow.Publisher<ClearingTradeEvent> clearingEvents() {
    return events;
  }

  @Override
  public Flow.Publisher<MarginCall> marginCalls() {
    return margins;
  }

  @Override
  public CompletionStage<List<ClearingReport>> pullDailyReports(LocalDate date) {
    return CompletableFuture.completedFuture(List.of());
  }

  @Override
  public HealthSnapshot health() {
    return new HealthSnapshot(
        CCP_ID,
        HealthSnapshot.Status.DISCONNECTED,
        RegulatoryClock.nowBiz(),
        0L,
        0L,
        "Phase 15 SECOM ISO 20022 — wiring follows");
  }

  @Override
  public String ccpId() {
    return CCP_ID;
  }
}
