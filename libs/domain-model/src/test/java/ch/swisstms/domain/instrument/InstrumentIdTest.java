package ch.swisstms.domain.instrument;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.Test;

class InstrumentIdTest {

  @Test
  void acceptsValidNestleSwiss() {
    // Nestlé S.A. registered share — well-known valid ISIN.
    new InstrumentId("CH0038863350", "XSWX");
  }

  @Test
  void rejectsBadIsinFormat() {
    assertThatThrownBy(() -> new InstrumentId("CH00388633501", "XSWX"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid ISIN format");
  }

  @Test
  void rejectsBadCheckDigit() {
    assertThatThrownBy(() -> new InstrumentId("CH0038863351", "XSWX"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("check digit");
  }

  @Test
  void rejectsBadMic() {
    assertThatThrownBy(() -> new InstrumentId("CH0038863350", "swx"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid MIC format");
  }

  @Property(tries = 50)
  boolean rejectsRandomGarbage(@ForAll("garbage") String junk) {
    try {
      new InstrumentId(junk, "XSWX");
      return false;
    } catch (IllegalArgumentException e) {
      return true;
    }
  }

  @Provide
  Arbitrary<String> garbage() {
    return Arbitraries.strings()
        .alpha()
        .ofMinLength(0)
        .ofMaxLength(15)
        .filter(s -> !s.matches("^[A-Z]{2}[A-Z0-9]{9}[0-9]$"));
  }
}
