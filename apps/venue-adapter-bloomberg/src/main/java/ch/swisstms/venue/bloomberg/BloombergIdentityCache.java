package ch.swisstms.venue.bloomberg;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * BLPAPI Identity / EMRS sync cache (T177).
 *
 * <p>Bloomberg requires every market-data subscription to attach an Identity built from (UUID,
 * SerialNumber, AuthID, seatType=BPS). The Identity carries the EMRS permission set; platform code
 * MUST cache it for at most 24 h (Bloomberg contractual ceiling) and re-validate on expiry. After
 * failure to refresh we fail closed — the next subscription returns DENIED until the cache is
 * repopulated.
 *
 * <p>Phase 14 wires the real {@code //blp/apiauth} resolution; Phase 8 provides the cache surface
 * so unit + integration tests can drive entitlement-revocation scenarios.
 */
@Component
public class BloombergIdentityCache {

  private static final Logger log = LoggerFactory.getLogger(BloombergIdentityCache.class);
  private static final Duration TTL = Duration.ofHours(24);

  public record Identity(int uuid, int serialNumber, int authId, String seatType) {}

  public record CachedIdentity(Identity id, Set<String> permissions, Instant expiresAt) {
    public boolean isExpired() {
      return Instant.now().isAfter(expiresAt);
    }
  }

  private final Map<Identity, CachedIdentity> store = new ConcurrentHashMap<>();

  public void put(Identity id, Set<String> permissions) {
    store.put(id, new CachedIdentity(id, Set.copyOf(permissions), Instant.now().plus(TTL)));
    log.debug("Bloomberg identity cached uuid={} permissions={}", id.uuid(), permissions.size());
  }

  /** Returns the cached identity if fresh; null if missing or expired (caller must fail closed). */
  public CachedIdentity get(Identity id) {
    CachedIdentity ci = store.get(id);
    return ci == null || ci.isExpired() ? null : ci;
  }

  public int size() {
    return store.size();
  }
}
