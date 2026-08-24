#!/usr/bin/env bash
# Stops every service started by run-all.sh, killing each one's whole process group
# (mvn plus the JVM it forks), not just the mvn wrapper.
set -uo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
PID_DIR="$ROOT_DIR/pids"

if [ ! -d "$PID_DIR" ] || [ -z "$(ls -A "$PID_DIR" 2>/dev/null)" ]; then
  echo "No running services found (nothing in pids/)."
  exit 0
fi

for pid_file in "$PID_DIR"/*.pid; do
  svc="$(basename "$pid_file" .pid)"
  pid="$(cat "$pid_file")"

  if ! kill -0 "$pid" 2>/dev/null; then
    echo "  $svc — already stopped"
    rm -f "$pid_file"
    continue
  fi

  pgid="$(ps -o pgid= -p "$pid" | tr -d ' ')"
  if [ -n "$pgid" ]; then
    kill -TERM -- "-$pgid" 2>/dev/null || kill -TERM "$pid" 2>/dev/null
  else
    kill -TERM "$pid" 2>/dev/null
  fi

  echo "  $svc — stopped (pid $pid)"
  rm -f "$pid_file"
done

echo "Done."
