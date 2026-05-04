package ch.swisstms.entitlements.dacs;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class DacsPermissionClientTest {

  private final DacsPermissionClient client = new DacsPermissionClient();

  @Test
  void publicRicsArePermittedForAnyUser() {
    // RIC has no PE-codes registered ⇒ public.
    assertThat(client.isPermitted("alice", "BOND.X")).isTrue();
  }

  @Test
  void userWithSupersetIsPermitted() {
    client.putUser("alice", Set.of("PE-001", "PE-002", "PE-003"));
    client.putRic("NESN.S", Set.of("PE-001", "PE-002"));
    assertThat(client.isPermitted("alice", "NESN.S")).isTrue();
  }

  @Test
  void userMissingOnePeCodeIsDenied() {
    client.putUser("bob", Set.of("PE-001"));
    client.putRic("NOVN.S", Set.of("PE-001", "PE-007"));
    assertThat(client.isPermitted("bob", "NOVN.S")).isFalse();
  }

  @Test
  void unknownUserIsDeniedFromGatedRics() {
    client.putRic("ROG.S", Set.of("PE-042"));
    assertThat(client.isPermitted("ghost", "ROG.S")).isFalse();
  }
}
