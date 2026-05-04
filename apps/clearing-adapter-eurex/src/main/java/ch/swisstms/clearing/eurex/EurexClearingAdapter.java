package ch.swisstms.clearing.eurex;

import ch.swisstms.audit_chain.AuditEvent.ActorType;
import ch.swisstms.audit_chain.HashChainWriter;
import ch.swisstms.domain.health.HealthSnapshot;
import ch.swisstms.domain.ports.ClearingPort;
import ch.swisstms.time_sync.RegulatoryClock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * Eurex Clearing Adapter (US4 / FR-014).
 *
 * <p>Constitution Principle I — externalisiert das Eurex-spezifische AMQP-/FIXML-/FpML-Wissen
 * hinter dem {@link ClearingPort}.
 */
@Component
public class EurexClearingAdapter implements ClearingPort {

  private static final Logger log = LoggerFactory.getLogger(EurexClearingAdapter.class);
  private static final String CCP_ID = "EUREX-C7";

  private final JmsTemplate jms;
  private final FixmlMessageMapper fixml;
  private final CommonReportEngineSftpPuller cre;
  private final HashChainWriter audit;
  private final String tradeCaptureQueue;
  private final SubmissionPublisher<ClearingTradeEvent> events = new SubmissionPublisher<>();
  private final SubmissionPublisher<MarginCall> margins = new SubmissionPublisher<>();
  private final AtomicLong sentCount = new AtomicLong();

  public EurexClearingAdapter(
      JmsTemplate eurexJmsTemplate,
      FixmlMessageMapper fixml,
      CommonReportEngineSftpPuller cre,
      HashChainWriter audit,
      @Value("${swisstms.eurex.queues.trade-capture:eurex.tradecapture}")
          String tradeCaptureQueue) {
    this.jms = eurexJmsTemplate;
    this.fixml = fixml;
    this.cre = cre;
    this.audit = audit;
    this.tradeCaptureQueue = tradeCaptureQueue;
  }

  @Override
  public CompletionStage<ClearingTradeAck> submitForClearing(ClearingFill fill) {
    String tradeReportId = "TR-" + UUID.randomUUID();
    String payload = fixml.toTradeCaptureReportXml(fill, tradeReportId);
    log.info(
        "Eurex submit → {} (rptId={}, exec={})",
        tradeCaptureQueue,
        tradeReportId,
        fill.executionId().value());
    jms.convertAndSend(tradeCaptureQueue, payload);
    sentCount.incrementAndGet();
    audit.append(
        ActorType.SERVICE,
        "clearing-adapter-eurex",
        "clearing.eurex.submitted",
        "ClearingFill",
        tradeReportId,
        ("{\"tradeReportId\":\""
                + tradeReportId
                + "\",\"executionId\":\""
                + fill.executionId().value()
                + "\"}")
            .getBytes(),
        null);
    return CompletableFuture.completedFuture(
        new ClearingTradeAck(
            tradeReportId, ClearingStatus.PENDING_NOVATION, RegulatoryClock.nowBiz(), CCP_ID));
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
    return CompletableFuture.supplyAsync(() -> cre.fetch(date));
  }

  @Override
  public HealthSnapshot health() {
    return new HealthSnapshot(
        CCP_ID,
        HealthSnapshot.Status.CONNECTED,
        RegulatoryClock.nowBiz(),
        sentCount.get(),
        0L,
        "AMQP 1.0 to Qpid broker");
  }

  @Override
  public String ccpId() {
    return CCP_ID;
  }

  /** Test-only: inject inbound clearing events parsed from FIXML. */
  void publishInboundFixml(String xml) {
    events.submit(fixml.fromInboundXml(xml));
  }
}
