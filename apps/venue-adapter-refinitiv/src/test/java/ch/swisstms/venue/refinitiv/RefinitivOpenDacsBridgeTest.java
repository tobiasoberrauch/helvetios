package ch.swisstms.venue.refinitiv;

import static org.assertj.core.api.Assertions.assertThat;

import ch.swisstms.domain.instrument.InstrumentId;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RefinitivOpenDacsBridgeTest {

  private final RefinitivOpenDacsBridge bridge = new RefinitivOpenDacsBridge();
  private final InstrumentId nestle = new InstrumentId("CH0038863350", "XSWX");

  @Test
  void publicInstrumentIsPermitted() {
    assertThat(bridge.isPermitted("alice", nestle)).isTrue();
  }

  @Test
  void userWithRequiredPeCodeIsPermitted() {
    bridge.putInstrument(nestle, Set.of("SIX-LEVEL1"));
    bridge.putUser("alice", Set.of("SIX-LEVEL1", "FX-G10"));
    assertThat(bridge.isPermitted("alice", nestle)).isTrue();
  }

  @Test
  void userMissingPeCodeIsDenied() {
    bridge.putInstrument(nestle, Set.of("SIX-LEVEL1", "SIX-LEVEL2"));
    bridge.putUser("bob", Set.of("SIX-LEVEL1"));
    assertThat(bridge.isPermitted("bob", nestle)).isFalse();
  }

  @Test
  void revokingPeCodeStopsAccess() {
    bridge.putInstrument(nestle, Set.of("SIX-LEVEL1"));
    bridge.putUser("alice", Set.of("SIX-LEVEL1"));
    assertThat(bridge.isPermitted("alice", nestle)).isTrue();
    bridge.putUser("alice", Set.of()); // revoke
    assertThat(bridge.isPermitted("alice", nestle)).isFalse();
  }
}
