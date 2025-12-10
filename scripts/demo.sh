#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
ROOT_DIR=$(cd "$SCRIPT_DIR/.." && pwd)

export COMPOSE_PROJECT_NAME=s3webapp

echo "Building frontend..."
cd "$ROOT_DIR/frontend"
npm install
npm run build:prod

cd "$ROOT_DIR"

# Sync built UI into backend static resources
STATIC_DIR="$ROOT_DIR/backend/src/main/resources/static"
rm -rf "$STATIC_DIR"/* || true
mkdir -p "$STATIC_DIR"
cp -r "$ROOT_DIR/frontend/dist/frontend/browser/"* "$STATIC_DIR"/

echo "Building backend jar..."
mvn -f backend/pom.xml clean package -DskipTests

echo "Starting stack via docker compose..."
"$SCRIPT_DIR/start-infra.sh"

# wait for backend to respond
until curl -sSf http://localhost:9080/actuator/health >/dev/null 2>&1; do
  echo "Waiting for backend to be healthy..."
  sleep 2
done

"$SCRIPT_DIR/seed-data.sh"

echo "Demo ready at http://localhost:9080"
