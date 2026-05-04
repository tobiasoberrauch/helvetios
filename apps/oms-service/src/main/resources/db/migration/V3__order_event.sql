-- T067 — Append-only event store for the Order aggregate (FR-003).
-- SHA-256 hash chain (Constitution Principle VI) — every event references
-- the previous event's hash on the same order_id.

CREATE TABLE order_event (
    event_id    UUID PRIMARY KEY,
    order_id    UUID NOT NULL REFERENCES order_aggregate(order_id),
    seq         BIGINT NOT NULL,
    event_type  TEXT NOT NULL,
    payload     JSONB NOT NULL,
    biz_time    TIMESTAMPTZ NOT NULL,
    proc_time   TIMESTAMPTZ NOT NULL,
    prev_hash   BYTEA NOT NULL,
    hash        BYTEA NOT NULL,
    traceparent TEXT,
    CONSTRAINT order_event_seq_unique UNIQUE (order_id, seq)
);

CREATE INDEX ix_order_event_biz_time ON order_event (biz_time);
CREATE INDEX ix_order_event_seq ON order_event (order_id, seq);

-- Verify-chain trigger — refuses INSERTs whose prev_hash doesn't match
-- the most recent event's hash for the same order_id. Daily reconciliation
-- in `apps/audit-service/` re-verifies the full chain.
CREATE OR REPLACE FUNCTION enforce_event_chain() RETURNS TRIGGER AS $$
DECLARE
    expected_prev_hash BYTEA;
    expected_seq BIGINT;
BEGIN
    SELECT hash, seq INTO expected_prev_hash, expected_seq
    FROM order_event WHERE order_id = NEW.order_id ORDER BY seq DESC LIMIT 1;

    IF expected_prev_hash IS NULL THEN
        IF NEW.seq != 1 THEN
            RAISE EXCEPTION 'First event for order_id=% must have seq=1, got %', NEW.order_id, NEW.seq;
        END IF;
    ELSE
        IF NEW.seq != expected_seq + 1 THEN
            RAISE EXCEPTION 'Event seq for order_id=% must be %, got %',
                NEW.order_id, expected_seq + 1, NEW.seq;
        END IF;
        IF NEW.prev_hash != expected_prev_hash THEN
            RAISE EXCEPTION 'prev_hash mismatch for order_id=% seq=%', NEW.order_id, NEW.seq;
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tg_order_event_chain BEFORE INSERT ON order_event
    FOR EACH ROW EXECUTE FUNCTION enforce_event_chain();
