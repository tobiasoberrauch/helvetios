-- US1 / FR-001/002/003 — OMS Order aggregate.

CREATE TYPE side_enum AS ENUM ('BUY', 'SELL', 'SELL_SHORT');
CREATE TYPE ord_type_enum AS ENUM ('MARKET', 'LIMIT', 'STOP', 'STOP_LIMIT', 'FUNARI', 'MOO', 'LOO');
CREATE TYPE tif_enum AS ENUM ('DAY', 'IOC', 'FOK', 'GTC', 'GTD', 'OPG');
CREATE TYPE routing_mode_enum AS ENUM ('DMA', 'ALGO_WHEEL', 'CARE');
CREATE TYPE algo_strategy_enum AS ENUM ('VWAP', 'TWAP', 'POV', 'IS');
CREATE TYPE region_enum AS ENUM ('ZH', 'LD4', 'NY4', 'TY3');
CREATE TYPE ord_status_enum AS ENUM (
    'NEW', 'ACKNOWLEDGED', 'PARTIALLY_FILLED', 'FILLED',
    'PENDING_CANCEL', 'CANCELLED', 'PENDING_REPLACE',
    'REJECTED', 'EXPIRED', 'TRADE_BUSTED', 'BUSINESS_REJECTED'
);

CREATE TABLE order_aggregate (
    order_id           UUID PRIMARY KEY,
    cl_ord_id          TEXT NOT NULL,
    orig_cl_ord_id     TEXT,
    client_id          UUID NOT NULL,
    trader_id          UUID,
    region             region_enum NOT NULL,
    instrument_isin    CHAR(12) NOT NULL,
    instrument_mic     CHAR(4)  NOT NULL,
    side               side_enum NOT NULL,
    ord_type           ord_type_enum NOT NULL,
    time_in_force      tif_enum NOT NULL,
    expire_time        TIMESTAMPTZ,
    quantity           NUMERIC(18,8) NOT NULL CHECK (quantity > 0),
    price              NUMERIC(18,8),
    routing_mode       routing_mode_enum NOT NULL,
    algo_strategy      algo_strategy_enum,
    algo_parameters    JSONB,
    ord_status         ord_status_enum NOT NULL DEFAULT 'NEW',
    cum_qty            NUMERIC(18,8) NOT NULL DEFAULT 0,
    leaves_qty         NUMERIC(18,8) NOT NULL,
    avg_px             NUMERIC(18,8),
    submitted_at_biz   TIMESTAMPTZ NOT NULL,
    submitted_at_proc  TIMESTAMPTZ NOT NULL,
    last_updated_at    TIMESTAMPTZ NOT NULL,
    preferred_venue    CHAR(4),
    execution_venue    CHAR(4),
    parent_order_id    UUID REFERENCES order_aggregate(order_id),
    CONSTRAINT cl_ord_id_per_client_unique UNIQUE (client_id, cl_ord_id)
);

CREATE INDEX ix_order_client_status ON order_aggregate (client_id, ord_status);
CREATE INDEX ix_order_region_status ON order_aggregate (region, ord_status);
CREATE INDEX ix_order_parent ON order_aggregate (parent_order_id);
CREATE INDEX ix_order_submitted_at ON order_aggregate (submitted_at_biz);
