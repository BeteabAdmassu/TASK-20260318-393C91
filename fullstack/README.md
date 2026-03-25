# MindFlow Authentication & Security Foundation

This deliverable contains a local-only full stack implementation under `fullstack/`:

- `backend/`: Spring Boot JWT auth, BCrypt password hashing, RBAC, and data desensitization.
- `frontend/`: Angular login screen and authenticated RBAC dashboard.
- `docker-compose.yml`: PostgreSQL + backend + frontend orchestration.

## Run with Docker Compose

```bash
cd fullstack
docker-compose up --build
```

Services:
- Frontend: `http://localhost`
- Backend API: `http://localhost:8080`
- PostgreSQL: `localhost:5432`

## Bootstrap Admin

- Username: from `BOOTSTRAP_ADMIN_USERNAME` (default `admin`)
- Password: must be provided via `BOOTSTRAP_ADMIN_PASSWORD` (no hardcoded default)

Configure via environment variables in `docker-compose.yml`:
- `BOOTSTRAP_ADMIN_USERNAME`
- `BOOTSTRAP_ADMIN_PASSWORD`
- `BOOTSTRAP_ADMIN_ENABLED`

## Security Notes

- JWT secret must be at least 256-bit base64 value (`JWT_SECRET`).
- Password policy minimum length is 8.
- Role restrictions:
  - Passenger endpoints: PASSENGER / DISPATCHER / ADMIN
  - Dispatcher endpoints: DISPATCHER / ADMIN
  - Admin endpoints: ADMIN only

## Sensitivity Rules

- `LOW`: no masking.
- `MEDIUM`: passenger receives partial masking.
- `HIGH`: passenger and dispatcher receive `[REDACTED]`; admin sees full value.

## Notification Preferences

- Settings page route: `http://localhost/settings`
- Supports:
  - Arrival reminders toggle
  - Reservation success notifications toggle
  - Reminder lead minutes (default `10`)
  - DND time range (`HH:mm` start/end)
- Preferences persist in PostgreSQL user profile and are applied by backend rule logic.

## Dispatcher Workflow Engine

- Dispatcher dashboard route: `http://localhost/dispatcher`
- Features:
  - Create operation tasks by workflow type and mode
  - Approve / reject / return actions
  - Batch approve selected tasks
  - Manual escalation evaluation for tasks unprocessed over 24 hours
- Visual stepper progress per task

## Unified Message Center

- Message center route: `http://localhost/messages`
- Includes:
  - Inbox by type filter (`Reservation Success`, `Arrival Reminder`, `Missed Check-In`)
  - Mark read/unread and delete actions
  - Polling refresh (15s) for near-real-time updates
  - Sensitive value masking for phone/ID-like strings
- Scheduler behavior:
  - Reservation success messages generated from booking events
  - Arrival reminders generated using preference lead time and DND policy
- Missed check-in generated 5 minutes after start time

## Data Integration & Cleaning Engine

- Admin APIs are available under `POST /api/admin/integration/import`.
- Supports ingestion templates:
  - `JSON`: list of objects
  - `HTML`: table rows with stop/address/apartment/area/price columns
- Cleaning output standards:
  - Area unit normalized to `㎡`
  - Price unit normalized to `yuan/month`
  - Missing fields mapped to `NULL` with source logging
- Persistence and traceability:
  - `import_jobs` for pipeline run status
  - `cleaned_records` for standardized output rows
  - `cleaning_audit_logs` for field-level transformation actions
- `stop_structure_versions` for field-level stop version history

## Administrator Control Panel

- Admin page route: `http://localhost/admin`
- Includes configuration pages for:
  - Notification message templates (CRUD)
  - Search ranking rule weights (relevance/frequency/popularity)
  - Standard field dictionaries (CRUD)
- Updates are persisted in PostgreSQL and applied immediately by backend services.

## Observability & Health Monitoring

- Observability route: `http://localhost/observability`
- Actuator endpoints exposed:
  - `http://localhost:8080/actuator/health`
  - `http://localhost:8080/actuator/metrics`
- Admin diagnostics APIs:
  - `GET /api/admin/observability/snapshot`
  - `GET /api/admin/observability/alerts`
- Alerting conditions:
  - Message queue backlog exceeds threshold (`app.monitoring.queue-backlog-threshold`)
  - API P95 exceeds threshold (`app.monitoring.api-p95-threshold-ms`, default 500ms)

### Local PostgreSQL Backup Strategy

- Backup: `fullstack/backup/backup_postgres.sh`
- Restore: `fullstack/backup/restore_postgres.sh`
- Example:
  - `bash fullstack/backup/backup_postgres.sh ./fullstack/backups`
  - `bash fullstack/backup/restore_postgres.sh ./fullstack/backups/<file>.sql`

## Test Determinism

- `fullstack/run_tests.sh` uses a deterministic test stack bootstrap.
- By default it resets compose resources (`down -v`) before API tests when the stack is not already healthy.
- Override with `TEST_RESET_STACK=false` to keep existing data during local test runs.
