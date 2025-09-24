#!/usr/bin/env sh
set -eu

DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
ENV_FILE="$DIR/env.example"

SERVICE_NAME=$(grep -E '^SERVICE_NAME=' "$ENV_FILE" | head -n1 | cut -d'=' -f2)
HOST_HTTP_PORT=$(grep -E '^HOST_HTTP_PORT=' "$ENV_FILE" | head -n1 | cut -d'=' -f2)
CONTAINER_HTTP_PORT=$(grep -E '^CONTAINER_HTTP_PORT=' "$ENV_FILE" | head -n1 | cut -d'=' -f2)
HOST_THRIFT_PORT=$(grep -E '^HOST_THRIFT_PORT=' "$ENV_FILE" | head -n1 | cut -d'=' -f2)
CONTAINER_THRIFT_PORT=$(grep -E '^CONTAINER_THRIFT_PORT=' "$ENV_FILE" | head -n1 | cut -d'=' -f2)
LOG_LABEL_SERVICE=$(grep -E '^LOG_LABEL_SERVICE=' "$ENV_FILE" | head -n1 | cut -d'=' -f2)
LOG_LABEL_ENVIRONMENT=$(grep -E '^LOG_LABEL_ENVIRONMENT=' "$ENV_FILE" | head -n1 | cut -d'=' -f2)

docker rm -f "$SERVICE_NAME" >/dev/null 2>&1 || true

docker run -d \
  --name "$SERVICE_NAME" \
  --restart unless-stopped \
  --env-file "$ENV_FILE" \
  -p "$HOST_HTTP_PORT:$CONTAINER_HTTP_PORT" \
  -p "$HOST_THRIFT_PORT:$CONTAINER_THRIFT_PORT" \
  -v "$DIR/logs:/app/logs" \
  --label service="$LOG_LABEL_SERVICE" \
  --label environment="$LOG_LABEL_ENVIRONMENT" \
  hzeroxium/user-thrift-server-service:latest

echo "Started $SERVICE_NAME on HTTP :$HOST_HTTP_PORT and Thrift :$HOST_THRIFT_PORT"


