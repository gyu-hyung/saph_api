-- liquibase formatted sql
-- changeset saph:009-alter-package-type-to-varchar

-- Convert package_type column from enum to varchar
ALTER TABLE payments
ALTER COLUMN package_type TYPE VARCHAR(50) USING package_type::text;
