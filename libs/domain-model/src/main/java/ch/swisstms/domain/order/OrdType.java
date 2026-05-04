package ch.swisstms.domain.order;

public enum OrdType {
  MARKET,
  LIMIT,
  STOP,
  STOP_LIMIT,
  /** Fill-and-store-the-rest — SIX-specific. */
  FUNARI,
  /** Market-on-Open. */
  MOO,
  /** Limit-on-Open. */
  LOO
}
