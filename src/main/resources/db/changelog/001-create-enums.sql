-- liquibase formatted sql
-- changeset saph:001-create-enums

DO $$ BEGIN
    CREATE TYPE job_status AS ENUM (
        'CREATED',
        'QUEUED',
        'PROCESSING',
        'COMPLETED',
        'FAILED'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE job_step AS ENUM (
        'AUDIO_EXTRACTION',
        'STT',
        'TRANSLATION',
        'SUBTITLE_BUILD'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE package_type AS ENUM (
        'MIN_10',
        'MIN_30',
        'HOUR_1',
        'HOUR_3',
        'HOUR_10'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE payment_status AS ENUM (
        'READY',
        'DONE',
        'FAILED',
        'CANCELED'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE credit_reason AS ENUM (
        'PURCHASE',
        'USAGE',
        'REFUND',
        'ADMIN'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE member_status AS ENUM (
        'ACTIVE',
        'BLOCKED',
        'WITHDRAWN'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;
