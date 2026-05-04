package ch.swisstms.domain.order;

public enum TimeInForce {
  DAY,
  /** Immediate or Cancel. */
  IOC,
  /** Fill or Kill. */
  FOK,
  /** Good till Cancel. */
  GTC,
  /** Good till Date. */
  GTD,
  /** At the Opening. */
  OPG
}
