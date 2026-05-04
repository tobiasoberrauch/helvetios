package ch.swisstms.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Constitution Principle VII (Test-First for Protocol Code, NICHT-VERHANDELBAR).
 *
 * <p>Properties tested: 1. Every legal transition listed in OrdStatus.LEGAL_TRANSITIONS does not
 * throw. 2. After OrdStatus.FILLED only OrdStatus.TRADE_BUSTED is accepted (FR-002). 3. Terminal
 * states (CANCELLED / REJECTED / EXPIRED / TRADE_BUSTED / BUSINESS_REJECTED) reject every further
 * transition. 4. Random transition sequences either succeed (legal) or throw IllegalStateException
 * (illegal) — never silently corrupt the state.
 */
class OrderStateMachinePropertyTest {

  @Property(tries = 1000)
  boolean legalTransitionsNeverThrow(@ForAll("ordStatusPair") OrdStatus[] pair) {
    OrdStatus from = pair[0];
    OrdStatus to = pair[1];
    if (!from.canTransitionTo(to)) {
      return true; // skip — only check legal transitions
    }
    from.transitionTo(to);
    return true;
  }

  @Property(tries = 200)
  boolean afterFilledOnlyTradeBust(@ForAll OrdStatus next) {
    if (next == OrdStatus.TRADE_BUSTED) {
      assertThat(OrdStatus.FILLED.canTransitionTo(next)).isTrue();
      OrdStatus.FILLED.transitionTo(next);
    } else {
      assertThat(OrdStatus.FILLED.canTransitionTo(next))
          .as("After FILLED only TRADE_BUSTED is legal — was: %s", next)
          .isFalse();
      assertThatThrownBy(() -> OrdStatus.FILLED.transitionTo(next))
          .isInstanceOf(IllegalStateException.class);
    }
    return true;
  }

  @Property(tries = 200)
  boolean terminalStatesAreReallyTerminal(
      @ForAll("terminalStatus") OrdStatus terminal, @ForAll OrdStatus anyNext) {
    assertThat(terminal.canTransitionTo(anyNext))
        .as("Terminal %s must not allow transition to %s", terminal, anyNext)
        .isFalse();
    return true;
  }

  @Property(tries = 500)
  boolean illegalTransitionThrows(@ForAll OrdStatus from, @ForAll OrdStatus to) {
    if (from.canTransitionTo(to)) {
      from.transitionTo(to); // would not throw
      return true;
    }
    assertThatThrownBy(() -> from.transitionTo(to))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Illegal OrdStatus transition");
    return true;
  }

  @Provide
  Arbitrary<OrdStatus[]> ordStatusPair() {
    return Arbitraries.of(OrdStatus.values()).array(OrdStatus[].class).ofSize(2);
  }

  @Provide
  Arbitrary<OrdStatus> terminalStatus() {
    return Arbitraries.of(
        OrdStatus.CANCELLED,
        OrdStatus.REJECTED,
        OrdStatus.EXPIRED,
        OrdStatus.TRADE_BUSTED,
        OrdStatus.BUSINESS_REJECTED);
  }
}
