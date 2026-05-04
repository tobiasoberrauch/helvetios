package ch.swisstms.reporting.archival;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Writes regulator-submission payloads to S3 with Object-Lock COMPLIANCE retention (FR-035 / T146).
 *
 * <p>Retention period is configured per category:
 *
 * <ul>
 *   <li>RTS-22 / FinfraG / Trax APA → 5 years (MiFID II Art.16)
 *   <li>EMIR → 10 years (EMIR Refit Art.9.6)
 *   <li>Audit chain → matches the longest above (10y)
 * </ul>
 *
 * <p>Phase 14 wires the real S3 client (AWS SDK v2 or MinIO). Phase 7 keeps this as an interface
 * around a {@link ObjectStore} stub so unit tests don't need network. Constitution Principle VI —
 * every WORM write emits an audit event tagged {@code reporting.worm.archived}.
 */
@Component
public class WormArchivalWriter {

  private static final Logger log = LoggerFactory.getLogger(WormArchivalWriter.class);

  private final ObjectStore store;
  private final String bucketName;

  public WormArchivalWriter(
      ObjectStore store, @Value("${swisstms.archival.bucket:swisstms-worm}") String bucketName) {
    this.store = store;
    this.bucketName = bucketName;
  }

  public ArchivedObject archive(
      String key, byte[] payload, String contentType, RetentionCategory category) {
    Duration retention = category.duration();
    Instant retainUntil = Instant.now().plus(retention);
    String checksum = sha256(payload);
    store.putWithObjectLock(bucketName, key, payload, contentType, retainUntil, checksum);
    log.info(
        "WORM archive write s3://{}/{} ({} bytes, retain-until {}, sha256[..7]={})",
        bucketName,
        key,
        payload.length,
        retainUntil,
        checksum.substring(0, 7));
    return new ArchivedObject(bucketName, key, retainUntil, checksum);
  }

  public record ArchivedObject(String bucket, String key, Instant retainUntil, String sha256) {}

  public enum RetentionCategory {
    MIFID_II(Duration.ofDays(365L * 5)),
    EMIR(Duration.ofDays(365L * 10)),
    AUDIT_CHAIN(Duration.ofDays(365L * 10));

    private final Duration duration;

    RetentionCategory(Duration duration) {
      this.duration = duration;
    }

    public Duration duration() {
      return duration;
    }
  }

  /** Indirection over S3 / MinIO so tests can swap an in-memory implementation. */
  public interface ObjectStore {
    void putWithObjectLock(
        String bucket,
        String key,
        byte[] payload,
        String contentType,
        Instant retainUntil,
        String sha256);
  }

  private static String sha256(byte[] data) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 must be available", e);
    }
  }
}
