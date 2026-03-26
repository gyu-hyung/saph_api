-- liquibase formatted sql
-- changeset bako:007-create-credit-logs

CREATE TABLE IF NOT EXISTS credit_logs (
    id              BIGSERIAL       PRIMARY KEY,
    member_id       BIGINT          NOT NULL REFERENCES members(id),
    job_id          BIGINT          REFERENCES jobs(id),
    payment_id      BIGINT          REFERENCES payments(id),
    change_amount   INTEGER         NOT NULL,
    reason          credit_reason   NOT NULL,
    balance_after   INTEGER         NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_credit_logs_member
    ON credit_logs (member_id, created_at DESC);
