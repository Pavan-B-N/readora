# Readora

A 10-service marketplace for physical and virtual books — Spring Boot microservices behind an API gateway, three React frontends (customer, admin, delivery agent), Postgres, Kafka, and Redis.

This doc covers getting a fresh checkout running locally. Two paths:

- **[Path A — Docker Compose](#path-a--everything-in-docker)**: one command, everything containerized. Simplest, but slower to iterate on since every code change needs a rebuild.
- **[Path B — Hybrid local dev](#path-b--hybrid-local-dev)**: infra (Postgres/Kafka/Redis) in Docker or installed natively, services run individually from Maven/IntelliJ. What you want for actually working on the code.

## Prerequisites

| Tool | Version | Needed for |
|---|---|---|
| Java (JDK) | 21 | Building/running every backend service |
| Maven | any recent 3.x | Building the backend |
| Node.js + npm | recent LTS | The three frontends |
| Docker + Docker Compose | any recent | Path A, or just infra in Path B |
| PostgreSQL | 16+ with the [pgvector](https://github.com/pgvector/pgvector) extension available | Path B without Docker |

Multiple services default to the same Kafka topics/env var names — you don't need to configure per-service infra endpoints beyond what's below.

---

## Path A — everything in Docker

Every backend service's image bundles the OpenTelemetry Java agent from a local file (`services/otel-agent.jar`) rather than downloading it during the build — 10 services each pulling the same ~24MB file from GitHub in parallel is enough to blow past Docker's build timeout on an unlucky connection. Fetch it once per machine (re-run this if you ever bump the `v2.31.1` version pinned in each `services/*/Dockerfile`):

```bash
curl -fL -o services/otel-agent.jar \
  https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.31.1/opentelemetry-javaagent.jar
```

```bash
cp .env.example .env
# fill in .env — see "Environment variables" below for what's required vs optional
docker compose up --build
```

That's it — Postgres, Redis, Kafka, the OpenTelemetry collector, all 10 backend services, and all 3 frontends start as containers, wired together on the `readora` Docker network. Postgres starts **empty**; each service creates its own schema/tables via Flyway on first boot (see [How the database gets built](#how-the-database-gets-built) below) — no manual SQL needed for Docker.

Frontends: `http://localhost:5173` (user), `:5174` (admin), `:5175` (delivery agent). API gateway: `http://localhost:8080`.

### Database commands — Docker Postgres

The container publishes on host port **5433**, not 5432 — deliberately, so it doesn't collide with a native Postgres install (Homebrew, etc.) also bound to 5432 on the same machine. Inside the `readora` Docker network, every backend service still talks to it as `postgres:5432` regardless; 5433 only matters for `psql` commands you run yourself from the host.

**Do not run `dataset/setup.sql` against this container — it doesn't need it and it will fail.** `setup.sql` creates the `readora` role and database from scratch, but the container's entrypoint already did both on first boot from the `POSTGRES_USER`/`POSTGRES_DB`/`POSTGRES_PASSWORD` values in your `.env` — and critically, the container **only** creates that one role. There's no `postgres` superuser role and no role matching your OS username inside it, so connecting as either (as you would for native Postgres in Path B) always fails with `password authentication failed`, no matter the password. Connect as `readora` for everything:

```bash
# Seed demo data (after the stack has been up at least once, so Flyway has created the tables)
cd dataset
psql -h localhost -p 5433 -U readora -d readora -f seed.sql -W

# Reset — wipe all data and start over (Flyway recreates schema/tables on next `up`)
docker compose down -v   # -v also drops the postgres/kafka/redis volumes; omit -v to keep them
docker compose up --build -d
```

---

## Path B — hybrid local dev

### 1. Start infra

Either run just the infra containers from `docker-compose.yml`:

```bash
docker compose up postgres redis kafka -d
```

...or install Postgres natively (`brew install postgresql@18 pgvector` on macOS) and run Redis/Kafka via Docker (`docker compose up redis kafka -d`). Native Postgres is what the rest of this section assumes, since that's the common setup for iterating on migrations.

**Pick one Postgres, not both at once.** Every `psql`/connection command below targets **port 5432** on the assumption you're using native Postgres. If you use the containerized one instead (first option above), it publishes on **port 5433** — swap `-p 5432` for `-p 5433` in every command in this section, and set `POSTGRES_DB_URL` to `jdbc:postgresql://localhost:5433/readora` in step 6. If a native Postgres service is already running in the background (`brew services list`), `psql -p 5432` will silently talk to *that* one even if you meant to hit the container — check `lsof -nP -iTCP:5432 -sTCP:LISTEN` if a query returns data you didn't expect to be there.

### 2. Set up PostgreSQL

**This step is for a native Postgres install only — do it once per machine. Never run it against the Docker container** (see [Path A's database commands](#database-commands--docker-postgres)); the container already creates the `readora` role/database itself on first boot, and it has no `postgres` or OS-username role for `setup.sql` to connect as, so it'll just fail with `password authentication failed`.

`dataset/setup.sql` creates both the `readora` role and the `readora` database it owns, as a superuser role — the native-Postgres equivalent of what the official Postgres image's entrypoint does automatically from `POSTGRES_USER`/`POSTGRES_DB`.

```bash
psql -h localhost -p 5432 -U "$(whoami)" -d postgres -f dataset/setup.sql -W
```

### 3. How the database gets built

Nothing else to run here manually — this is just what happens automatically:

- Each service owns one Postgres **schema** (`auth`, `catalog`, `commerce`, `ai`, …) and creates it, plus its own tables, via its own Flyway migrations (`services/<name>/src/main/resources/db/migration/`) the first time that service starts.
- ai-service's first migration also runs `CREATE EXTENSION IF NOT EXISTS vector` — this is why `setup.sql` makes the `readora` role superuser (step 2): creating a Postgres *extension* always requires it, no matter who's issuing the command.
- Services are independent here — you don't need to start them in a particular order for schema creation to work.

### 4. Seed demo data

Run this **after** the relevant services — auth-service, user-service, catalog-service, and delivery-agent-service — have started at least once and created their tables — it's a plain data-only script, not Flyway-managed, so it fails on missing tables if run first.

`seed.sql` loads its data from JSON files via relative paths (`data/categories.json`, etc.), resolved against wherever the shell is sitting when `psql` starts — **not** against the script's own location. Run it from inside `dataset/`, not the repo root, or you'll get `cat: data/categories.json: No such file or directory` on the first load and every subsequent insert fails too. It's all one transaction, so nothing partial gets left behind if that happens — just `cd` in and rerun.

```bash
cd dataset
psql -h localhost -p 5432 -U readora -d readora -f seed.sql -W
```

### 5. Cleanup — wipe the database and start over

```bash
psql -h localhost -p 5432 -U "$(whoami)" -d postgres -f dataset/cleanup.sql -W
```

This drops **both** the `readora` database and the `readora` role — deliberately, not just the database. A role that outlives a cleanup can end up non-superuser (e.g. if it was ever created some other way), which silently brings back the `permission denied to create extension "vector"` error on ai-service. Dropping the role too means the next `setup.sql` run always creates a clean superuser role from scratch. To start over, just re-run step 2's `setup.sql`.

### 6. Environment variables

Backend services read these directly from the process environment — a `.env` file only matters to Docker Compose (Path A), **not** to a service you launch yourself from Maven or IntelliJ. Set them as real shell/IntelliJ environment variables for Path B: either `export` them in the shell you launch IntelliJ from (so it inherits them), or set them per run configuration under *Run → Edit Configurations → Environment variables*.

| Variable | Required | Local value |
|---|---|---|
| `POSTGRES_DB_URL` | yes | `jdbc:postgresql://localhost:5432/readora` |
| `POSTGRES_USER` | yes | `readora` |
| `POSTGRES_PASSWORD` | yes | `readora` (or whatever you set in step 2) |
| `JWT_SECRET` | yes | any Base64 string ≥32 bytes — e.g. `openssl rand -base64 32` |
| `GATEWAY_SECRET` | yes | any string — shared secret between api-gateway and every downstream service |
| `AZURE_STORAGE_CONNECTION_STRING` / `AZURE_STORAGE_CONTAINER_NAME` | only for catalog-service's virtual-edition download endpoint | — |
| `AZURE_OPENAI_*` (4 vars) | only for ai-service's AI features | — |
| `NEW_RELIC_LICENSE_KEY` | no | only if you want otel-collector actually exporting somewhere |

`MCP_SERVER_URL` defaults to `http://localhost:8087` and doesn't need setting locally.

### 7. Run the backend services

Open `services/` as a Maven project in IntelliJ (or `mvn -pl services -am compile` from the CLI) and run each service's `*Application` main class. `shared-core` is a dependency of every service — IntelliJ's multi-module reactor builds it automatically; from the CLI, `mvn install` it first if you're building modules individually.

Each service listens on its own port (8080–8089) and its own **management/actuator port** (9080–9089, one offset from the main port) — every service can run simultaneously without port collisions. There's no required start order for local dev, though commerce-service, api-gateway, and mcp-server do make live calls to other services once traffic actually flows, so for a full end-to-end smoke test start everything before testing a flow that spans services (e.g. checkout).

Health check once a service is up: `curl http://localhost:9080/actuator/health` (api-gateway; swap the port for others per the table above).

### 8. Run the frontends

```bash
cd frontend
npm install          # installs all 4 workspace packages (shared-ui + 3 apps) in one pass
npm run dev --workspace=frontend-user          # http://localhost:5173
npm run dev --workspace=frontend-admin         # http://localhost:5174
npm run dev --workspace=frontend-delivery-agent # http://localhost:5175
```

Each expects api-gateway at `http://localhost:8080` by default (`VITE_API_BASE_URL`).

---

## Troubleshooting

- **`permission denied to create extension "vector"`** — the `readora` role isn't a superuser (e.g. it was created some other way than `setup.sql`). Fix with `psql -d postgres -c "ALTER ROLE readora SUPERUSER;"`.
- **`psql` on port 5432 returns data you didn't seed, or seeds into a database Docker doesn't see** — you have a native Postgres running on the host (check `brew services list` / `lsof -nP -iTCP:5432 -sTCP:LISTEN`) and it's shadowing the containerized one. They can't both use 5432 on the host at once, so the Docker container publishes on **5433** instead (see [Path A](#path-a--everything-in-docker) / [Path B step 1](#1-start-infra)) — make sure `-p` in your `psql` command matches whichever Postgres you actually meant to hit.
- **`password authentication failed for user "postgres"` / `"<your-username>"` on port 5433** — you ran a Path B command (`setup.sql`, or anything connecting as `postgres` or `$(whoami)`) against the *Docker* Postgres container. That container only has the single `readora` role (see [Database commands — Docker Postgres](#database-commands--docker-postgres)) — connect as `-U readora` instead, and skip `setup.sql` entirely for Docker.
- **`Web server failed to start. Port 90XX was already in use`** — another instance of that same service is still running (check `lsof -nP -iTCP:90XX -sTCP:LISTEN`); each service's actuator port is unique, so this only happens from a genuine duplicate process, not from running multiple different services together.
- **Running `mvn test` locally fails with Mockito/ByteBuddy errors mentioning "Java 26 is not supported"** — that's your default JDK, not the project's. Point Maven at JDK 21 (`JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test`) or run tests from IntelliJ with the module's configured 21 SDK.
