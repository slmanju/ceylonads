#!/usr/bin/env bash
set -euo pipefail
ENVIRONMENT="${1:-}"
APP_DIR="${2:-}"
TAG="${3:-latest}"

if [[ -z "$ENVIRONMENT" || -z "$APP_DIR" ]]; then
  echo "Usage: $0 dev|prod /path/to/ceylonads [tag]" >&2
  exit 2
fi

"$(dirname "$0")/deploy-foundation.sh" "$ENVIRONMENT"
"$(dirname "$0")/push-image.sh" "$ENVIRONMENT" "$APP_DIR" "$TAG"
"$(dirname "$0")/deploy-app.sh" "$ENVIRONMENT"
