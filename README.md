# Readora

A quick-commerce bookstore built as 10 independent Spring Boot microservices behind an API
gateway, plus three React frontends (customer-facing, admin, and delivery agent).

## Ports

| Service              | Port |
|-----------------------|------|
| api-gateway            | 8080 |
| auth-service            | 8081 |
| user-service             | 8082 |
| catalog-service           | 8083 |
| commerce-service            | 8084 |
| payment-service               | 8085 |
| notification-service            | 8086 |
| mcp-server                        | 8087 |
| ai-service                          | 8088 |
| delivery-agent-service                | 8089 |
| frontend-user (Vite dev server)       | 5173 |
| frontend-admin (Vite dev server)        | 5174 |
| frontend-delivery-agent (Vite dev server) | 5175 |

## Prerequisites

Postgres, Redis, and Kafka must be running before any backend service will start. Redis and
Kafka are containerized:

```bash
cd infra
docker compose up -d
```

Postgres runs natively (not in docker-compose) — see `dataset/seed.sql` for setup/reseed
instructions. Each service also expects a `.env` file in its own directory (`services/<name>/.env`)
with its DB credentials and secrets — see `services/*/.env` for the expected keys.

---

## Running the backend services

Each service is run independently with Maven from its own directory. All commands below assume
you're starting from the repo root.

### Without the OTel agent (default — fast, no tracing overhead)

```bash
cd services/auth-service && mvn -o spring-boot:run
```
```bash
cd services/user-service && mvn -o spring-boot:run
```
```bash
cd services/catalog-service && mvn -o spring-boot:run
```
```bash
cd services/commerce-service && mvn -o spring-boot:run
```
```bash
cd services/payment-service && mvn -o spring-boot:run
```
```bash
cd services/notification-service && mvn -o spring-boot:run
```
```bash
cd services/mcp-server && mvn -o spring-boot:run
```
```bash
cd services/ai-service && mvn -o spring-boot:run
```
```bash
cd services/delivery-agent-service && mvn -o spring-boot:run
```
```bash
cd services/api-gateway && mvn -o spring-boot:run
```

Start `api-gateway` last — it's the one every frontend request goes through, but the individual
services don't depend on it being up to start themselves.

### With the OTel agent (opt-in tracing/metrics/logs)

One-time setup — the agent jar is a 24MB binary and isn't committed:

```bash
infra/otel/download-agent.sh
```

This also requires the `otel-collector` container from `infra/docker-compose.yml` to be running
(it's included in the `docker compose up -d` from Prerequisites) so there's somewhere for the
agent to actually send data.

**Use `-Dspring-boot.run.agents=`, not `-Dspring-boot.run.jvmArguments=-javaagent:...`.** This
repo's path contains spaces ("Best Projects"), and `jvmArguments` splits on whitespace and mangles
the path — you'll get `Error opening zip file or JAR manifest missing`. The Spring Boot Maven
plugin's dedicated `agents` parameter handles this correctly.

Each service needs its own `OTEL_SERVICE_NAME` so traces are attributed correctly. Substitute
`$REPO` for the absolute path to this repo, or just use the literal path:

```bash
AGENT="$REPO/infra/otel/opentelemetry-javaagent.jar"

cd services/auth-service && \
  OTEL_SERVICE_NAME=auth-service \
  OTEL_SDK_DISABLED=false \
  OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317 \
  OTEL_EXPORTER_OTLP_PROTOCOL=grpc \
  OTEL_LOGS_EXPORTER=otlp \
  mvn -o spring-boot:run -Dspring-boot.run.agents="$AGENT"
```

Repeat per service, changing only the directory and `OTEL_SERVICE_NAME`:

```bash
cd services/user-service && OTEL_SERVICE_NAME=user-service OTEL_SDK_DISABLED=false OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317 OTEL_EXPORTER_OTLP_PROTOCOL=grpc OTEL_LOGS_EXPORTER=otlp mvn -o spring-boot:run -Dspring-boot.run.agents="$AGENT"
```
```bash
cd services/catalog-service && OTEL_SERVICE_NAME=catalog-service OTEL_SDK_DISABLED=false OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317 OTEL_EXPORTER_OTLP_PROTOCOL=grpc OTEL_LOGS_EXPORTER=otlp mvn -o spring-boot:run -Dspring-boot.run.agents="$AGENT"
```
```bash
cd services/commerce-service && OTEL_SERVICE_NAME=commerce-service OTEL_SDK_DISABLED=false OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317 OTEL_EXPORTER_OTLP_PROTOCOL=grpc OTEL_LOGS_EXPORTER=otlp mvn -o spring-boot:run -Dspring-boot.run.agents="$AGENT"
```
```bash
cd services/payment-service && OTEL_SERVICE_NAME=payment-service OTEL_SDK_DISABLED=false OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317 OTEL_EXPORTER_OTLP_PROTOCOL=grpc OTEL_LOGS_EXPORTER=otlp mvn -o spring-boot:run -Dspring-boot.run.agents="$AGENT"
```
```bash
cd services/notification-service && OTEL_SERVICE_NAME=notification-service OTEL_SDK_DISABLED=false OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317 OTEL_EXPORTER_OTLP_PROTOCOL=grpc OTEL_LOGS_EXPORTER=otlp mvn -o spring-boot:run -Dspring-boot.run.agents="$AGENT"
```
```bash
cd services/mcp-server && OTEL_SERVICE_NAME=mcp-server OTEL_SDK_DISABLED=false OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317 OTEL_EXPORTER_OTLP_PROTOCOL=grpc OTEL_LOGS_EXPORTER=otlp mvn -o spring-boot:run -Dspring-boot.run.agents="$AGENT"
```
```bash
cd services/ai-service && OTEL_SERVICE_NAME=ai-service OTEL_SDK_DISABLED=false OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317 OTEL_EXPORTER_OTLP_PROTOCOL=grpc OTEL_LOGS_EXPORTER=otlp mvn -o spring-boot:run -Dspring-boot.run.agents="$AGENT"
```
```bash
cd services/delivery-agent-service && OTEL_SERVICE_NAME=delivery-agent-service OTEL_SDK_DISABLED=false OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317 OTEL_EXPORTER_OTLP_PROTOCOL=grpc OTEL_LOGS_EXPORTER=otlp mvn -o spring-boot:run -Dspring-boot.run.agents="$AGENT"
```
```bash
cd services/api-gateway && OTEL_SERVICE_NAME=api-gateway OTEL_SDK_DISABLED=false OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317 OTEL_EXPORTER_OTLP_PROTOCOL=grpc OTEL_LOGS_EXPORTER=otlp mvn -o spring-boot:run -Dspring-boot.run.agents="$AGENT"
```

`OTEL_SDK_DISABLED=false` is what actually turns tracing on — the agent attaches either way, but
does nothing unless this is set. Drop it (or set it to `true`) to attach the agent without it
doing anything, which is rarely useful — just omit `-Dspring-boot.run.agents` entirely instead to
skip the agent altogether.

Verified working: `mvn -o spring-boot:run -Dspring-boot.run.agents="$AGENT"` produces
`[INFO] Attaching agents: [...]` followed by the agent's own
`opentelemetry-javaagent - version: 2.31.1` banner on startup — that's the confirmation it's
actually attached.

---

## Running the frontends

```bash
cd frontend/frontend-user && npm run dev
```
```bash
cd frontend/frontend-admin && npm run dev
```
```bash
cd frontend/frontend-delivery-agent && npm run dev
```

All three are plain Vite dev servers — no OTel agent applies to them (that's a JVM-only mechanism).

---

## Stopping everything

Each `mvn spring-boot:run` and `npm run dev` is a foreground process — `Ctrl+C` in its terminal,
or find and kill by port:

```bash
lsof -ti tcp:8080 | xargs kill
```

Repeat per port from the table above.

## Gotchas

- **Rate-limit config changes need a Redis flush, not just a restart.** The gateway's rate
  limiter (Bucket4j, Redis-backed) bakes each caller's limit into their bucket the first time it's
  created — changing `app.rate-limit` in `application.yml` and restarting the gateway does nothing
  for callers who already have a bucket. Clear it with:
  ```bash
  docker exec readora-redis redis-cli --scan --pattern "ratelimit:*" | xargs -I{} docker exec readora-redis redis-cli DEL {}
  ```
- **A stale service silently serves old behavior.** If something you just changed doesn't seem to
  take effect, check whether the service on that port actually restarted — `lsof -ti tcp:<port>`
  to find the PID, `ps -p <pid> -o lstart=` to see how long it's been running.
