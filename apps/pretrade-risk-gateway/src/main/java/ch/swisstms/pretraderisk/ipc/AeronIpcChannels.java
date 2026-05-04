package ch.swisstms.pretraderisk.ipc;

/**
 * Aeron IPC channel + stream-id constants (T236).
 *
 * <p>The pre-trade risk gateway uses three Aeron IPC streams within the shared {@code aeron:ipc}
 * channel:
 *
 * <ul>
 *   <li>{@link #INBOUND_STREAM_ID} (100) — orders from {@code inbound-fix-acceptor}.
 *   <li>{@link #OUTBOUND_TO_EMS_STREAM_ID} (101) — approved orders forwarded to {@code
 *       ems-service}.
 *   <li>{@link #OUTBOUND_TO_ACCEPTOR_STREAM_ID} (102) — risk decisions returned to the acceptor (so
 *       it can build the right Reject(35=3) / BusinessMessageReject(35=j)).
 * </ul>
 *
 * <p>Channel + stream IDs are constants so the acceptor and the risk gateway can be deployed in the
 * same Pod and share the same Aeron MediaDriver without runtime coupling.
 */
public final class AeronIpcChannels {

  public static final String CHANNEL = "aeron:ipc";

  public static final int INBOUND_STREAM_ID = 100;
  public static final int OUTBOUND_TO_EMS_STREAM_ID = 101;
  public static final int OUTBOUND_TO_ACCEPTOR_STREAM_ID = 102;

  private AeronIpcChannels() {}
}
