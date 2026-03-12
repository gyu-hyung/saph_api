-- liquibase formatted sql
-- changeset saph:006-create-jobs

CREATE TABLE IF NOT EXISTS jobs (
    id              BIGSERIAL       PRIMARY KEY,
    member_id       BIGINT          NOT NULL REFERENCES members(id),
    status          job_status      NOT NULL DEFAULT 'CREATED',
    current_step    job_step,
    progress        SMALLINT        DEFAULT 0,

    video_path      VARCHAR(500)    NOT NULL,
    original_name   VARCHAR(255)    NOT NULL,
    video_duration  INTEGER         NOT NULL,
    credit_used     INTEGER         NOT NULL,

    source_lang     VARCHAR(10)     NOT NULL DEFAULT 'auto',
    target_lang     VARCHAR(10)     NOT NULL DEFAULT 'ko',

    original_srt    VARCHAR(500),
    translated_srt  VARCHAR(500),

    error_message   VARCHAR(1000),
    retry_count     SMALLINT        NOT NULL DEFAULT 0,

    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_jobs_member
    ON jobs (member_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_jobs_status
    ON jobs (status) WHERE status IN ('CREATED', 'QUEUED', 'PROCESSING');
