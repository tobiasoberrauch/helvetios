package ch.swisstms.domain.ports;

import ch.swisstms.domain.client.ClientId;
import ch.swisstms.domain.execution.ExecutionId;
import ch.swisstms.domain.health.HealthSnapshot;
import ch.swisstms.domain.instrument.InstrumentId;
import ch.swisstms.domain.order.Side;
import ch.swisstms.domain.price.Price;
import ch.swisstms.domain.price.Quantity;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

/** Single port for clearing-house adapters (Eurex C7, SIX x-clear / SECOM, OTCC↔SHCH). */
public interface ClearingPort {

  /** Submit a fill for clearing/novation. Completes when the CCP ACKs capture. */
  CompletionStage<ClearingTradeAck> submitForClearing(ClearingFill fill);

  /** Stream of clearing-trade lifecycle events (PENDING_NOVATION → NOVATED, …). */
  Flow.Publisher<ClearingTradeEvent> clearingEvents();

  /** Stream of margin calls from this CCP. */
  Flow.Publisher<MarginCall> marginCalls();

  /** Pull the daily report set (Eurex CRE / equivalent) for a value date. */
  CompletionStage<List<ClearingReport>> pullDailyReports(LocalDate date);

  HealthSnapshot health();

  String ccpId();

  /**
   * A fill ready to be sent to the CCP for clearing. Phase 6 — fields chosen so that the FIXML
   * TradeCaptureReport (35=AE) and the FpML InterestRateSwap document can be built from a single
   * value object.
   */
  record ClearingFill(
      ExecutionId executionId,
      ClientId clientId,
      InstrumentId instrument,
      Side side,
      Quantity quantity,
      Price price,
      Instant tradeTime,
      LocalDate valueDate,
      String venue,
      String clearingAccount,
      ClearingProductType productType)
      implements Serializable {}

  /** ACK from the CCP — TradeReportID + capture timestamp. */
  record ClearingTradeAck(
      String tradeReportId, ClearingStatus status, Instant capturedAt, String ccpId)
      implements Serializable {}

  /** Lifecycle event from the CCP for a previously-submitted trade. */
  record ClearingTradeEvent(
      String tradeReportId, ClearingStatus newStatus, Instant occurredAt, String reason)
      implements Serializable {}

  /** Margin call (variation or initial) from the CCP. */
  record MarginCall(
      String ccpId,
      String account,
      MarginType type,
      BigDecimal amount,
      String currency,
      Instant deadline)
      implements Serializable {}

  /** A daily clearing report file pulled from the CCP (e.g. Eurex CRE). */
  record ClearingReport(
      String reportName, LocalDate valueDate, byte[] payload, String contentType, String checksum)
      implements Serializable {}

  enum ClearingStatus {
    PENDING_NOVATION,
    NOVATED,
    REJECTED,
    GIVE_UP,
    BACKED_OUT
  }

  enum MarginType {
    INITIAL_MARGIN,
    VARIATION_MARGIN,
    DEFAULT_FUND_CONTRIBUTION
  }

  enum ClearingProductType {
    LISTED_DERIVATIVE,
    OTC_INTEREST_RATE_SWAP,
    OTC_CDS,
    REPO,
    EQUITY_CASH,
    BOND_CASH
  }

  /**
   * Marker interface — used only so {@link java.io.Serializable}-style guarantees can be expressed
   * without forcing the records to literally implement {@link java.io.Serializable}.
   */
  interface Serializable {}
}
