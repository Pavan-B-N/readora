-- One-time local setup: creates the readora role and the readora database it owns.
--
-- SUPERUSER is deliberate, not a shortcut: creating a Postgres *extension* always requires
-- superuser, no matter who issues the command, and ai-service's Flyway migrations need to
-- run "CREATE EXTENSION IF NOT EXISTS vector". Matches what Docker's official Postgres image
-- does automatically for POSTGRES_USER, so behavior stays the same whether Postgres is
-- containerized or native.
--
-- Not idempotent — run once against a fresh Postgres instance. To start over, run cleanup.sql
-- first — it drops both the database and this role, so re-running this whole script recreates
-- a clean SUPERUSER role rather than leaving a stale, possibly-non-superuser one behind.
CREATE ROLE readora WITH LOGIN SUPERUSER PASSWORD 'readora';
CREATE DATABASE readora OWNER readora;
