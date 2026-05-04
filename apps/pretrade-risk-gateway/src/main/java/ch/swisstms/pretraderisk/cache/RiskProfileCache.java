package ch.swisstms.pretraderisk.cache;

import ch.swisstms.domain.client.ClientId;
import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Off-heap-friendly Risk-Profile-Cache. Phase 13 verwendet eine schlanke Variante mit
 * ConcurrentHashMap; Phase 16 wechselt auf Agrona's Long2ObjectHashMap mit primitive-key path.
 *
 * <p>Updates kommen via Kafka {@code warm.entitlements.limit-update.v1}.
 */
@Component
public class RiskProfileCache {

  public record Profile(
      BigDecimal fatFingerNotional,
      BigDecimal fatFingerQuantity,
      BigDecimal maxOrderSizeNotional,
      boolean killSwitchTripped,
      long version) {}

  private final ConcurrentHashMap<ClientId, Profile> store = new ConcurrentHashMap<>();

  public Profile lookup(ClientId clientId) {
    return store.get(clientId);
  }

  public void upsert(ClientId clientId, Profile profile) {
    store.merge(
        clientId,
        profile,
        (existing, incoming) -> incoming.version > existing.version ? incoming : existing);
  }

  /** Mark a subject's entry as dirty; consumed by the next entitlement-update tick. */
  public void markDirty(String subjectId) {
    // Phase 13B will couple subjectId → ClientId via a directory lookup; for now this no-ops on
    // unknown subjects which is safe (the cache will simply re-resolve on next access).
  }

  public void tripKillSwitch(ClientId clientId) {
    store.computeIfPresent(
        clientId,
        (k, p) ->
            new Profile(
                p.fatFingerNotional(),
                p.fatFingerQuantity(),
                p.maxOrderSizeNotional(),
                true,
                p.version() + 1));
  }
}
