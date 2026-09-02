#!/usr/bin/env bash
set -euo pipefail
ENVIRONMENT="${1:-}"
APP_DIR="${2:-}"
TAG="${3:-latest}"

if [[ "$ENVIRONMENT" != "dev" && "$ENVIRONMENT" != "prod" ]]; then
  echo "Usage: $0 dev|prod /path/to/ceylonads-api [tag]" >&2
  exit 2
fi
if [[ -z "$APP_DIR" || ! -d "$APP_DIR" ]]; then
  echo "Application directory does not exist: $APP_DIR" >&2
  exit 2
fi

IMAGE="ceylonads-${ENVIRONMENT}-app:${TAG}"

docker build --platform linux/amd64 -t "$IMAGE" "$APP_DIR"

echo "Built: $IMAGE"
