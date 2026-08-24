#!/usr/bin/env bash
# Fetches the OpenTelemetry Java agent jar used by every service's -javaagent flag.
# The jar itself isn't committed (24MB binary) — run this once after cloning.
set -euo pipefail

VERSION="v2.31.1"
DEST="$(dirname "$0")/opentelemetry-javaagent.jar"

curl -sL -o "$DEST" "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/${VERSION}/opentelemetry-javaagent.jar"
echo "Downloaded OTel Java agent ${VERSION} to ${DEST}"
