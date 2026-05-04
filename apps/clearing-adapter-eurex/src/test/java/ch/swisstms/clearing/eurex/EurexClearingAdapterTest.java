package ch.swisstms.clearing.eurex;

import static org.assertj.core.api.Assertions.assertThat;

import ch.swisstms.domain.health.HealthSnapshot;
import org.junit.jupiter.api.Test;

class EurexClearingAdapterTest {

  @Test
  void ccpIdAndHealthAreConsistent() {
    // No JmsTemplate needed for this snapshot — health() is I/O-free.
    EurexClearingAdapter adapter =
        new EurexClearingAdapter(null, null, null, null, "eurex.tradecapture");
    assertThat(adapter.ccpId()).isEqualTo("EUREX-C7");
    HealthSnapshot snap = adapter.health();
    assertThat(snap.venueId()).isEqualTo("EUREX-C7");
  }
}
