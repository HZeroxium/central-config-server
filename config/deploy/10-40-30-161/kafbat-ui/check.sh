#!/usr/bin/env sh
set -eu

DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
ENV_FILE="$DIR/env.example"

HOST_KAFBAT_PORT=$(grep -E '^HOST_KAFBAT_PORT=' "$ENV_FILE" | cut -d'=' -f2)

curl -fsS "http://localhost:$HOST_KAFBAT_PORT/" >/dev/null && echo "healthy" || { echo "unhealthy"; exit 1; }


