-- liquibase formatted sql
-- changeset bako:005-create-payments

CREATE TABLE IF NOT EXISTS payments (
    id              BIGSERIAL       PRIMARY KEY,
    member_id       BIGINT          NOT NULL REFERENCES members(id),
    package_type    package_type    NOT NULL,
    credit_amount   INTEGER         NOT NULL,
    price           INTEGER         NOT NULL,
    status          payment_status  NOT NULL DEFAULT 'READY',
    pg_payment_key  VARCHAR(255),
    pg_order_id     VARCHAR(255)    NOT NULL UNIQUE,
    failed_reason   VARCHAR(500),
    paid_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_payments_member ON payments (member_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_payments_order ON payments (pg_order_id);
