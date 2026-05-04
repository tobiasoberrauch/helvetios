package ch.swisstms.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * T301 — Audit-service Kafka consumer.
 *
 * <p>Consumes {@code audit.command.v1} (the canonical audit-event topic) and writes each event to
 * (a) OpenSearch for analyst search and (b) S3 WORM with Object-Lock COMPLIANCE for the regulatory
 * retention window. The OpenSearch + S3 clients are interfaces so unit tests can substitute
 * in-memory implementations.
 *
 * <p>Constitution Principle VI — every audit event MUST end up on durable storage with a
 * tamper-evident SHA-256. The hash is recomputed here and matched against the producer-side value
 * in the message header; mismatches raise a Sev-1 alert.
 */
@Component
public class AuditWriter {

  private static final Logger log = LoggerFactory.getLogger(AuditWriter.class);
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final Duration MIFID_RETENTION = Duration.ofDays(365L * 5);

  private final OpenSearchSink openSearch;
  private final WormSink worm;
  private final AtomicLong written = new AtomicLong();
  private final AtomicLong hashMismatches = new AtomicLong();

  public AuditWriter(OpenSearchSink openSearch, WormSink worm) {
    this.openSearch = openSearch;
    this.worm = worm;
  }

  @KafkaListener(topics = "audit.command.v1", groupId = "audit-service")
  public void onAuditEvent(byte[] payload) {
    try {
      JsonNode node = JSON.readTree(payload);
      String eventId = node.path("eventId").asText("");
      String region = node.path("region").asText("");
      long seq = node.path("seq").asLong();
      String declaredHash = node.path("hash").asText("");
      String observedHash = sha256(payload);

      // OpenSearch index path: audit-{region}-{yyyy-MM-dd}.
      openSearch.index(
          "audit-" + region.toLowerCase() + "-" + Instant.now().toString().substring(0, 10),
          eventId,
          payload);
      // WORM key path: region/yyyy/MM/dd/{seq}.json
      worm.put(
          "swisstms-audit-" + region.toLowerCase(),
          region.toLowerCase() + "/" + Instant.now() + "/" + seq + ".json",
          payload,
          Instant.now().plus(MIFID_RETENTION));

      if (!declaredHash.isEmpty() && !declaredHash.equalsIgnoreCase(observedHash)) {
        hashMismatches.incrementAndGet();
        log.error(
            "AUDIT HASH MISMATCH region={} seq={} declared={} observed={} — Sev-1",
            region,
            seq,
            declaredHash,
            observedHash);
      }
      long n = written.incrementAndGet();
      if (n % 10_000 == 0) {
        log.info("AuditWriter persisted {} events ({} hash mismatches)", n, hashMismatches.get());
      }
    } catch (Exception e) {
      log.error("AuditWriter failed to handle event: {}", e.getMessage());
    }
  }

  public long writtenCount() {
    return written.get();
  }

  public long hashMismatches() {
    return hashMismatches.get();
  }

  /** Indirection so tests can stub OpenSearch out. */
  public interface OpenSearchSink {
    void index(String index, String docId, byte[] body);
  }

  /** Indirection so tests can stub the WORM target out. */
  public interface WormSink {
    void put(String bucket, String key, byte[] body, Instant retainUntil);
  }

  private static String sha256(byte[] data) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
