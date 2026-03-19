-- liquibase formatted sql
-- changeset saph:010-alter-payment-status-to-varchar

ALTER TABLE payments
ALTER COLUMN status TYPE VARCHAR(50) USING status::text;
