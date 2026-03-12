-- liquibase formatted sql
-- changeset saph:003-create-refresh-tokens

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          BIGSERIAL       PRIMARY KEY,
    member_id   BIGINT          NOT NULL REFERENCES members(id),
    token       VARCHAR(512)    NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ     NOT NULL,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_member ON refresh_tokens (member_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires ON refresh_tokens (expires_at);
