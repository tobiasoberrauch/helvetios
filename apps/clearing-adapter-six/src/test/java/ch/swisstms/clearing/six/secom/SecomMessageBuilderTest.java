package ch.swisstms.clearing.six.secom;

import static org.assertj.core.api.Assertions.assertThat;

import ch.swisstms.clearing.six.secom.SecomMessageBuilder.SettlementInstruction;
import ch.swisstms.clearing.six.secom.SecomMessageBuilder.SettlementInstruction.Side;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

class SecomMessageBuilderTest {

  private final SecomMessageBuilder b = new SecomMessageBuilder();

  @Test
  void sese023IsWellFormedAndCarriesAllRequiredFields() throws Exception {
    var si =
        new SettlementInstruction(
            "TX-001",
            "TR-001",
            "CH0038863350",
            new BigDecimal("1000"),
            new BigDecimal("105.42"),
            "CHF",
            LocalDate.parse("2026-05-04"),
            LocalDate.parse("2026-05-06"),
            "5493001KJTIIGC8Y1R12",
            "529900CFNG3UWY76TS37",
            Side.DELIV,
            "ACME-CLEARING-001");
    String xml = b.buildSese023(si);
    assertThat(xml)
        .contains("CH0038863350")
        .contains("ACME-CLEARING-001")
        .contains("105.42")
        .contains("DELIV");

    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    DocumentBuilder db = dbf.newDocumentBuilder();
    var doc = db.parse(new InputSource(new StringReader(xml)));
    assertThat(doc.getDocumentElement().getTagName()).isEqualTo("Document");
  }

  @Test
  void sese025MatchedConfirmationParses() {
    String confirm =
        "<?xml version=\"1.0\"?><Document><SctiesSttlmTxConf>"
            + "<TxId>TX-99</TxId><MtchgSts>MACH</MtchgSts></SctiesSttlmTxConf></Document>";
    var outcome = b.parseSese025(confirm);
    assertThat(outcome.txId()).isEqualTo("TX-99");
    assertThat(outcome.status()).isEqualTo("MATCHED");
  }
}
