package ch.swisstms.domain.order;

/** Per-order handling mode (driven by FIX HandlInst Tag 21 on inbound). */
public enum RoutingMode {
  /** Direct Market Access — pass-through routing. */
  DMA,
  /** Algo wheel — automated venue/strategy selection. */
  ALGO_WHEEL,
  /** Care order — trader-handled. */
  CARE
}
