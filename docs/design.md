# Design Document

## Architecture
- `fullstack/backend`: Spring Boot 3.3 JWT authentication service with PostgreSQL persistence.
- `fullstack/frontend`: Angular 17 login UI and RBAC probe dashboard served by Nginx.
- `fullstack/docker-compose.yml`: orchestrates PostgreSQL, backend, and frontend.

## Security Foundation
- Authentication uses `POST /api/auth/login` with username and password.
- Passwords are salted and hashed with BCrypt (`password_hash` field).
- Password policy requires minimum 8 characters for user creation and bootstrap admin validation.
- Authorization uses JWT bearer tokens (`Authorization: Bearer <token>`).
- RBAC roles: `PASSENGER`, `DISPATCHER`, `ADMIN`.

## RBAC Rules
- `/api/passenger/**` -> PASSENGER, DISPATCHER, ADMIN
- `/api/dispatcher/**` -> DISPATCHER, ADMIN
- `/api/admin/**` -> ADMIN only

## Data Privacy
- Sensitivity levels: `LOW`, `MEDIUM`, `HIGH`.
- `LOW`: no masking.
- `MEDIUM`: partial masking for passengers.
- `HIGH`: full redaction for passenger and dispatcher, visible to admin.

## Smart Search System
- Endpoint: `GET /api/passenger/search?q=<query>` (JWT protected).
- Supported query forms:
  - Route number matching (`1A`, `BJ7`).
  - Stop name and keyword matching.
  - Pinyin/initial-letter matching (`bj` for Beijing).
- Real-time suggestion output includes top-ranked route-stop labels.
- Result ordering rule: `Relevance` then `Frequency Priority` then `Stop Popularity`.
- Deduplication removes duplicate stop names while preserving highest-ranked candidate.

## Notification Preferences & DND
- Passenger profile stores notification settings in PostgreSQL user row.
- Preference controls:
  - Arrival reminder toggle.
  - Reservation success toggle.
  - Arrival lead time in minutes (default `10`).
- DND supports user-defined time windows, including cross-midnight ranges (e.g., `22:00-07:00`).
- Notification allow/suppress evaluation rule:
  - First check toggle for notification type.
  - Then suppress if current time falls inside DND window.
- API endpoints:
  - `GET /api/passenger/preferences`
  - `PUT /api/passenger/preferences`

## Dispatcher Workflow & Approval Engine
- Domain supports workflow types:
  - `ROUTE_DATA_CHANGE`
  - `REMINDER_CONFIGURATION`
  - `ABNORMAL_DATA_REVIEW`
- State machine statuses:
  - `SUBMITTED` -> `IN_REVIEW` -> `APPROVED`
  - `REJECTED`
  - `RETURNED` (return to submitter)
- Workflow modes:
  - `CONDITIONAL`: serial gate progression.
  - `JOINT`: multiple approvals required before completion.
  - `PARALLEL`: concurrent approvals counted toward threshold.
- Escalation logic:
  - For `SUBMITTED` or `IN_REVIEW` tasks idle > 24 hours, timeout warning is raised and task is marked escalated.
- Dispatcher dashboard (Angular Material):
  - Task table with batch select/approve.
  - Per-task approve/reject/return actions.
  - Stepper component rendering progress stages.

## Unified Message Center
- Internal delivery mechanism uses an in-app message queue table (`message_queue_events`) without external SMS/Push dependencies.
- Message categories:
  - `RESERVATION_SUCCESS`
  - `ARRIVAL_REMINDER`
  - `MISSED_CHECK_IN` (5 minutes after start time)
- Scheduled jobs (`@Scheduled`):
  - Generate reservation success messages.
  - Generate arrival reminders using per-user lead time and DND checks.
  - Generate missed check-in alerts.
  - Process queue events into inbox messages.
- Inbox supports:
  - Read/unread toggling.
  - Delete.
  - Filter by message type.
  - Polling-based real-time updates on the Angular client.
- Sensitive fields (phone/ID-like tokens) are masked before rendering.

## Data Integration & Cleaning Engine
- Admin ingestion endpoint accepts structured payloads in either `JSON` or `HTML` template format.
- Parsing layer maps heterogeneous raw field names to standard canonical fields:
  - stop name
  - address
  - apartment type
  - area
  - price
- Cleaning rules:
  - Area normalized to `㎡` (e.g., `88 sqm` -> `88 ㎡`).
  - Price normalized to `yuan/month` (e.g., `2200 RMB` -> `2200 yuan/month`).
  - Missing values mapped to literal `NULL` and recorded with source reference.
- Audit trail persists per-field transformation events in `cleaning_audit_logs`.
- Versioning:
  - Any stop-structure field change creates a new row in `stop_structure_versions`.
  - Version records include old/new values, version number, and import job linkage.
- Import job orchestration tracks lifecycle in `import_jobs` with row-level success/failure counters.

## Administrator Control Panel
- Admin-only configuration domain provides real-time system behavior tuning without redeploy:
  - Notification templates (`notification_templates`)
  - Search algorithm weights (`system_configs`)
  - Standard field dictionaries (`field_dictionaries`)
- Search ranking reads dynamic weights from DB keys:
  - `search.weight.relevance`
  - `search.weight.frequency`
  - `search.weight.popularity`
- Angular admin UI offers CRUD tables/forms for all three configuration groups.

## Observability & Health Monitoring
- Stack: Spring Boot Actuator + Micrometer + structured JSON logs.
- Traceability:
  - Incoming requests receive/propagate `X-Trace-Id`.
  - Trace ID included in logs and message queue records for end-to-end tracking.
- Metrics captured:
  - API request duration (`app.api.request.duration`, percentiles including P95).
  - Workflow timings for search/parsing/queue handling.
  - Queue backlog gauge.
- Alert diagnostics:
  - Queue backlog threshold breach.
  - API P95 latency threshold breach (> configurable ms).
  - Alert records persisted in `alert_diagnostics`.
- Health endpoints:
  - `/actuator/health`
  - `/actuator/metrics`
- Local PostgreSQL backup strategy provided via shell scripts for backup/restore.

## Tech Stack
- Backend: Java 17, Spring Boot, Spring Security, Spring Data JPA, JJWT.
- Frontend: Angular 17, TypeScript, RxJS.
- Database: PostgreSQL 16.
- Containers: Docker + Docker Compose.

## Database Schema
- `users`
  - `id` BIGSERIAL PRIMARY KEY
  - `username` VARCHAR(120) UNIQUE NOT NULL
  - `password_hash` VARCHAR(255) NOT NULL
  - `role` VARCHAR(20) CHECK in PASSENGER/DISPATCHER/ADMIN
  - `enabled` BOOLEAN NOT NULL
  - `created_at` TIMESTAMPTZ NOT NULL
  - `updated_at` TIMESTAMPTZ NOT NULL
