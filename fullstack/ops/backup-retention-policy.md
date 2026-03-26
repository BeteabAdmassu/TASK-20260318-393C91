# Local Backup Retention Policy

This project uses PostgreSQL local backups under `fullstack/backup/`.

## Policy

- Backup frequency: daily (recommended via cron/task scheduler).
- Retention window: 14 days by default.
- Backup naming: `mindflow_YYYYMMDD_HHMMSS.sql`.
- Storage path: operator-defined local path (for example `./fullstack/backups`).

## Enforced Script

Use:

```bash
bash fullstack/backup/backup_postgres_with_retention.sh ./fullstack/backups
```

Retention behavior is enforced inside script:

- Controlled by `RETENTION_DAYS` environment variable.
- Old `mindflow_*.sql` files exceeding retention are deleted automatically.

## Example Scheduled Job

Linux cron example (daily at 02:30):

```bash
30 2 * * * cd /path/to/repo && RETENTION_DAYS=14 bash fullstack/backup/backup_postgres_with_retention.sh ./fullstack/backups >> ./fullstack/backups/backup.log 2>&1
```
