#!/usr/bin/env sh
set -eu

DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
ENV_FILE="$DIR/env.example"

SERVICE_NAME=$(grep -E '^SERVICE_NAME=' "$ENV_FILE" | head -n1 | cut -d'=' -f2)
HOST_KAFKA_CLIENT_PORT=$(grep -E '^HOST_KAFKA_CLIENT_PORT=' "$ENV_FILE" | head -n1 | cut -d'=' -f2)
CONTAINER_KAFKA_CLIENT_PORT=$(grep -E '^CONTAINER_KAFKA_CLIENT_PORT=' "$ENV_FILE" | head -n1 | cut -d'=' -f2)
KAFKA_DATA_DIR=$(grep -E '^KAFKA_DATA_DIR=' "$ENV_FILE" | head -n1 | cut -d'=' -f2)

mkdir -p "$KAFKA_DATA_DIR"
docker rm -f "$SERVICE_NAME" >/dev/null 2>&1 || true

docker run -d \
  --name "$SERVICE_NAME" \
  --restart unless-stopped \
  --env-file "$ENV_FILE" \
  -p "$HOST_KAFKA_CLIENT_PORT:$CONTAINER_KAFKA_CLIENT_PORT" \
  -v "$KAFKA_DATA_DIR:/bitnami/kafka" \
  bitnami/kafka:3.9

echo "Started $SERVICE_NAME on :$HOST_KAFKA_CLIENT_PORT (data: $KAFKA_DATA_DIR)"


