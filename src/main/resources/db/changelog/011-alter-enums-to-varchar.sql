-- liquibase formatted sql
-- changeset saph:011-alter-enums-to-varchar

-- jobs.status: drop partial index and default before type change
DROP INDEX IF EXISTS idx_jobs_status;
ALTER TABLE jobs ALTER COLUMN status DROP DEFAULT;
ALTER TABLE jobs ALTER COLUMN status TYPE VARCHAR(50) USING status::text;
ALTER TABLE jobs ALTER COLUMN status SET DEFAULT 'CREATED';
CREATE INDEX IF NOT EXISTS idx_jobs_status ON jobs (status) WHERE status IN ('CREATED', 'QUEUED', 'PROCESSING');

-- jobs.current_step: no default/index, direct conversion
ALTER TABLE jobs ALTER COLUMN current_step TYPE VARCHAR(50) USING current_step::text;

-- members.status: drop partial indexes and default before type change
DROP INDEX IF EXISTS idx_members_email_active;
DROP INDEX IF EXISTS idx_members_email;
ALTER TABLE members ALTER COLUMN status DROP DEFAULT;
ALTER TABLE members ALTER COLUMN status TYPE VARCHAR(50) USING status::text;
ALTER TABLE members ALTER COLUMN status SET DEFAULT 'ACTIVE';
CREATE UNIQUE INDEX IF NOT EXISTS idx_members_email_active ON members (email) WHERE status != 'WITHDRAWN';
CREATE INDEX IF NOT EXISTS idx_members_email ON members (email) WHERE status = 'ACTIVE';

-- credit_logs.reason: no index, direct conversion
ALTER TABLE credit_logs ALTER COLUMN reason TYPE VARCHAR(50) USING reason::text;
