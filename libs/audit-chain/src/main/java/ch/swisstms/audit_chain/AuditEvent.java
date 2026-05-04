package ch.swisstms.audit_chain;

import ch.swisstms.domain.common.Region;
import java.time.Instant;
import java.util.UUID;

public record AuditEvent(
    UUID auditEventId,
    long seq,
    Region region,
    ActorType actorType,
    String actorId,
    String action,
    String targetType,
    String targetId,
    byte[] payload,
    Instant bizTime,
    Instant procTime,
    byte[] prevHash,
    byte[] hash,
    String traceparent) {
  public enum ActorType {
    USER,
    SERVICE,
    EXTERNAL
  }
}
