#!/usr/bin/env bash
# Inserts seed.sql's dummy data into the local readora Postgres database.
# Safe to re-run — every insert in seed.sql uses deterministic UUIDs + ON CONFLICT DO NOTHING.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"

PGHOST="${PGHOST:-localhost}"
PGPORT="${PGPORT:-5432}"
PGDATABASE="${PGDATABASE:-readora}"
PGUSER="${PGUSER:-readora}"
PGPASSWORD="${PGPASSWORD:-readora}"

export PGPASSWORD

echo "Seeding $PGDATABASE @ $PGHOST:$PGPORT as $PGUSER..."
psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" -v ON_ERROR_STOP=1 -f "$ROOT_DIR/seed.sql"
echo "Done."
