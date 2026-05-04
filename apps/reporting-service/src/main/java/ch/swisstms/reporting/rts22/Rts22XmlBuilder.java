package ch.swisstms.reporting.rts22;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * RTS-22 (MiFID II Art. 26) Transaction Report XML builder.
 *
 * <p>Generates the ISO 20022 {@code auth.016.001.02} envelope used by LSEG TRADEcho ARM. The
 * surface here is the minimum-viable subset: TxnDtls + Buyr/Sellr legal-entity blocks. A full
 * RTS-22 has 65+ fields — the missing ones are vendored in {@code
 * contracts/iso20022/rts22-full.xsd} and will be wired in Phase 7B once we have a real
 * reference-data feed for LEIs.
 *
 * <p>Constitution Principle III — schema-as-versioned-contract: the v02 namespace below MUST stay
 * in sync with the XSD pinned in {@code contracts/iso20022/}.
 */
@Component
public class Rts22XmlBuilder {

  public record TransactionReport(
      String transactionRefId,
      String executingEntityLei,
      String submittingEntityLei,
      String buyerLei,
      String sellerLei,
      String instrumentIsin,
      String venueMic,
      Side side,
      BigDecimal price,
      String priceCurrency,
      BigDecimal quantity,
      Instant tradingDateTime) {

    public enum Side {
      BUYI,
      SELL
    }
  }

  /** Build a single auth.016.001.02 envelope holding {@code reports} as TxnDtls children. */
  public String build(List<TransactionReport> reports) {
    StringBuilder sb = new StringBuilder(2048);
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        .append("<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:auth.016.001.02\">")
        .append("<FinInstrmRptgTxRpt>");
    for (TransactionReport r : reports) {
      sb.append("<Tx>")
          .append("<New>")
          .append("<TxId>")
          .append(esc(r.transactionRefId()))
          .append("</TxId>")
          .append("<ExctgPty>")
          .append(esc(r.executingEntityLei()))
          .append("</ExctgPty>")
          .append("<SubmitgPty>")
          .append(esc(r.submittingEntityLei()))
          .append("</SubmitgPty>")
          .append("<Buyr><AcctOwnr><Id><LEI>")
          .append(esc(r.buyerLei()))
          .append("</LEI></Id></AcctOwnr></Buyr>")
          .append("<Sellr><AcctOwnr><Id><LEI>")
          .append(esc(r.sellerLei()))
          .append("</LEI></Id></AcctOwnr></Sellr>")
          .append("<TradDt>")
          .append(DateTimeFormatter.ISO_INSTANT.format(r.tradingDateTime()))
          .append("</TradDt>")
          .append("<TradgVn>")
          .append(esc(r.venueMic()))
          .append("</TradgVn>")
          .append("<FinInstrm><Othr><FinInstrmGnlAttrbts><Id>")
          .append(esc(r.instrumentIsin()))
          .append("</Id></FinInstrmGnlAttrbts></Othr></FinInstrm>")
          .append("<Pric><Pric><MntryVal><Amt Ccy=\"")
          .append(esc(r.priceCurrency()))
          .append("\">")
          .append(r.price().toPlainString())
          .append("</Amt></MntryVal></Pric></Pric>")
          .append("<Qty><Unit>")
          .append(r.quantity().toPlainString())
          .append("</Unit></Qty>")
          .append("<Sd>")
          .append(r.side().name())
          .append("</Sd>")
          .append("</New>")
          .append("</Tx>");
    }
    sb.append("</FinInstrmRptgTxRpt></Document>");
    return sb.toString();
  }

  private static String esc(String s) {
    return s == null
        ? ""
        : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
  }
}
