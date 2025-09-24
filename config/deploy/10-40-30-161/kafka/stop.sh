#!/usr/bin/env sh
set -eu

DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
ENV_FILE="$DIR/env.example"
SERVICE_NAME=$(grep -E '^SERVICE_NAME=' "$ENV_FILE" | head -n1 | cut -d'=' -f2)

docker rm -f "$SERVICE_NAME" >/dev/null 2>&1 || true
echo "Stopped $SERVICE_NAME"


