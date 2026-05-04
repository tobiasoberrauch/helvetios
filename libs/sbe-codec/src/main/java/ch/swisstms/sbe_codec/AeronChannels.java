package ch.swisstms.sbe_codec;

/**
 * Zentrale Channel-Naming-Convention für Aeron IPC + UDP. Bezug: contracts/sbe/orders.xml.md,
 * executions.xml.md, market-data.xml.md.
 */
public final class AeronChannels {
  private AeronChannels() {}

  // Hot-path inbound flow
  public static final String IPC_ACCEPTOR_TO_RISK = "aeron:ipc?endpoint=acceptor-to-risk";
  public static final int STREAM_ACCEPTOR_TO_RISK = 100;
  public static final String IPC_RISK_TO_EMS = "aeron:ipc?endpoint=risk-to-ems";
  public static final int STREAM_RISK_TO_EMS = 101;
  public static final String IPC_EMS_TO_ACCEPTOR = "aeron:ipc?endpoint=ems-to-acceptor";
  public static final int STREAM_EMS_TO_ACCEPTOR = 102;

  // Hot-path venue execution
  public static final int STREAM_VENUE_TO_EMS_SIX = 200;
  public static final int STREAM_VENUE_TO_EMS_EUREX = 201;
  public static final int STREAM_EMS_DROPCOPY = 210;

  // Market-data multicast
  public static final int STREAM_MD_EQUITY_QUOTES = 300;
  public static final int STREAM_MD_DERIV_QUOTES = 301;
  public static final int STREAM_MD_FX_QUOTES = 302;
  public static final int STREAM_MD_EQUITY_TRADES = 310;
}
