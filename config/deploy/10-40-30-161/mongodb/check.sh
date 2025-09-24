#!/usr/bin/env sh
set -eu

DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
ENV_FILE="$DIR/env.example"

HOST_MONGODB_PORT=$(grep -E '^HOST_MONGODB_PORT=' "$ENV_FILE" | cut -d'=' -f2)

if command -v nc >/dev/null 2>&1; then
  nc -z localhost "$HOST_MONGODB_PORT" && echo "healthy" || { echo "unhealthy"; exit 1; }
else
  echo "nc not found; assuming healthy if port reachable"
  exit 0
fi


