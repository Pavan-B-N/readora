-- Drops the readora database AND the readora role, so the next setup.sql run recreates both
-- from scratch — including the role's SUPERUSER attribute. Dropping only the database and
-- leaving a stale (possibly non-superuser) role behind is exactly how ai-service's
-- "permission denied to create extension vector" error creeps back in after a cleanup.
DROP DATABASE IF EXISTS readora;
DROP ROLE IF EXISTS readora;
