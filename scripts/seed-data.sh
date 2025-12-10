#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
ROOT_DIR=$(cd "$SCRIPT_DIR/.." && pwd)

MINIO_ENDPOINT=${MINIO_ENDPOINT:-http://localhost:9070}
MINIO_ACCESS_KEY=${MINIO_ACCESS_KEY:-minioadmin}
MINIO_SECRET_KEY=${MINIO_SECRET_KEY:-minioadmin}

TEMP_DIR=$(mktemp -d)
cleanup() { rm -rf "$TEMP_DIR"; }
trap cleanup EXIT

# Build sample data tree
mkdir -p "$TEMP_DIR/logs/app/2025/01/01" "$TEMP_DIR/logs/app/2025/01/02" "$TEMP_DIR/backups/db/2025/01/01" "$TEMP_DIR/backups/db/2025/02/01"
mkdir -p "$TEMP_DIR/backups/files" "$TEMP_DIR/root"
printf "app log line\n" > "$TEMP_DIR/logs/app/2025/01/01/app.log"
printf "debug entry\n" > "$TEMP_DIR/logs/app/2025/01/02/debug.json"
printf "csv,header\nrow1,data" > "$TEMP_DIR/logs/app/2025/01/02/trade_2025_01.csv"
printf "root level file" > "$TEMP_DIR/root/readme.txt"
printf "orphan file" > "$TEMP_DIR/orphan.bin"
head -c 10240 </dev/urandom > "$TEMP_DIR/backups/db/2025/01/01/dump.bin"
printf "{}" > "$TEMP_DIR/backups/files/config.json"

SEED_SCRIPT=$(cat <<SCRIPT
alias mc='mc --insecure'
mc alias set local $MINIO_ENDPOINT $MINIO_ACCESS_KEY $MINIO_SECRET_KEY >/dev/null
mc mb --ignore-existing local/logs
mc mb --ignore-existing local/backups
mc rm -r --force local/logs || true
mc rm -r --force local/backups || true
mc mirror /seed/logs local/logs
mc mirror /seed/backups local/backups
# root-level files
mc cp /seed/root/readme.txt local/logs/readme.txt
mc cp /seed/orphan.bin local/backups/orphan.bin
SCRIPT
)

docker run --rm --network host -v "$TEMP_DIR:/seed" --entrypoint /bin/sh minio/mc -c "$SEED_SCRIPT"

echo "Seeded sample data into MinIO. Buckets: logs, backups"
