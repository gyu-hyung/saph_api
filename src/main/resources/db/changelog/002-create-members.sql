-- liquibase formatted sql
-- changeset bako:002-create-members

CREATE TABLE IF NOT EXISTS members (
    id          BIGSERIAL       PRIMARY KEY,
    email       VARCHAR(255),
    password    VARCHAR(255),
    nickname    VARCHAR(50),
    status      member_status   NOT NULL DEFAULT 'ACTIVE',
    deleted_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_members_email_active
    ON members (email) WHERE status != 'WITHDRAWN';

CREATE INDEX IF NOT EXISTS idx_members_email
    ON members (email) WHERE status = 'ACTIVE';
