package ch.swisstms.venue.marketaxess;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class TraxApaSessionTest {

  private final TraxApaSession session = new TraxApaSession();

  @Test
  void resetWindowDetectsBoundaries() {
    assertThat(session.inResetWindowAt(Instant.parse("2026-05-04T22:30:00Z"))).isFalse();
    assertThat(session.inResetWindowAt(Instant.parse("2026-05-04T23:00:00Z"))).isTrue();
    assertThat(session.inResetWindowAt(Instant.parse("2026-05-04T23:04:59Z"))).isTrue();
    assertThat(session.inResetWindowAt(Instant.parse("2026-05-04T23:05:00Z"))).isFalse();
  }

  @Test
  void submitOutsideWindowReturnsTradeReportId() {
    var trId = session.submit("DE000BAY0017", "BUY", 250, 48.20).toCompletableFuture().join();
    assertThat(trId).startsWith("TR-");
    assertThat(session.submittedCount()).isEqualTo(1);
  }
}
