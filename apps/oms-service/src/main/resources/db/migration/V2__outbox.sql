-- T066 — Outbox pattern: every state change is appended to `outbox`,
-- Debezium streams the table to Kafka topic `cold.oms.event.v1`.

CREATE TABLE outbox (
    outbox_id      UUID PRIMARY KEY,
    aggregate_type TEXT NOT NULL DEFAULT 'Order',
    aggregate_id   UUID NOT NULL,
    topic          TEXT NOT NULL,
    payload        JSONB NOT NULL,
    headers        JSONB,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_outbox_aggregate ON outbox (aggregate_type, aggregate_id);
CREATE INDEX ix_outbox_created   ON outbox (created_at);

-- Debezium reads incrementally; the outbox table is purged after publication
-- by an in-process janitor running every 5 minutes (see OutboxJanitor.java).
