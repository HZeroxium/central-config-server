#!/usr/bin/env sh
set -eu

DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
ENV_FILE="$DIR/env.example"

HOST_MONGO_EXPRESS_PORT=$(grep -E '^HOST_MONGO_EXPRESS_PORT=' "$ENV_FILE" | cut -d'=' -f2)

curl -fsS "http://localhost:$HOST_MONGO_EXPRESS_PORT/" >/dev/null && echo "healthy" || { echo "unhealthy"; exit 1; }


