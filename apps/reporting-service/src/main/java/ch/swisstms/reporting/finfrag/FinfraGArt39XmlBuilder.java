package ch.swisstms.reporting.finfrag;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * FinfraG Art. 39 (Swiss Financial Market Infrastructure Act) daily transaction report.
 *
 * <p>Targets the SIX Trade Repository (SIX TR) inbound TRI-XML format (per {@code
 * contracts/reporting/finfrag-art39.md}). Each report is a {@code <FinfraGArt39>} root containing
 * one {@code <Tx>} per fill plus a header with the reporting party LEI and value date.
 *
 * <p>Constitution Principle III — XSD pinned at {@code contracts/iso20022/finfrag-art39.xsd}.
 */
@Component
public class FinfraGArt39XmlBuilder {

  public record FinfraGTx(
      String txId,
      String reportingEntityLei,
      String counterpartyLei,
      String instrumentIsin,
      String venueMic,
      BigDecimal price,
      String priceCurrency,
      BigDecimal quantity,
      Instant tradingDateTime,
      String assetClass) {}

  public String build(LocalDate valueDate, String reportingEntityLei, List<FinfraGTx> txs) {
    StringBuilder sb = new StringBuilder(2048);
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        .append("<FinfraGArt39 xmlns=\"https://schema.six-group.com/finfrag/art39/v1\"")
        .append(" valueDate=\"")
        .append(valueDate)
        .append("\"")
        .append(" rptEntity=\"")
        .append(esc(reportingEntityLei))
        .append("\">");
    for (FinfraGTx tx : txs) {
      sb.append("<Tx id=\"")
          .append(esc(tx.txId()))
          .append("\">")
          .append("<Cpty>")
          .append(esc(tx.counterpartyLei()))
          .append("</Cpty>")
          .append("<Instr isin=\"")
          .append(esc(tx.instrumentIsin()))
          .append("\" assetClass=\"")
          .append(esc(tx.assetClass()))
          .append("\"/>")
          .append("<Venue mic=\"")
          .append(esc(tx.venueMic()))
          .append("\"/>")
          .append("<Px ccy=\"")
          .append(esc(tx.priceCurrency()))
          .append("\">")
          .append(tx.price().toPlainString())
          .append("</Px>")
          .append("<Qty>")
          .append(tx.quantity().toPlainString())
          .append("</Qty>")
          .append("<Time>")
          .append(DateTimeFormatter.ISO_INSTANT.format(tx.tradingDateTime()))
          .append("</Time>")
          .append("</Tx>");
    }
    sb.append("</FinfraGArt39>");
    return sb.toString();
  }

  private static String esc(String s) {
    return s == null
        ? ""
        : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
  }
}
