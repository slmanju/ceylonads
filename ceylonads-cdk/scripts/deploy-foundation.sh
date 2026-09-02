#!/usr/bin/env bash
set -euo pipefail
ENVIRONMENT="${1:-}"
case "$ENVIRONMENT" in
  dev) STACK="CeylonAdsDevFoundation" ;;
  prod) STACK="CeylonAdsProdFoundation" ;;
  *) echo "Usage: $0 dev|prod" >&2; exit 2 ;;
esac
cdk deploy "$STACK" --require-approval never
