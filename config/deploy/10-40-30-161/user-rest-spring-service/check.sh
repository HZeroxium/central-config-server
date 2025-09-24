#!/usr/bin/env sh
set -eu

DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
ENV_FILE="$DIR/env.example"

HOST_HTTP_PORT=$(grep -E '^HOST_HTTP_PORT=' "$ENV_FILE" | cut -d'=' -f2)
curl -fsS "http://localhost:$HOST_HTTP_PORT/actuator/health" | grep -qi '"status"\s*:\s*"UP"' && echo "healthy" || { echo "unhealthy"; exit 1; }


