-- ============================================================
-- Migration 003: Admin management (Phase 7)
-- Safe for existing databases (idempotent, run on every boot).
-- Adds account activation for users and listing approval for
-- products. Fresh installs already get the columns from
-- db/schema.sql; this migration only fills the gap for databases
-- created before the admin management feature existed.
-- ============================================================

ALTER TABLE users     ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE products  ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;