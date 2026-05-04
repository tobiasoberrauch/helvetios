package ch.swisstms.venue.six.sti;

import static org.assertj.core.api.Assertions.assertThat;

import ch.swisstms.domain.health.HealthSnapshot;
import ch.swisstms.domain.health.LatencyTier;
import org.junit.jupiter.api.Test;

class SixStiHealthTest {

  @Test
  void healthMetadataIsConsistent() {
    SixStiAdapter adapter = new SixStiAdapter(false);
    assertThat(adapter.venueMic()).isEqualTo("XSWX");
    assertThat(adapter.tier()).isEqualTo(LatencyTier.WARM);

    HealthSnapshot snapshot = adapter.health();
    assertThat(snapshot.venueId()).isEqualTo("XSWX");
    assertThat(snapshot.status()).isEqualTo(HealthSnapshot.Status.CONNECTED);
  }
}
