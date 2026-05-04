package ch.swisstms.venue.six.sti;

import static org.assertj.core.api.Assertions.assertThat;

import ch.swisstms.domain.health.LatencyTier;
import ch.swisstms.domain.ports.VenueGatewayPort;
import org.junit.jupiter.api.Test;

/**
 * T063 — Pact-Provider-Test (vereinfacht: Vertragstest, dass jeder Venue-Adapter genau die {@link
 * VenueGatewayPort}-Schnittstelle implementiert. Der echte Pact-Broker-Push kommt in Phase 5 (US3)
 * wenn der Adapter-Generator Live ist.
 */
class SixVenueAdapterPactTest {

  @Test
  void adapterImplementsVenueGatewayPort() {
    VenueGatewayPort adapter = new SixStiAdapter(false);
    assertThat(adapter.venueMic()).isEqualTo("XSWX");
    assertThat(adapter.tier()).isIn(LatencyTier.HOT, LatencyTier.WARM);
    assertThat(adapter.executions()).isNotNull();
    assertThat(adapter.health()).isNotNull();
  }
}
