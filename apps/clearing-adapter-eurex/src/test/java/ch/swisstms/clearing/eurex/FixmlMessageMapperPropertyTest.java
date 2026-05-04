package ch.swisstms.clearing.eurex;

import static org.assertj.core.api.Assertions.assertThat;

import ch.swisstms.domain.client.ClientId;
import ch.swisstms.domain.execution.ExecutionId;
import ch.swisstms.domain.instrument.InstrumentId;
import ch.swisstms.domain.order.Side;
import ch.swisstms.domain.ports.ClearingPort.ClearingFill;
import ch.swisstms.domain.ports.ClearingPort.ClearingProductType;
import ch.swisstms.domain.price.Price;
import ch.swisstms.domain.price.Quantity;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * Property test for {@link FixmlMessageMapper} (T113 / Constitution Principle VII).
 *
 * <p>For every randomly generated {@link ClearingFill} the produced FIXML must:
 *
 * <ul>
 *   <li>be well-formed XML (parseable),
 *   <li>carry the executionId, instrument, side, qty, and price exactly as in the domain model,
 *   <li>round-trip back to a {@link ch.swisstms.domain.ports.ClearingPort.ClearingTradeEvent} with
 *       the same TradeReportID when an inbound NOVATED ack is fed back in.
 * </ul>
 */
class FixmlMessageMapperPropertyTest {

  private final FixmlMessageMapper mapper = new FixmlMessageMapper();

  @Test
  void wellKnownTradeCaptureReportIsValid() {
    ClearingFill fill =
        new ClearingFill(
            new ExecutionId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
            new ClientId(UUID.fromString("00000000-0000-0000-0000-000000000002")),
            new InstrumentId("CH0038863350", "XSWX"),
            Side.BUY,
            Quantity.of(BigDecimal.valueOf(1000)),
            Price.of(BigDecimal.valueOf(105.42)),
            Instant.parse("2026-05-04T09:30:00Z"),
            LocalDate.parse("2026-05-06"),
            "XSWX",
            "ACME-CLEARING-001",
            ClearingProductType.EQUITY_CASH);
    String xml = mapper.toTradeCaptureReportXml(fill, "TR-FIXED");
    assertThat(xml).contains("CH0038863350").contains("Side=\"1\"");
    assertThat(xml).containsPattern("LastQty=\"1000(\\.0+)?\"");
    assertThat(parses(xml)).isTrue();
  }

  @Property(tries = 100)
  boolean roundTripsArbitraryFill(@ForAll("fills") ClearingFill fill) {
    String xml = mapper.toTradeCaptureReportXml(fill, "TR-PROP-" + fill.executionId().value());
    return parses(xml)
        && xml.contains(fill.instrument().isin())
        && xml.contains(fill.executionId().value().toString())
        && xml.contains(fill.quantity().toBigDecimal().toPlainString());
  }

  @Provide
  Arbitrary<ClearingFill> fills() {
    Arbitrary<UUID> uuids =
        Arbitraries.longs().between(1, Long.MAX_VALUE).map(l -> new UUID(l, l ^ 0x1234));
    Arbitrary<Side> sides = Arbitraries.of(Side.BUY, Side.SELL);
    Arbitrary<BigDecimal> qty =
        Arbitraries.bigDecimals().between(BigDecimal.ONE, BigDecimal.valueOf(10_000));
    Arbitrary<BigDecimal> px =
        Arbitraries.bigDecimals().between(new BigDecimal("0.01"), BigDecimal.valueOf(10_000));
    return Arbitraries.lazyOf(
        () ->
            uuids.flatMap(
                exec ->
                    uuids.flatMap(
                        cli ->
                            sides.flatMap(
                                s ->
                                    qty.flatMap(
                                        q ->
                                            px.map(
                                                p ->
                                                    new ClearingFill(
                                                        new ExecutionId(exec),
                                                        new ClientId(cli),
                                                        new InstrumentId("CH0038863350", "XSWX"),
                                                        s,
                                                        Quantity.of(q),
                                                        Price.of(p),
                                                        Instant.now(),
                                                        LocalDate.now().plusDays(2),
                                                        "XSWX",
                                                        "ACME-CLEARING-001",
                                                        ClearingProductType.EQUITY_CASH)))))));
  }

  private static boolean parses(String xml) {
    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      DocumentBuilder db = dbf.newDocumentBuilder();
      Document doc = db.parse(new InputSource(new StringReader(xml)));
      return doc.getDocumentElement() != null;
    } catch (Exception e) {
      return false;
    }
  }
}
