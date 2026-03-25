#!/bin/bash
set -euo pipefail

if [ $# -lt 1 ]; then
  echo "Usage: ./restore_postgres.sh /path/to/backup.sql"
  exit 1
fi

BACKUP_FILE=$1
cat "$BACKUP_FILE" | docker exec -i mindflow-db psql -U mindflow -d mindflow
echo "Restore completed from $BACKUP_FILE"
