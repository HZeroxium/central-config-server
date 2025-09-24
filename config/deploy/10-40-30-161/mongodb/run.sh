#!/usr/bin/env sh
set -eu

DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
ENV_FILE="$DIR/env.example"

SERVICE_NAME=$(grep -E '^SERVICE_NAME=' "$ENV_FILE" | head -n1 | cut -d'=' -f2)
HOST_MONGODB_PORT=$(grep -E '^HOST_MONGODB_PORT=' "$ENV_FILE" | head -n1 | cut -d'=' -f2)
CONTAINER_MONGODB_PORT=$(grep -E '^CONTAINER_MONGODB_PORT=' "$ENV_FILE" | head -n1 | cut -d'=' -f2)
MONGO_DATA_DIR=$(grep -E '^MONGO_DATA_DIR=' "$ENV_FILE" | head -n1 | cut -d'=' -f2)

mkdir -p "$MONGO_DATA_DIR"
docker rm -f "$SERVICE_NAME" >/dev/null 2>&1 || true

docker run -d \
  --name "$SERVICE_NAME" \
  --restart unless-stopped \
  --env-file "$ENV_FILE" \
  -p "$HOST_MONGODB_PORT:$CONTAINER_MONGODB_PORT" \
  -v "$MONGO_DATA_DIR:/data/db" \
  mongo:8.0

echo "Started $SERVICE_NAME on :$HOST_MONGODB_PORT (data: $MONGO_DATA_DIR)"


