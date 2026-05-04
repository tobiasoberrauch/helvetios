package ch.swisstms.oms.infra;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox")
public class OutboxEntity {

  @Id
  @Column(name = "outbox_id")
  private UUID outboxId;

  @Column(name = "aggregate_type", nullable = false)
  private String aggregateType = "Order";

  @Column(name = "aggregate_id", nullable = false)
  private UUID aggregateId;

  @Column(name = "topic", nullable = false)
  private String topic;

  @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
  private String payload;

  @Column(name = "headers", columnDefinition = "jsonb")
  private String headers;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  protected OutboxEntity() {}

  public OutboxEntity(
      UUID outboxId, UUID aggregateId, String topic, String payload, String headers) {
    this.outboxId = outboxId;
    this.aggregateId = aggregateId;
    this.topic = topic;
    this.payload = payload;
    this.headers = headers;
  }

  public UUID getOutboxId() {
    return outboxId;
  }

  public String getAggregateType() {
    return aggregateType;
  }

  public UUID getAggregateId() {
    return aggregateId;
  }

  public String getTopic() {
    return topic;
  }

  public String getPayload() {
    return payload;
  }

  public String getHeaders() {
    return headers;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
