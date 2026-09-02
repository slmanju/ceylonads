#!/usr/bin/env bash
set -euo pipefail
# Destroy the application first because it references foundation resources.
cdk destroy CeylonAdsDevApp --force
cdk destroy CeylonAdsDevFoundation --force
