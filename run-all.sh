#!/usr/bin/env bash
# Starts every backend service in parallel, each logging to logs/<service>.log.
#
# OTel is off by default (OTEL_SDK_DISABLED=true). To capture telemetry for a run:
#   OTEL_SDK_DISABLED=false ./run-all.sh
set -m
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Spring Boot's jvmArguments passthrough re-splits on whitespace, which breaks if the repo
# path itself contains a space (it does here). Symlink the agent to a space-free path instead
# of relying on quoting behavior inside that re-split.
AGENT_JAR="/tmp/readora-otel-javaagent.jar"
ln -sf "$ROOT_DIR/infra/otel/opentelemetry-javaagent.jar" "$AGENT_JAR"

SERVICES=(
  "auth-service:8081"
  "api-gateway:8080"
  "user-service:8082"
  "catalog-service:8083"
  "commerce-service:8084"
  "payment-service:8085"
  "notification-service:8086"
  "ai-service:8088"
  "mcp-server:8087"
)

mkdir -p "$ROOT_DIR/logs" "$ROOT_DIR/pids"
rm -f "$ROOT_DIR"/pids/*.pid

export OTEL_SDK_DISABLED="${OTEL_SDK_DISABLED:-true}"
export OTEL_EXPORTER_OTLP_ENDPOINT="${OTEL_EXPORTER_OTLP_ENDPOINT:-http://localhost:4317}"
export OTEL_EXPORTER_OTLP_PROTOCOL="${OTEL_EXPORTER_OTLP_PROTOCOL:-grpc}"
export OTEL_LOGS_EXPORTER="${OTEL_LOGS_EXPORTER:-otlp}"

echo "Starting ${#SERVICES[@]} services (OTEL_SDK_DISABLED=$OTEL_SDK_DISABLED)..."
echo

for entry in "${SERVICES[@]}"; do
  svc="${entry%%:*}"
  port="${entry##*:}"

  (
    cd "$ROOT_DIR/services" && \
    OTEL_SERVICE_NAME="$svc" \
    mvn -q -pl "$svc" \
    -Dspring-boot.run.jvmArguments="-javaagent:$AGENT_JAR" \
    spring-boot:run
  ) > "$ROOT_DIR/logs/$svc.log" 2>&1 &

  echo $! > "$ROOT_DIR/pids/$svc.pid"
  printf "  %-22s port %s  (pid %s, log logs/%s)\n" "$svc" "$port" "$!" "$svc.log"
done

echo
echo "All services launching. Tail a log with: tail -f logs/<service>.log"
echo "Stop everything with: ./stop-all.sh"
