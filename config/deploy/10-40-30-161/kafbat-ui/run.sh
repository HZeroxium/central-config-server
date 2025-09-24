#!/usr/bin/env sh
set -eu

DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
ENV_FILE="$DIR/env.example"

SERVICE_NAME=$(grep -E '^SERVICE_NAME=' "$ENV_FILE" | head -n1 | cut -d'=' -f2)
HOST_KAFBAT_PORT=$(grep -E '^HOST_KAFBAT_PORT=' "$ENV_FILE" | head -n1 | cut -d'=' -f2)
CONTAINER_KAFBAT_PORT=$(grep -E '^CONTAINER_KAFBAT_PORT=' "$ENV_FILE" | head -n1 | cut -d'=' -f2)

docker rm -f "$SERVICE_NAME" >/dev/null 2>&1 || true

docker run -d \
  --name "$SERVICE_NAME" \
  --restart unless-stopped \
  --env-file "$ENV_FILE" \
  -p "$HOST_KAFBAT_PORT:$CONTAINER_KAFBAT_PORT" \
  ghcr.io/kafbat/kafka-ui:main

echo "Started $SERVICE_NAME on :$HOST_KAFBAT_PORT"


