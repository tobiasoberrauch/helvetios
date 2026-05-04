package ch.swisstms.reporting.traxapa;

import static org.assertj.core.api.Assertions.assertThat;

import ch.swisstms.reporting.traxapa.TraxApaTcrBuilder.TradeReport;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.Test;

/** T130 — Trax APA TradeCaptureReport(35=AE) FIX builder property test. */
class TraxApaTcrBuilderPropertyTest {

  private static final char SOH = '\u0001';
  private final TraxApaTcrBuilder builder = new TraxApaTcrBuilder();

  @Test
  void wellKnownReportContainsAllRequiredFixTags() {
    TradeReport r =
        new TradeReport(
            "TR-1",
            "DE000BAY0017",
            "MAXX",
            TradeReport.Side.BUY,
            new BigDecimal("250"),
            new BigDecimal("48.20"),
            "EUR",
            Instant.parse("2026-05-04T13:14:15Z"),
            "5493001KJTIIGC8Y1R12");
    String fix = builder.buildFixMessage(r);
    assertThat(fix).contains("35=AE" + SOH);
    assertThat(fix).contains("571=TR-1" + SOH);
    assertThat(fix).contains("48=DE000BAY0017" + SOH);
    assertThat(fix).contains("207=MAXX" + SOH);
    assertThat(fix).contains("54=1" + SOH);
    assertThat(fix).contains("32=250" + SOH);
    assertThat(fix).contains("31=48.20" + SOH);
    assertThat(fix).contains("448=5493001KJTIIGC8Y1R12" + SOH);
  }

  @Property(tries = 100)
  boolean fixMessageContainsAllProvidedFields(@ForAll("reports") TradeReport r) {
    String fix = builder.buildFixMessage(r);
    return fix.contains("35=AE" + SOH)
        && fix.contains("571=" + r.tradeReportId() + SOH)
        && fix.contains("48=" + r.instrumentIsin() + SOH)
        && fix.contains("32=" + r.lastQty().toPlainString() + SOH);
  }

  @Property(tries = 50)
  boolean csvBatchHasOneLinePerReport(@ForAll("reportLists") List<TradeReport> reports) {
    String csv = builder.buildCsvBatch(reports);
    long lineCount = csv.lines().count();
    return lineCount == reports.size() + 1L; // header + rows
  }

  @Provide
  Arbitrary<TradeReport> reports() {
    return Arbitraries.strings()
        .alpha()
        .ofLength(8)
        .map(
            id ->
                new TradeReport(
                    "TR-" + id,
                    "DE000BAY0017",
                    "MAXX",
                    TradeReport.Side.BUY,
                    new BigDecimal("250"),
                    new BigDecimal("48.20"),
                    "EUR",
                    Instant.parse("2026-05-04T13:14:15Z"),
                    "5493001KJTIIGC8Y1R12"));
  }

  @Provide
  Arbitrary<List<TradeReport>> reportLists() {
    return reports().list().ofMinSize(0).ofMaxSize(50);
  }
}
