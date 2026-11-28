-- V3: Stock Ledger, Balances & Stock Counts


-- ---- 1. Extend stock_mvts with ledger columns ----
ALTER TABLE stock_mvts
    ADD COLUMN occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN reason VARCHAR(40) NOT NULL DEFAULT 'OTHER',
    ADD COLUMN source_type VARCHAR(40),
    ADD COLUMN source_id VARCHAR(255),
    ADD COLUMN balance_after INTEGER,
    ADD COLUMN unit_cost NUMERIC(19,4);

-- Make product_id NOT NULL (was nullable in V1)
ALTER TABLE stock_mvts ALTER COLUMN product_id SET NOT NULL;

-- New indexes for stock_mvts
CREATE INDEX idx_stock_mvts_product_occurred ON stock_mvts (product_id, occurred_at DESC);
CREATE INDEX idx_stock_mvts_product_date ON stock_mvts (product_id, date_mvt);
CREATE INDEX idx_stock_mvts_reason_occurred ON stock_mvts (reason, occurred_at);

-- ---- 2. stock_balances ----
CREATE TABLE stock_balances (
    product_id VARCHAR(255) NOT NULL PRIMARY KEY
        CONSTRAINT fk_stock_balances_product REFERENCES products(id),
    on_hand INTEGER NOT NULL DEFAULT 0,
    reserved INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT now(),
    CONSTRAINT chk_on_hand_non_negative CHECK (on_hand >= 0)
);

-- ---- 3. stock_counts ----
CREATE TABLE stock_counts (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    created_at TIMESTAMP(6) NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT false,
    updated_at TIMESTAMP(6),
    updated_by VARCHAR(255),
    product_id VARCHAR(255) NOT NULL
        CONSTRAINT fk_stock_counts_product REFERENCES products(id),
    counted_qty INTEGER NOT NULL,
    system_qty INTEGER NOT NULL,
    variance INTEGER NOT NULL,
    note TEXT,
    counted_by VARCHAR(255) NOT NULL,
    counted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    adjustment_movement_id VARCHAR(255)
        CONSTRAINT fk_stock_counts_adjustment REFERENCES stock_mvts(id)
);

CREATE INDEX idx_stock_counts_product_counted ON stock_counts (product_id, counted_at DESC);
