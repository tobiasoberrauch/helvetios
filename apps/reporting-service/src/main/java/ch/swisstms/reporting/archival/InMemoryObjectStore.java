package ch.swisstms.reporting.archival;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * In-memory {@link WormArchivalWriter.ObjectStore} for dev / unit tests.
 *
 * <p>Only active in {@code dev} and {@code test} profiles. The {@code prod} profile binds {@link
 * S3ObjectStore} (Phase 14).
 */
@Component
@Profile({"dev", "test", "default"})
public class InMemoryObjectStore implements WormArchivalWriter.ObjectStore {

  public record StoredObject(
      byte[] payload, String contentType, Instant retainUntil, String sha256) {}

  private final Map<String, StoredObject> store = new ConcurrentHashMap<>();

  @Override
  public void putWithObjectLock(
      String bucket,
      String key,
      byte[] payload,
      String contentType,
      Instant retainUntil,
      String sha256) {
    String composite = bucket + "/" + key;
    if (store.containsKey(composite)) {
      throw new IllegalStateException("Object Lock COMPLIANCE — cannot overwrite " + composite);
    }
    store.put(composite, new StoredObject(payload.clone(), contentType, retainUntil, sha256));
  }

  public StoredObject get(String bucket, String key) {
    return store.get(bucket + "/" + key);
  }

  public int size() {
    return store.size();
  }
}
