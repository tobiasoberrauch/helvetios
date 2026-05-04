package ch.swisstms.reconciler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.Test;

/**
 * T091 — Property test für JoinKey roundtrip + Konsistenz unter zufälligen (SenderCompID, ClOrdID,
 * ExecID)-Tripeln.
 *
 * <p>Constitution Principle V — der Join-Key ist die einzige Brücke zwischen OMS- und
 * Drop-Copy-Stream; eine Ambiguität hier bricht die Reconciliation.
 */
class ReconcilerKeyJoinTest {

  @Test
  void roundtripCanonicalString() {
    JoinKey k = new JoinKey("SIX", "ALICE-001", "EXEC-7");
    JoinKey parsed = JoinKey.parse(k.asString());
    assertThat(parsed).isEqualTo(k);
  }

  @Test
  void rejectsMalformed() {
    assertThatThrownBy(() -> JoinKey.parse("only|two"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Property(tries = 200)
  boolean roundtripUnderRandomTripletsIsLossless(@ForAll("triplet") String[] t) {
    JoinKey original = new JoinKey(t[0], t[1], t[2]);
    JoinKey parsed = JoinKey.parse(original.asString());
    return parsed.equals(original);
  }

  @Provide
  Arbitrary<String[]> triplet() {
    return Arbitraries.strings()
        .alpha()
        .numeric()
        .withChars('-')
        .ofMinLength(1)
        .ofMaxLength(20)
        .filter(s -> !s.contains("|"))
        .array(String[].class)
        .ofSize(3);
  }
}
