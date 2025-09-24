#!/usr/bin/env sh
set -eu

DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
ENV_FILE="$DIR/env.example"

SERVICE_NAME=$(grep -E '^SERVICE_NAME=' "$ENV_FILE" | head -n1 | cut -d'=' -f2)
HOST_REDIS_PORT=$(grep -E '^HOST_REDIS_PORT=' "$ENV_FILE" | head -n1 | cut -d'=' -f2)
CONTAINER_REDIS_PORT=$(grep -E '^CONTAINER_REDIS_PORT=' "$ENV_FILE" | head -n1 | cut -d'=' -f2)
REDIS_DATA_DIR=$(grep -E '^REDIS_DATA_DIR=' "$ENV_FILE" | head -n1 | cut -d'=' -f2)

mkdir -p "$REDIS_DATA_DIR"
docker rm -f "$SERVICE_NAME" >/dev/null 2>&1 || true

CMD="redis-server --port $CONTAINER_REDIS_PORT"
if grep -q '^REDIS_PASSWORD=' "$ENV_FILE"; then
  PASS=$(grep -E '^REDIS_PASSWORD=' "$ENV_FILE" | head -n1 | cut -d'=' -f2)
  if [ -n "$PASS" ]; then
    CMD="$CMD --requirepass $PASS"
  fi
fi

docker run -d \
  --name "$SERVICE_NAME" \
  --restart unless-stopped \
  --env-file "$ENV_FILE" \
  -p "$HOST_REDIS_PORT:$CONTAINER_REDIS_PORT" \
  -v "$REDIS_DATA_DIR:/data" \
  redis:latest sh -c "$CMD"

echo "Started $SERVICE_NAME on :$HOST_REDIS_PORT (data: $REDIS_DATA_DIR)"


