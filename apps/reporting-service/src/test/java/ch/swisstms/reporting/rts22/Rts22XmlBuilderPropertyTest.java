package ch.swisstms.reporting.rts22;

import static org.assertj.core.api.Assertions.assertThat;

import ch.swisstms.reporting.rts22.Rts22XmlBuilder.TransactionReport;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

/** T128 — RTS-22 builder property test. Generated XML must always be well-formed. */
class Rts22XmlBuilderPropertyTest {

  private final Rts22XmlBuilder builder = new Rts22XmlBuilder();

  @Test
  void emptyBatchProducesValidEnvelope() {
    String xml = builder.build(List.of());
    assertThat(xml).contains("<FinInstrmRptgTxRpt").contains("</Document>");
    assertThat(parses(xml)).isTrue();
  }

  @Property(tries = 50)
  boolean arbitraryBatchIsWellFormed(@ForAll("reports") List<TransactionReport> reports) {
    String xml = builder.build(reports);
    return parses(xml) && xml.contains("auth.016.001.02");
  }

  @Provide
  Arbitrary<List<TransactionReport>> reports() {
    Arbitrary<TransactionReport> single =
        Arbitraries.strings()
            .alpha()
            .ofLength(20)
            .map(
                lei ->
                    new TransactionReport(
                        "TX-" + lei,
                        "EXCT-" + lei,
                        "SUBM-" + lei,
                        "BUYR-" + lei,
                        "SELL-" + lei,
                        "CH0038863350",
                        "XSWX",
                        TransactionReport.Side.BUYI,
                        new BigDecimal("100.50"),
                        "CHF",
                        new BigDecimal("1000"),
                        Instant.parse("2026-05-04T10:00:00Z")));
    return single.list().ofMinSize(0).ofMaxSize(20);
  }

  private static boolean parses(String xml) {
    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      DocumentBuilder db = dbf.newDocumentBuilder();
      return db.parse(new InputSource(new StringReader(xml))).getDocumentElement() != null;
    } catch (Exception e) {
      return false;
    }
  }
}
