package ch.swisstms.domain.execution;

public enum ExecType {
  NEW,
  PARTIAL_FILL,
  FILL,
  CANCELED,
  REPLACED,
  REJECTED,
  TRADE_BUST,
  EXPIRED
}
