#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)

echo "Stopping application stack..."
"$SCRIPT_DIR/stop-infra.sh"
