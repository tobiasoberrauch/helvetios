package ch.swisstms.audit_chain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.swisstms.audit_chain.AuditEvent.ActorType;
import ch.swisstms.domain.common.Region;
import java.util.ArrayList;
import java.util.List;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import org.junit.jupiter.api.Test;

/** Property tests for the audit hash chain (Constitution Principle VII + VI). */
class HashChainWriterTest {

  @Test
  void chainOfFiveEventsVerifies() {
    HashChainWriter w = new HashChainWriter(Region.ZH, 0L, new byte[32]);
    List<AuditEvent> events = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      events.add(
          w.append(
              ActorType.SERVICE,
              "oms-service",
              "order.submit",
              "Order",
              "order-" + i,
              ("payload-" + i).getBytes(),
              null));
    }
    HashChainWriter.verifyChain(events); // throws on mismatch
  }

  @Test
  void tampering_a_single_payload_breaks_the_chain() {
    HashChainWriter w = new HashChainWriter(Region.ZH, 0L, new byte[32]);
    List<AuditEvent> events = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      events.add(
          w.append(
              ActorType.SERVICE,
              "oms-service",
              "order.submit",
              "Order",
              "order-" + i,
              ("payload-" + i).getBytes(),
              null));
    }
    // Replace event #1 with tampered payload but same hash field
    AuditEvent ev = events.get(1);
    AuditEvent tampered =
        new AuditEvent(
            ev.auditEventId(),
            ev.seq(),
            ev.region(),
            ev.actorType(),
            ev.actorId(),
            ev.action(),
            ev.targetType(),
            ev.targetId(),
            "TAMPERED".getBytes(),
            ev.bizTime(),
            ev.procTime(),
            ev.prevHash(),
            ev.hash(),
            ev.traceparent());
    events.set(1, tampered);
    assertThatThrownBy(() -> HashChainWriter.verifyChain(events))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("hash mismatch");
  }

  @Property(tries = 100)
  boolean monotonic_seq_under_random_writes(@ForAll int eventCount) {
    int n = Math.abs(eventCount % 50);
    HashChainWriter w = new HashChainWriter(Region.LD4, 0L, new byte[32]);
    long lastSeq = 0;
    for (int i = 0; i < n; i++) {
      AuditEvent ev =
          w.append(
              ActorType.USER,
              "alice",
              "killswitch.trip",
              "KillScope",
              "trader-42",
              ("p" + i).getBytes(),
              null);
      assertThat(ev.seq()).isGreaterThan(lastSeq);
      lastSeq = ev.seq();
    }
    return true;
  }
}
