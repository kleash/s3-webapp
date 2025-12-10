#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
ROOT_DIR=$(cd "$SCRIPT_DIR/.." && pwd)

export COMPOSE_PROJECT_NAME=s3webapp
cd "$ROOT_DIR"

echo "Starting infrastructure (MinIO + backend)..."
docker compose up -d --build minio backend

echo "Infra started. MinIO at http://localhost:9070, console at http://localhost:9072"
