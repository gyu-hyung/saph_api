-- liquibase formatted sql
-- changeset saph:008-add-dual-srt

ALTER TABLE jobs ADD COLUMN IF NOT EXISTS dual_srt VARCHAR(500);
