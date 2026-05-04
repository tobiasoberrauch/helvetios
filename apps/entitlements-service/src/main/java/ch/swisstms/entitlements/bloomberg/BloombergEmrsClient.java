package ch.swisstms.entitlements.bloomberg;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Bloomberg Entitlement Management & Reporting System (EMRS) client (T166).
 *
 * <p>The Bloomberg authority for "who is entitled to consume what". Every BLPAPI subscription
 * supplies a {@code (UUID, SerialNumber, AuthID)} triple plus seat type {@code BPS} (Bloomberg
 * Professional Service); EMRS maps these to a permission set with 24h TTL (Bloomberg's contractual
 * cache limit). After the TTL we MUST re-validate with EMRS or fail closed.
 */
@Component
public class BloombergEmrsClient {

  private static final Logger log = LoggerFactory.getLogger(BloombergEmrsClient.class);
  private static final Duration TTL = Duration.ofHours(24);

  public record IdentityKey(int uuid, int serialNumber, int authId, String seatType) {}

  public record CachedIdentity(
      IdentityKey key, java.util.Set<String> permissions, Instant expiresAt) {
    public boolean isExpired() {
      return Instant.now().isAfter(expiresAt);
    }
  }

  private final Map<IdentityKey, CachedIdentity> cache = new ConcurrentHashMap<>();

  /** Resolve and cache the permissions for the given identity. Re-validates if expired. */
  public CachedIdentity resolve(IdentityKey key) {
    CachedIdentity cached = cache.get(key);
    if (cached != null && !cached.isExpired()) {
      return cached;
    }
    CachedIdentity fresh = loadFromEmrs(key);
    cache.put(key, fresh);
    log.debug(
        "EMRS resolved identity uuid={} permissions={} ttl-until={}",
        key.uuid(),
        fresh.permissions().size(),
        fresh.expiresAt());
    return fresh;
  }

  /**
   * Stub that the test wiring overrides. Phase 14 swaps in a real BLPAPI {@code //blp/apiauth} call
   * with the EMRS-issued token returned from {@code Bloomberg-Authentication-Service}.
   */
  protected CachedIdentity loadFromEmrs(IdentityKey key) {
    return new CachedIdentity(key, java.util.Set.of(), Instant.now().plus(TTL));
  }

  public void put(IdentityKey key, java.util.Set<String> permissions) {
    cache.put(
        key, new CachedIdentity(key, java.util.Set.copyOf(permissions), Instant.now().plus(TTL)));
  }

  public int cacheSize() {
    return cache.size();
  }
}
