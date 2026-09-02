#!/usr/bin/env bash
set -euo pipefail
ENVIRONMENT="${1:-}"
APP_DIR="${2:-}"
TAG="${3:-latest}"
REGION="${AWS_REGION:-ap-south-1}"

if [[ "$ENVIRONMENT" != "dev" && "$ENVIRONMENT" != "prod" ]]; then
  echo "Usage: $0 dev|prod /path/to/ceylonads [tag]" >&2
  exit 2
fi
if [[ -z "$APP_DIR" || ! -d "$APP_DIR" ]]; then
  echo "Application directory does not exist: $APP_DIR" >&2
  exit 2
fi

ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
REPOSITORY="ceylonads-${ENVIRONMENT}-app"
REGISTRY="${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com"
IMAGE="${REGISTRY}/${REPOSITORY}:${TAG}"

aws ecr get-login-password --region "$REGION" | \
  docker login --username AWS --password-stdin "$REGISTRY"

docker build --platform linux/amd64 -t "$IMAGE" "$APP_DIR"
docker push "$IMAGE"

echo "Pushed: $IMAGE"
