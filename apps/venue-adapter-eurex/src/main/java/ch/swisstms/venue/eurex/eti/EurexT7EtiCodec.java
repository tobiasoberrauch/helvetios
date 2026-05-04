package ch.swisstms.venue.eurex.eti;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Eurex T7 ETI binary codec (T292).
 *
 * <p>Hand-rolled little-endian codec mirroring the SBE templates pinned in {@code
 * contracts/sbe/eurex-t7/eurex-t7-eti.xml}. Keeping the codec hand-rolled (vs. SBE-codegen) so
 * Phase 15 can ship without a tools/codegen step on the build path; Phase 16 swaps in the generated
 * Java once the SBE schema is finalised.
 *
 * <p>Wire layout per ETI release-2026.1: 12-byte messageHeader + body. Body field offsets match the
 * {@code blockLength} declared in the SBE schema — every field is fixed-width.
 */
public final class EurexT7EtiCodec {

  /** Header constants — schemaId / version pinned to the SBE schema. */
  public static final int SCHEMA_ID = 11;

  public static final int SCHEMA_VERSION = 0;
  public static final int HEADER_SIZE = 12;

  /** Template IDs (subset). */
  public static final int TEMPLATE_NEW_ORDER_SINGLE = 10100;

  public static final int TEMPLATE_EXECUTION_REPORT = 10110;
  public static final int TEMPLATE_ORDER_CANCEL_REQUEST = 10120;

  public record NewOrderSingle(
      long senderCompId,
      long targetCompId,
      long securityId,
      long orderQty,
      long price,
      byte side, // 1=BUY, 2=SELL
      byte ordType, // 1=MARKET, 2=LIMIT, 3=STOP, 4=STOP_LIMIT
      long execTimestamp) {}

  public record ExecutionReport(
      long orderId,
      long execId,
      long lastQty,
      long lastPx,
      long cumQty,
      long leavesQty,
      long execTimestamp) {}

  private EurexT7EtiCodec() {}

  /** Encode a NewOrderSingle into the destination buffer; returns total bytes written. */
  public static int encodeNewOrderSingle(NewOrderSingle msg, ByteBuffer dst) {
    dst.order(ByteOrder.LITTLE_ENDIAN);
    int blockLength = 80;
    putHeader(dst, blockLength, TEMPLATE_NEW_ORDER_SINGLE, HEADER_SIZE + blockLength);
    dst.putLong(msg.senderCompId());
    dst.putLong(msg.targetCompId());
    dst.putLong(msg.securityId());
    dst.putLong(msg.orderQty());
    dst.putLong(msg.price());
    dst.put(msg.side());
    dst.put(msg.ordType());
    dst.put(new byte[6]); // padding to 8-byte alignment
    dst.putLong(msg.execTimestamp());
    // Padding to declared blockLength.
    while (dst.position() < HEADER_SIZE + blockLength) {
      dst.put((byte) 0);
    }
    return HEADER_SIZE + blockLength;
  }

  /** Decode an ExecutionReport from the source buffer (positioned past the header is also OK). */
  public static ExecutionReport decodeExecutionReport(ByteBuffer src) {
    src.order(ByteOrder.LITTLE_ENDIAN);
    Header header = readHeader(src);
    if (header.templateId() != TEMPLATE_EXECUTION_REPORT) {
      throw new IllegalArgumentException(
          "expected templateId " + TEMPLATE_EXECUTION_REPORT + ", got " + header.templateId());
    }
    long orderId = src.getLong();
    long execId = src.getLong();
    long lastQty = src.getLong();
    long lastPx = src.getLong();
    long cumQty = src.getLong();
    long leavesQty = src.getLong();
    long execTs = src.getLong();
    return new ExecutionReport(orderId, execId, lastQty, lastPx, cumQty, leavesQty, execTs);
  }

  // ---- header helpers ---------------------------------------------------------------------

  public record Header(int blockLength, int templateId, int schemaId, int version, int bodyLen) {}

  static void putHeader(ByteBuffer buf, int blockLength, int templateId, int bodyLen) {
    buf.putShort((short) blockLength);
    buf.putShort((short) templateId);
    buf.putShort((short) SCHEMA_ID);
    buf.putShort((short) SCHEMA_VERSION);
    buf.putInt(bodyLen);
  }

  static Header readHeader(ByteBuffer buf) {
    int blockLength = Short.toUnsignedInt(buf.getShort());
    int templateId = Short.toUnsignedInt(buf.getShort());
    int schemaId = Short.toUnsignedInt(buf.getShort());
    int version = Short.toUnsignedInt(buf.getShort());
    int bodyLen = buf.getInt();
    return new Header(blockLength, templateId, schemaId, version, bodyLen);
  }
}
