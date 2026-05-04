package ch.swisstms.domain.order;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Order lifecycle state machine.
 *
 * <p>Constitution Principle II — every transition is checked at compile- and run-time; illegal
 * transitions throw {@link IllegalStateException}. The invariant "after FILLED only TRADE_BUSTED is
 * legal" is encoded directly in the {@link #LEGAL_TRANSITIONS} map and is the subject of {@code
 * OrderStateMachinePropertyTest} (Constitution Principle VII).
 */
public enum OrdStatus {
  NEW,
  ACKNOWLEDGED,
  PARTIALLY_FILLED,
  FILLED,
  PENDING_CANCEL,
  CANCELLED,
  PENDING_REPLACE,
  REJECTED,
  EXPIRED,
  TRADE_BUSTED,
  BUSINESS_REJECTED;

  private static final Map<OrdStatus, Set<OrdStatus>> LEGAL_TRANSITIONS;

  static {
    LEGAL_TRANSITIONS = new EnumMap<>(OrdStatus.class);

    LEGAL_TRANSITIONS.put(NEW, EnumSet.of(ACKNOWLEDGED, REJECTED, BUSINESS_REJECTED));
    LEGAL_TRANSITIONS.put(
        ACKNOWLEDGED,
        EnumSet.of(
            PARTIALLY_FILLED,
            FILLED,
            PENDING_CANCEL,
            PENDING_REPLACE,
            REJECTED,
            BUSINESS_REJECTED,
            EXPIRED));
    LEGAL_TRANSITIONS.put(
        PARTIALLY_FILLED,
        EnumSet.of(PARTIALLY_FILLED, FILLED, PENDING_CANCEL, PENDING_REPLACE, EXPIRED));
    // FR-002 — after FILLED only TRADE_BUSTED is legal.
    LEGAL_TRANSITIONS.put(FILLED, EnumSet.of(TRADE_BUSTED));
    LEGAL_TRANSITIONS.put(PENDING_CANCEL, EnumSet.of(CANCELLED, ACKNOWLEDGED, REJECTED));
    LEGAL_TRANSITIONS.put(CANCELLED, EnumSet.noneOf(OrdStatus.class)); // terminal
    LEGAL_TRANSITIONS.put(PENDING_REPLACE, EnumSet.of(ACKNOWLEDGED, REJECTED));
    LEGAL_TRANSITIONS.put(REJECTED, EnumSet.noneOf(OrdStatus.class)); // terminal
    LEGAL_TRANSITIONS.put(EXPIRED, EnumSet.noneOf(OrdStatus.class)); // terminal
    LEGAL_TRANSITIONS.put(TRADE_BUSTED, EnumSet.noneOf(OrdStatus.class)); // terminal
    LEGAL_TRANSITIONS.put(BUSINESS_REJECTED, EnumSet.noneOf(OrdStatus.class)); // terminal
  }

  public boolean canTransitionTo(OrdStatus next) {
    return LEGAL_TRANSITIONS.getOrDefault(this, EnumSet.noneOf(OrdStatus.class)).contains(next);
  }

  public boolean isTerminal() {
    return LEGAL_TRANSITIONS.getOrDefault(this, EnumSet.noneOf(OrdStatus.class)).isEmpty();
  }

  public OrdStatus transitionTo(OrdStatus next) {
    if (!canTransitionTo(next)) {
      throw new IllegalStateException("Illegal OrdStatus transition: " + this + " -> " + next);
    }
    return next;
  }
}
