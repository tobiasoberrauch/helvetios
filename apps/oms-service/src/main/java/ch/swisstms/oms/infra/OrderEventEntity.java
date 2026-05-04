package ch.swisstms.oms.infra;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_event")
public class OrderEventEntity {

  @Id
  @Column(name = "event_id")
  private UUID eventId;

  @Column(name = "order_id", nullable = false)
  private UUID orderId;

  @Column(name = "seq", nullable = false)
  private long seq;

  @Column(name = "event_type", nullable = false)
  private String eventType;

  @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
  private String payload;

  @Column(name = "biz_time", nullable = false)
  private Instant bizTime;

  @Column(name = "proc_time", nullable = false)
  private Instant procTime;

  @Column(name = "prev_hash", nullable = false)
  private byte[] prevHash;

  @Column(name = "hash", nullable = false)
  private byte[] hash;

  @Column(name = "traceparent")
  private String traceparent;

  protected OrderEventEntity() {}

  public OrderEventEntity(
      UUID eventId,
      UUID orderId,
      long seq,
      String eventType,
      String payload,
      Instant bizTime,
      Instant procTime,
      byte[] prevHash,
      byte[] hash,
      String traceparent) {
    this.eventId = eventId;
    this.orderId = orderId;
    this.seq = seq;
    this.eventType = eventType;
    this.payload = payload;
    this.bizTime = bizTime;
    this.procTime = procTime;
    this.prevHash = prevHash;
    this.hash = hash;
    this.traceparent = traceparent;
  }

  public UUID getEventId() {
    return eventId;
  }

  public UUID getOrderId() {
    return orderId;
  }

  public long getSeq() {
    return seq;
  }

  public String getEventType() {
    return eventType;
  }

  public String getPayload() {
    return payload;
  }

  public Instant getBizTime() {
    return bizTime;
  }

  public Instant getProcTime() {
    return procTime;
  }

  public byte[] getPrevHash() {
    return prevHash;
  }

  public byte[] getHash() {
    return hash;
  }

  public String getTraceparent() {
    return traceparent;
  }
}
