#!/bin/bash
set -euo pipefail

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
OUTPUT_DIR=${1:-/backups}
RETENTION_DAYS=${RETENTION_DAYS:-14}

mkdir -p "$OUTPUT_DIR"

TARGET_FILE="$OUTPUT_DIR/mindflow_${TIMESTAMP}.sql"
docker exec mindflow-db pg_dump -U mindflow -d mindflow > "$TARGET_FILE"

find "$OUTPUT_DIR" -type f -name "mindflow_*.sql" -mtime +"$RETENTION_DAYS" -delete

echo "Backup written to $TARGET_FILE"
echo "Retention cleanup complete (days=$RETENTION_DAYS)"
