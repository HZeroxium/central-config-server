#!/usr/bin/env sh
set -eu

DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
ENV_FILE="$DIR/env.example"

SERVICE_NAME=$(grep -E '^SERVICE_NAME=' "$ENV_FILE" | head -n1 | cut -d'=' -f2)
HOST_MONGO_EXPRESS_PORT=$(grep -E '^HOST_MONGO_EXPRESS_PORT=' "$ENV_FILE" | head -n1 | cut -d'=' -f2)
CONTAINER_MONGO_EXPRESS_PORT=$(grep -E '^CONTAINER_MONGO_EXPRESS_PORT=' "$ENV_FILE" | head -n1 | cut -d'=' -f2)

docker rm -f "$SERVICE_NAME" >/dev/null 2>&1 || true

docker run -d \
  --name "$SERVICE_NAME" \
  --restart unless-stopped \
  --env-file "$ENV_FILE" \
  -p "$HOST_MONGO_EXPRESS_PORT:$CONTAINER_MONGO_EXPRESS_PORT" \
  mongo-express:latest

echo "Started $SERVICE_NAME on :$HOST_MONGO_EXPRESS_PORT"


