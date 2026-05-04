package ch.swisstms.reporting.traxapa;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Trax APA (Approved Publication Arrangement) TradeCaptureReport (35=AE) builder.
 *
 * <p>Trax accepts FIX 5.0 SP2 for the post-trade publication feed. We emit the SOH-delimited
 * canonical FIX wire format; the {@link TraxApaJob} can stream it over QuickFIX/J or — if the daily
 * file exceeds 3 GB (FR-026) — fall back to CSV-SFTP via the same payload model.
 *
 * <p>FIX Tags used: 35=AE, 571 TradeReportID, 856 TradeReportType, 568 TradeRequestID, 487
 * TradeReportTransType, 828 TrdType, 55 Symbol, 48 SecurityID, 22 SecurityIDSource, 54 Side, 32
 * LastQty, 31 LastPx, 75 TradeDate, 60 TransactTime, 207 SecurityExchange.
 */
@Component
public class TraxApaTcrBuilder {

  private static final char SOH = '\u0001';

  public record TradeReport(
      String tradeReportId,
      String instrumentIsin,
      String venueMic,
      Side side,
      BigDecimal lastQty,
      BigDecimal lastPx,
      String currency,
      Instant tradingDateTime,
      String publishingEntityLei) {

    public enum Side {
      BUY,
      SELL
    }
  }

  /** Build a single FIX 35=AE message (SOH-delimited, no header, no checksum yet). */
  public String buildFixMessage(TradeReport r) {
    StringBuilder body = new StringBuilder();
    appendField(body, 35, "AE"); // MsgType
    appendField(body, 571, r.tradeReportId());
    appendField(body, 856, "0"); // TradeReportType: Submit
    appendField(body, 487, "0"); // TradeReportTransType: New
    appendField(body, 828, "0"); // TrdType: Regular Trade
    appendField(body, 48, r.instrumentIsin());
    appendField(body, 22, "4"); // ISIN
    appendField(body, 207, r.venueMic());
    appendField(body, 54, r.side() == TradeReport.Side.BUY ? "1" : "2");
    appendField(body, 32, r.lastQty().toPlainString());
    appendField(body, 31, r.lastPx().toPlainString());
    appendField(body, 15, r.currency()); // Currency
    appendField(
        body,
        75,
        DateTimeFormatter.ISO_LOCAL_DATE.format(
            r.tradingDateTime().atZone(java.time.ZoneOffset.UTC).toLocalDate()));
    appendField(body, 60, DateTimeFormatter.ISO_INSTANT.format(r.tradingDateTime()));
    appendField(body, 448, r.publishingEntityLei()); // PartyID = LEI
    appendField(body, 447, "N"); // PartyIDSource = LEI
    appendField(body, 452, "1"); // PartyRole = ExecutingFirm
    return body.toString();
  }

  /** CSV-SFTP fallback per FR-026 (when the day's batch exceeds the 3 GB FIX-stream cap). */
  public String buildCsvBatch(List<TradeReport> reports) {
    StringBuilder sb = new StringBuilder(reports.size() * 128);
    sb.append("tradeReportId,isin,mic,side,qty,px,ccy,tradeTime,publishingLei\n");
    for (TradeReport r : reports) {
      sb.append(r.tradeReportId())
          .append(',')
          .append(r.instrumentIsin())
          .append(',')
          .append(r.venueMic())
          .append(',')
          .append(r.side())
          .append(',')
          .append(r.lastQty().toPlainString())
          .append(',')
          .append(r.lastPx().toPlainString())
          .append(',')
          .append(r.currency())
          .append(',')
          .append(DateTimeFormatter.ISO_INSTANT.format(r.tradingDateTime()))
          .append(',')
          .append(r.publishingEntityLei())
          .append('\n');
    }
    return sb.toString();
  }

  private static void appendField(StringBuilder sb, int tag, String value) {
    sb.append(tag).append('=').append(value).append(SOH);
  }
}
