package ch.swisstms.audit;

import static org.assertj.core.api.Assertions.assertThat;

import ch.swisstms.audit_chain.AuditEvent;
import ch.swisstms.audit_chain.AuditEvent.ActorType;
import ch.swisstms.audit_chain.HashChainWriter;
import ch.swisstms.domain.common.Region;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * T273 — generate audit events in 4 regions, verify per-region chain validity, then verify
 * cross-region time-ordered concatenation reads cleanly without breaking the per-region hashes.
 *
 * <p>The cross-region merge isn't a single hash chain — each region has its own — but auditors MUST
 * be able to interleave events globally and trust each per-region segment.
 */
class CrossRegionAuditChainTest {

  @Test
  void perRegionChainsRemainValidAfterMerge() {
    List<AuditEvent> all = new ArrayList<>();
    for (Region region : List.of(Region.ZH, Region.LD4, Region.NY4, Region.TY3)) {
      HashChainWriter w = new HashChainWriter(region, 0L, null);
      for (int i = 0; i < 10; i++) {
        AuditEvent ev =
            w.append(
                ActorType.SERVICE,
                "test",
                "test.event",
                "Order",
                region + "-O-" + i,
                ("payload-" + i).getBytes(),
                null);
        all.add(ev);
      }
    }

    // Cross-region merge: order by bizTime so analysts see a global timeline.
    all.sort(Comparator.comparing(AuditEvent::bizTime));

    // Per-region chain validity check: filter to one region and assert seq is monotonic +
    // every entry's prevHash matches the previous entry's hash.
    for (Region region : List.of(Region.ZH, Region.LD4, Region.NY4, Region.TY3)) {
      var chain = all.stream().filter(e -> region == e.region()).toList();
      assertThat(chain).hasSize(10);
      for (int i = 1; i < chain.size(); i++) {
        assertThat(chain.get(i).seq()).isEqualTo(chain.get(i - 1).seq() + 1);
        assertThat(chain.get(i).prevHash()).isEqualTo(chain.get(i - 1).hash());
      }
    }
  }
}
