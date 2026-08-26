-- Drops the readora database entirely — not a TRUNCATE, the database object itself is gone,
-- including its schemas, tables, and the pgvector extension. Run seed.sql afterward to get a
-- fully working database back — it recreates the database, schemas, tables, indexes, and
-- extensions itself, then reloads dataset/data/*.json; no service needs to run first.
--
-- Postgres refuses to drop a database you're currently connected to, and also refuses if any
-- other session still holds a connection to it — so this MUST be run while connected to a
-- different database (postgres, not readora), and terminates any other lingering connections
-- first.
--
-- Run from anywhere:
--   PGPASSWORD=readora psql -h localhost -p 5432 -U readora -d postgres \
--        -v ON_ERROR_STOP=1 -f dataset/drop-database.sql

SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE datname = 'readora' AND pid <> pg_backend_pid();

DROP DATABASE IF EXISTS readora;
