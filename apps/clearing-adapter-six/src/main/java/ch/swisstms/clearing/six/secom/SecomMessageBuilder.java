package ch.swisstms.clearing.six.secom;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/**
 * SIX SECOM ISO 20022 message builder (T290).
 *
 * <p>SECOM (Settlement Communication) is the SIX SIS / x-clear settlement-instruction protocol used
 * for Swiss-listed equity / fixed-income / derivatives clearing. We emit two ISO 20022 messages:
 *
 * <ul>
 *   <li>{@code sese.023.001.11} — SecuritiesSettlementTransactionInstruction (the trade-level
 *       instruction on settlement date − 1).
 *   <li>{@code sese.025.001.11} — SecuritiesSettlementTransactionConfirmation (the inbound SIS
 *       confirmation we persist into the audit chain).
 * </ul>
 *
 * <p>Constitution Principle III — schemas pinned in {@code contracts/iso20022/sese-023-v11.xsd} and
 * {@code sese-025-v11.xsd}; any field-set change requires a contract test update.
 */
@Component
public class SecomMessageBuilder {

  public record SettlementInstruction(
      String txId,
      String tradeReportId,
      String isin,
      BigDecimal qty,
      BigDecimal price,
      String currency,
      LocalDate tradeDate,
      LocalDate settlementDate,
      String partyLei,
      String counterpartyLei,
      Side side,
      String safekeepingAccount) {

    public enum Side {
      DELIV,
      RECE
    }
  }

  /** Build sese.023.001.11 — SecuritiesSettlementTransactionInstruction. */
  public String buildSese023(SettlementInstruction si) {
    StringBuilder sb = new StringBuilder(2048);
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        .append("<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:sese.023.001.11\">")
        .append("<SctiesSttlmTxInstr>")
        .append("<TxId>")
        .append(esc(si.txId()))
        .append("</TxId>")
        .append("<SttlmTpAndAddtlParams>")
        .append("<SctiesMvmntTp>")
        .append(si.side())
        .append("</SctiesMvmntTp>")
        .append("<Pmt>FREE</Pmt>")
        .append("</SttlmTpAndAddtlParams>")
        .append("<TradDtls>")
        .append("<TradDt><Dt><Dt>")
        .append(si.tradeDate())
        .append("</Dt></Dt></TradDt>")
        .append("<SttlmDt><Dt><Dt>")
        .append(si.settlementDate())
        .append("</Dt></Dt></SttlmDt>")
        .append("</TradDtls>")
        .append("<FinInstrmId><ISIN>")
        .append(esc(si.isin()))
        .append("</ISIN></FinInstrmId>")
        .append("<QtyAndAcctDtls>")
        .append("<SttlmQty><Qty><FaceAmt>")
        .append(si.qty().toPlainString())
        .append("</FaceAmt></Qty></SttlmQty>")
        .append("<SfkpgAcct><Id>")
        .append(esc(si.safekeepingAccount()))
        .append("</Id></SfkpgAcct>")
        .append("</QtyAndAcctDtls>")
        .append("<SttlmParams>")
        .append("<Pric><Amt Ccy=\"")
        .append(esc(si.currency()))
        .append("\">")
        .append(si.price().toPlainString())
        .append("</Amt></Pric>")
        .append("</SttlmParams>")
        .append("<DlvrgSttlmPties>")
        .append("<Pty1><Id><AnyBIC><AnyBIC>")
        .append(esc(si.partyLei()))
        .append("</AnyBIC></AnyBIC></Id></Pty1>")
        .append("</DlvrgSttlmPties>")
        .append("<RcvgSttlmPties>")
        .append("<Pty1><Id><AnyBIC><AnyBIC>")
        .append(esc(si.counterpartyLei()))
        .append("</AnyBIC></AnyBIC></Id></Pty1>")
        .append("</RcvgSttlmPties>")
        .append("</SctiesSttlmTxInstr>")
        .append("</Document>");
    return sb.toString();
  }

  /** Parse sese.025.001.11 — return TxId + status. */
  public ConfirmationOutcome parseSese025(String xml) {
    String txId = between(xml, "<TxId>", "</TxId>");
    String status =
        xml.contains("<MtchgSts>MACH</MtchgSts>")
            ? "MATCHED"
            : xml.contains("<MtchgSts>NMAT</MtchgSts>") ? "UNMATCHED" : "UNKNOWN";
    return new ConfirmationOutcome(txId, status);
  }

  public record ConfirmationOutcome(String txId, String status) {}

  static final DateTimeFormatter D = DateTimeFormatter.ISO_LOCAL_DATE;

  private static String esc(String s) {
    return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }

  private static String between(String s, String open, String close) {
    int a = s.indexOf(open);
    if (a < 0) {
      return "";
    }
    int b = s.indexOf(close, a + open.length());
    return b < 0 ? "" : s.substring(a + open.length(), b);
  }
}
