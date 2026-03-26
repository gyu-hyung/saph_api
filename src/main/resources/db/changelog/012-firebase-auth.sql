-- liquibase formatted sql
-- changeset bako:012-firebase-auth

-- Add firebase_uid to members
ALTER TABLE members ADD COLUMN IF NOT EXISTS firebase_uid VARCHAR(128);
CREATE UNIQUE INDEX IF NOT EXISTS idx_members_firebase_uid ON members(firebase_uid) WHERE firebase_uid IS NOT NULL;

-- Drop refresh_tokens (Firebase manages refresh tokens)
DROP TABLE IF EXISTS refresh_tokens;
