package ch.swisstms.audit_chain;

import ch.swisstms.audit_chain.AuditEvent.ActorType;
import ch.swisstms.domain.common.Region;
import ch.swisstms.time_sync.RegulatoryClock;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SHA-256 hash chain für jedes State-änderungsbringende Kommando.
 *
 * <p>Constitution Principle VI (NICHT-VERHANDELBAR): {@code hash = SHA256(prevHash ||
 * canonical(payload))}. Daily verification reads back the chain and checks every link; any mismatch
 * is a Sev-1 incident.
 *
 * <p>Diese Klasse ist die in-process Schreibseite. Der Kafka-Producer für `audit.command.v1` wird
 * in `apps/audit-service/` gebaut (Phase 15).
 */
public final class HashChainWriter {

  private final Region region;
  private final AtomicLong seq;
  private volatile byte[] previousHash;

  public HashChainWriter(Region region, long initialSeq, byte[] initialPrevHash) {
    this.region = Objects.requireNonNull(region);
    this.seq = new AtomicLong(initialSeq);
    this.previousHash = initialPrevHash != null ? initialPrevHash.clone() : new byte[32];
  }

  public AuditEvent append(
      ActorType actorType,
      String actorId,
      String action,
      String targetType,
      String targetId,
      byte[] payload,
      String traceparent) {
    byte[] prev = this.previousHash;
    byte[] hash = sha256(prev, payload);
    Instant bizTime = RegulatoryClock.nowBiz();
    AuditEvent event =
        new AuditEvent(
            UUID.randomUUID(),
            seq.incrementAndGet(),
            region,
            actorType,
            actorId,
            action,
            targetType,
            targetId,
            payload.clone(),
            bizTime,
            bizTime, // procTime ≈ bizTime in-process
            prev,
            hash,
            traceparent);
    this.previousHash = hash;
    return event;
  }

  private static byte[] sha256(byte[] prev, byte[] payload) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.update(prev);
      md.update(payload);
      return md.digest();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }

  /**
   * Verifiziere eine Kette von AuditEvents. Wirft IllegalStateException, sobald irgendein Link
   * bricht.
   */
  public static void verifyChain(java.util.List<AuditEvent> events) {
    AuditEvent prev = null;
    for (AuditEvent ev : events) {
      byte[] expectedHash = sha256(ev.prevHash(), ev.payload());
      if (!java.util.Arrays.equals(expectedHash, ev.hash())) {
        throw new IllegalStateException("Audit chain hash mismatch at seq=" + ev.seq());
      }
      if (prev != null && !java.util.Arrays.equals(prev.hash(), ev.prevHash())) {
        throw new IllegalStateException(
            "Audit chain broken at seq="
                + ev.seq()
                + " — prevHash does not match previous event hash");
      }
      prev = ev;
    }
  }
}
