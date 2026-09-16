-- ============================================================
-- Migration 001: Mobile Number + OTP authentication
-- Safe for existing databases (idempotent, run on every boot).
-- Existing users are preserved; rows without a mobile number
-- receive a deterministic placeholder (90000 + zero-padded id)
-- so the NOT NULL / UNIQUE rules hold without deleting data.
-- ============================================================

ALTER TABLE users ADD COLUMN IF NOT EXISTS mobile_number VARCHAR(10);

UPDATE users SET mobile_number = '90000' || LPAD(id, 5, '0') WHERE mobile_number IS NULL;

ALTER TABLE users ALTER COLUMN mobile_number SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_mobile_number ON users (mobile_number);

CREATE TABLE IF NOT EXISTS otp_verifications (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    otp_hash   VARCHAR(64)  NOT NULL,
    expires_at TIMESTAMP    NOT NULL,
    attempts   INT          NOT NULL DEFAULT 0,
    verified   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_otp_verifications_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_otp_verifications_user_id ON otp_verifications (user_id);
