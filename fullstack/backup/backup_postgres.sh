#!/bin/bash
set -euo pipefail

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
OUTPUT_DIR=${1:-/backups}
mkdir -p "$OUTPUT_DIR"

docker exec mindflow-db pg_dump -U mindflow -d mindflow > "$OUTPUT_DIR/mindflow_${TIMESTAMP}.sql"
echo "Backup written to $OUTPUT_DIR/mindflow_${TIMESTAMP}.sql"
