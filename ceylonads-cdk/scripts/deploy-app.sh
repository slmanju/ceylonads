#!/usr/bin/env bash
set -euo pipefail
ENVIRONMENT="${1:-}"
case "$ENVIRONMENT" in
  dev) STACK="CeylonAdsDevApp" ;;
  prod) STACK="CeylonAdsProdApp" ;;
  *) echo "Usage: $0 dev|prod" >&2; exit 2 ;;
esac
cdk deploy "$STACK" --require-approval never
