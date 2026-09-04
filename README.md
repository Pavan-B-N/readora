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

```bash
cp .env.example .env
# fill in .env — see "Environment variables" below for what's required vs optional
docker compose up --build
```

That's it — Postgres, Redis, Kafka, the OpenTelemetry collector, all 10 backend services, and all 3 frontends start as containers, wired together on the `readora` Docker network. Postgres starts **empty**; each service creates its own schema/tables via Flyway on first boot (see [How the database gets built](#how-the-database-gets-built) below) — no manual SQL needed for Docker.

Frontends: `http://localhost:5173` (user), `:5174` (admin), `:5175` (delivery agent). API gateway: `http://localhost:8080`.

To load demo data on top of the empty schema, run the [seed step](#4-seed-demo-data) below against the containerized Postgres (`-h localhost -p 5432`, same as native — the container publishes 5432).

---

## Path B — hybrid local dev

### 1. Start infra

Either run just the infra containers from `docker-compose.yml`:

```bash
docker compose up postgres redis kafka -d
```

...or install Postgres natively (`brew install postgresql@18 pgvector` on macOS) and run Redis/Kafka via Docker (`docker compose up redis kafka -d`). Native Postgres is what the rest of this section assumes, since that's the common setup for iterating on migrations.

### 2. Set up PostgreSQL

**This is the one step nothing in Docker automates for you — do it once per machine.**

`dataset/setup.sql` creates both the `readora` role and the `readora` database it owns, as a superuser role. (In Docker, this is invisible because the official Postgres image auto-creates a role matching `POSTGRES_USER` as a superuser on first container boot — `setup.sql` is the native-Postgres equivalent of that.)

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
- **`Web server failed to start. Port 90XX was already in use`** — another instance of that same service is still running (check `lsof -nP -iTCP:90XX -sTCP:LISTEN`); each service's actuator port is unique, so this only happens from a genuine duplicate process, not from running multiple different services together.
- **Running `mvn test` locally fails with Mockito/ByteBuddy errors mentioning "Java 26 is not supported"** — that's your default JDK, not the project's. Point Maven at JDK 21 (`JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test`) or run tests from IntelliJ with the module's configured 21 SDK.
