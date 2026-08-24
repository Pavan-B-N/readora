-- One-time local Postgres setup for Readora. Run once against a fresh Postgres instance.
-- ddl-auto: update (used by every service) only creates tables inside a schema that already
-- exists — it doesn't create the database or the schemas themselves, so this step is required.

CREATE DATABASE readora;

\c readora

CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS users;
CREATE SCHEMA IF NOT EXISTS catalog;
CREATE SCHEMA IF NOT EXISTS commerce;
CREATE SCHEMA IF NOT EXISTS payments;
CREATE SCHEMA IF NOT EXISTS ai;

-- ai-service's vector_store table (EmbeddingService / VectorStore) needs this.
CREATE EXTENSION IF NOT EXISTS vector;
