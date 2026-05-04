package ch.swisstms.reconciler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Verifies the JSON parser used by the topology to extract the (SenderCompID, ClOrdID, ExecID) join
 * key from raw event payloads.
 */
class ReconcilerTopologyExtractKeyTest {

  @Test
  void extractsClOrdIdAndExecId() {
    String payload =
        "{\"orderId\":\"abc\",\"clOrdId\":\"ALICE-001\",\"venueExecutionId\":\"SIX-EXEC-42\"}";
    String key = ReconcilerTopology.extractJoinKey(payload);
    assertThat(key).contains("ALICE-001");
    assertThat(key).contains("SIX-EXEC-42");
  }

  @Test
  void fallsBackToExecutionIdIfNoVenueExecutionId() {
    String payload = "{\"clOrdId\":\"BOB-1\",\"executionId\":\"uuid-1234\"}";
    String key = ReconcilerTopology.extractJoinKey(payload);
    assertThat(key).contains("BOB-1");
    assertThat(key).contains("uuid-1234");
  }

  @Test
  void returnsUnknownForNullPayload() {
    assertThat(ReconcilerTopology.extractJoinKey(null)).isEqualTo("UNKNOWN");
  }
}
