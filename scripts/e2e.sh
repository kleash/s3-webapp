#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
ROOT_DIR=$(cd "$SCRIPT_DIR/.." && pwd)
export COMPOSE_PROJECT_NAME=s3webapp

cleanup() {
  cd "$ROOT_DIR"
  docker compose down
}
trap cleanup EXIT

cd "$ROOT_DIR"

echo "Building and starting stack for e2e..."
docker compose up -d --build

echo "Waiting for backend health..."
until curl -sSf http://localhost:9080/actuator/health >/dev/null 2>&1; do
  sleep 2
done

"$SCRIPT_DIR/seed-data.sh"

cd "$ROOT_DIR/e2e"
npm install
npm run install:browsers
npm test

echo "E2E tests finished"
