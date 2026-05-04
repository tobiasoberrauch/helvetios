package ch.swisstms.venue.bidfx;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SubjectBuilderTest {

  private final SubjectBuilder b = new SubjectBuilder();

  @Test
  void buildsCanonicalEurUsdSpotSubject() {
    var subject =
        b.build()
            .source("BARX")
            .assetClass("FX")
            .symbol("EUR/USD")
            .tenor("SPOT")
            .quantity(new BigDecimal("1000000"))
            .currency("EUR")
            .build();
    String wire = b.toWireString(subject);
    assertThat(wire)
        .contains("LiquidityProvider=BARX")
        .contains("Symbol=EUR/USD")
        .contains("Tenor=SPOT")
        .contains("Quantity=1000000")
        .contains("Currency=EUR");
  }

  @Test
  void preservesInsertionOrder() {
    var subject = b.build().source("UBSW").symbol("USD/JPY").tenor("1M").build();
    assertThat(b.toWireString(subject))
        .startsWith("LiquidityProvider=UBSW,Symbol=USD/JPY,Tenor=1M");
  }
}
