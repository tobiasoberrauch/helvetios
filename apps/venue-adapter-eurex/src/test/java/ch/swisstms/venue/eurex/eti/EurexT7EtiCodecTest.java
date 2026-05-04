package ch.swisstms.venue.eurex.eti;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class EurexT7EtiCodecTest {

  @Test
  void encodesNewOrderSingleWithCorrectHeader() {
    var msg =
        new EurexT7EtiCodec.NewOrderSingle(
            42L, 99L, 12345L, 1000L, 105_42_000_000L, (byte) 1, (byte) 2, 1_700_000_000_000_000L);
    ByteBuffer buf = ByteBuffer.allocate(256);
    int written = EurexT7EtiCodec.encodeNewOrderSingle(msg, buf);
    assertThat(written).isEqualTo(EurexT7EtiCodec.HEADER_SIZE + 80);

    buf.flip();
    var header = EurexT7EtiCodec.readHeader(buf);
    assertThat(header.templateId()).isEqualTo(EurexT7EtiCodec.TEMPLATE_NEW_ORDER_SINGLE);
    assertThat(header.schemaId()).isEqualTo(EurexT7EtiCodec.SCHEMA_ID);
    assertThat(header.blockLength()).isEqualTo(80);
    // First field after header is senderCompId (uint64).
    assertThat(buf.getLong()).isEqualTo(42L);
  }

  @Test
  void roundTripsExecutionReport() {
    // Build a synthetic ExecutionReport buffer using the header format.
    ByteBuffer buf = ByteBuffer.allocate(256).order(java.nio.ByteOrder.LITTLE_ENDIAN);
    EurexT7EtiCodec.putHeader(
        buf, 64, EurexT7EtiCodec.TEMPLATE_EXECUTION_REPORT, EurexT7EtiCodec.HEADER_SIZE + 64);
    buf.putLong(7L); // orderId
    buf.putLong(8L); // execId
    buf.putLong(100L); // lastQty
    buf.putLong(105_50_000_000L); // lastPx
    buf.putLong(100L); // cumQty
    buf.putLong(0L); // leavesQty
    buf.putLong(1_700_000_000_000_000L); // execTimestamp
    buf.flip();

    var report = EurexT7EtiCodec.decodeExecutionReport(buf);
    assertThat(report.orderId()).isEqualTo(7L);
    assertThat(report.execId()).isEqualTo(8L);
    assertThat(report.lastQty()).isEqualTo(100L);
    assertThat(report.cumQty()).isEqualTo(100L);
    assertThat(report.leavesQty()).isZero();
  }
}
