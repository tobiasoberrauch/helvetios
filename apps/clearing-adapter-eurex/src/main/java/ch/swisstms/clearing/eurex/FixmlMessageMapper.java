package ch.swisstms.clearing.eurex;

import ch.swisstms.domain.ports.ClearingPort.ClearingFill;
import ch.swisstms.domain.ports.ClearingPort.ClearingStatus;
import ch.swisstms.domain.ports.ClearingPort.ClearingTradeEvent;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * FIXML 5.0 SP2 envelope mapper for Eurex C7 (US4 / FR-014).
 *
 * <p>Constitution Principle I — alle FIXML-Tag-Details leben hier. Aufrufender Code (OMS / EMS)
 * sieht nur Domain-Typen.
 *
 * <p>Wir generieren die XML hand-crafted statt JAXB, weil (a) die XSD aus
 * `contracts/fixml/FIXML50SP2.xsd` einen riesigen object graph erzeugt und (b) wir nur einen
 * winzigen Subset (TradeCaptureReport, PositionMaintenanceRequest) tatsächlich brauchen. Property
 * tests gegen ein round-trip-XSD-Validate (Phase 13) decken die Korrektheit ab.
 */
@Component
public class FixmlMessageMapper {

  private static final DateTimeFormatter UTC_TS = DateTimeFormatter.ISO_INSTANT;

  /** Build a FIXML TradeCaptureReport (35=AE) for an Eurex clearing submission. */
  public String toTradeCaptureReportXml(ClearingFill fill, String tradeReportId) {
    StringBuilder sb = new StringBuilder(512);
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        .append("<FIXML xmlns=\"http://www.fixprotocol.org/FIXML-5-0-SP2\" v=\"5.0 SP2\">")
        .append("<TrdCaptRpt")
        .append(" RptID=\"")
        .append(escape(tradeReportId))
        .append("\"")
        .append(" TxnTm=\"")
        .append(UTC_TS.format(fill.tradeTime()))
        .append("\"")
        .append(" LastQty=\"")
        .append(fill.quantity().toBigDecimal().toPlainString())
        .append("\"")
        .append(" LastPx=\"")
        .append(fill.price().toBigDecimal().toPlainString())
        .append("\"")
        .append(" Side=\"")
        .append(toFixSide(fill.side()))
        .append("\"")
        .append(" PreallocAcct=\"")
        .append(escape(fill.clearingAccount()))
        .append("\"")
        .append(" ExecID=\"")
        .append(escape(fill.executionId().value().toString()))
        .append("\">")
        .append("<Instrmt")
        .append(" Sym=\"")
        .append(escape(fill.instrument().mic()))
        .append(":")
        .append(escape(fill.instrument().isin()))
        .append("\"")
        .append(" ID=\"")
        .append(escape(fill.instrument().isin()))
        .append("\"")
        .append(" Src=\"4\"") // 4 = ISIN
        .append("/>")
        .append("<RptSide Side=\"")
        .append(toFixSide(fill.side()))
        .append("\"")
        .append(" Acct=\"")
        .append(escape(fill.clearingAccount()))
        .append("\"/>")
        .append("</TrdCaptRpt>")
        .append("</FIXML>");
    return sb.toString();
  }

  /** Parse an inbound FIXML TradeCaptureReportAck or NovationConfirm into a domain event. */
  public ClearingTradeEvent fromInboundXml(String xml) {
    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      // Constitution: contracts must round-trip — XXE protection on every external XML parse.
      dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      dbf.setExpandEntityReferences(false);
      DocumentBuilder db = dbf.newDocumentBuilder();
      Document doc = db.parse(new InputSource(new java.io.StringReader(xml)));
      Element root = doc.getDocumentElement();
      String tradeReportId =
          root.getElementsByTagName("TrdCaptRpt").getLength() > 0
              ? ((Element) root.getElementsByTagName("TrdCaptRpt").item(0)).getAttribute("RptID")
              : "";
      ClearingStatus status =
          xml.contains("Stat=\"NOVATED\"")
              ? ClearingStatus.NOVATED
              : xml.contains("Stat=\"REJECTED\"")
                  ? ClearingStatus.REJECTED
                  : ClearingStatus.PENDING_NOVATION;
      return new ClearingTradeEvent(tradeReportId, status, Instant.now(), "");
    } catch (ParserConfigurationException | SAXException | java.io.IOException e) {
      throw new IllegalArgumentException("Malformed FIXML payload", e);
    }
  }

  private static char toFixSide(ch.swisstms.domain.order.Side side) {
    return switch (side) {
      case BUY -> '1';
      case SELL -> '2';
      case SELL_SHORT -> '5';
    };
  }

  private static String escape(String s) {
    if (s == null) {
      return "";
    }
    return s.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;");
  }
}
