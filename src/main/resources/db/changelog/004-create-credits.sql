-- liquibase formatted sql
-- changeset bako:004-create-credits

CREATE TABLE IF NOT EXISTS credits (
    id          BIGSERIAL   PRIMARY KEY,
    member_id   BIGINT      NOT NULL UNIQUE REFERENCES members(id),
    balance_min INTEGER     NOT NULL DEFAULT 0,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
