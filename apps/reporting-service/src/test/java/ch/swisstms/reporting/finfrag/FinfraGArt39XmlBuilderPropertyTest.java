package ch.swisstms.reporting.finfrag;

import static org.assertj.core.api.Assertions.assertThat;

import ch.swisstms.reporting.finfrag.FinfraGArt39XmlBuilder.FinfraGTx;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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

/** T129 — FinfraG Art.39 builder property test. */
class FinfraGArt39XmlBuilderPropertyTest {

  private final FinfraGArt39XmlBuilder builder = new FinfraGArt39XmlBuilder();

  @Test
  void emptyDayProducesEnvelopeWithReportingEntity() {
    String xml = builder.build(LocalDate.parse("2026-05-04"), "5493001KJTIIGC8Y1R12", List.of());
    assertThat(xml).contains("FinfraGArt39").contains("5493001KJTIIGC8Y1R12");
    assertThat(parses(xml)).isTrue();
  }

  @Property(tries = 50)
  boolean arbitraryBatchIsWellFormed(@ForAll("txs") List<FinfraGTx> txs) {
    String xml = builder.build(LocalDate.parse("2026-05-04"), "5493001KJTIIGC8Y1R12", txs);
    return parses(xml) && xml.contains("FinfraGArt39");
  }

  @Provide
  Arbitrary<List<FinfraGTx>> txs() {
    Arbitrary<FinfraGTx> single =
        Arbitraries.strings()
            .alpha()
            .ofLength(8)
            .map(
                id ->
                    new FinfraGTx(
                        "TX-" + id,
                        "5493001KJTIIGC8Y1R12",
                        "CPTY-" + id,
                        "CH0038863350",
                        "XSWX",
                        new BigDecimal("100.50"),
                        "CHF",
                        new BigDecimal("1000"),
                        Instant.parse("2026-05-04T10:00:00Z"),
                        "EQUITY"));
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
