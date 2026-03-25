# API Specification

| Method | Path | Description | Params | Response |
|---|---|---|---|---|
| POST | /api/auth/login | Authenticate and issue JWT token | username, password | token, tokenType, expiresInSeconds, username, role |
| POST | /api/admin/users | Create user with BCrypt-hashed password (ADMIN only) | username, password(min 8), role | id, username, role, enabled |
| GET | /api/passenger/ping | RBAC probe endpoint for passenger scope | Bearer token | message |
| GET | /api/passenger/search?q=<query> | Smart passenger search with autocomplete suggestions and ranked deduplicated stops | Bearer token, query optional | query, suggestions[], results[] |
| GET | /api/passenger/preferences | Read current user's notification preferences and DND status | Bearer token | username, toggles, reminderLeadMinutes, dnd window, dndActiveNow |
| PUT | /api/passenger/preferences | Update current user's notification preferences and DND window | Bearer token + JSON body | updated preference profile |
| POST | /api/dispatcher/workflows | Create dispatcher workflow task | Bearer token + type/mode/title/payload | workflow task |
| GET | /api/dispatcher/workflows | List dispatcher workflow tasks | Bearer token | workflow task[] |
| GET | /api/dispatcher/workflows/{id} | Get single task details and progress steps | Bearer token | task + progress[] |
| PUT | /api/dispatcher/workflows/{id}/approve | Approve task step | Bearer token + reason | updated task |
| PUT | /api/dispatcher/workflows/{id}/reject | Reject task | Bearer token + reason | updated task |
| PUT | /api/dispatcher/workflows/{id}/return | Return task to submitter | Bearer token + reason | updated task |
| PUT | /api/dispatcher/workflows/batch/approve | Batch approve task list | Bearer token + taskIds[] | updated tasks[] |
| POST | /api/dispatcher/workflows/escalations/evaluate | Mark stale tasks (>24h) as escalated | Bearer token | escalated tasks[] |
| GET | /api/passenger/messages-center | Unified inbox list (supports `type` filter) | Bearer token, optional type | messages[] |
| PUT | /api/passenger/messages-center/{id}/read | Mark message read/unread | Bearer token + read flag | updated message |
| DELETE | /api/passenger/messages-center/{id} | Delete message from inbox | Bearer token | 204 |
| POST | /api/passenger/messages-center/booking-events | Seed booking event for scheduler-generated messages | Bearer token + route/phone/id/startTime | bookingEventId |
| POST | /api/admin/integration/import | Ingest raw HTML/JSON payload and run cleaning pipeline | Admin token + format/sourceName/payload | import summary with notes |
| GET | /api/admin/integration/jobs | List import jobs and statuses | Admin token | job list |
| GET | /api/admin/integration/audit?jobId=... | Field-level cleaning audit for import job | Admin token | audit log list |
| GET | /api/admin/integration/versions?stopName=... | Stop structure version history (field-level) | Admin token | version entries |
| GET | /api/admin/control/search-weights | Get configurable search ranking weights | Admin token | relevance/frequency/popularity weights |
| PUT | /api/admin/control/search-weights | Update search ranking weights in real-time | Admin token + weights | updated weights |
| GET | /api/admin/control/templates | List notification templates | Admin token | template list |
| POST | /api/admin/control/templates | Create notification template | Admin token + key/subject/body | created template |
| PUT | /api/admin/control/templates/{id} | Update notification template | Admin token + key/subject/body | updated template |
| DELETE | /api/admin/control/templates/{id} | Delete notification template | Admin token | 204 |
| GET | /api/admin/control/dictionaries | List field dictionary entries (optional category filter) | Admin token | dictionary list |
| POST | /api/admin/control/dictionaries | Create dictionary entry | Admin token + category/code/value/enabled | created entry |
| PUT | /api/admin/control/dictionaries/{id} | Update dictionary entry | Admin token + category/code/value/enabled | updated entry |
| DELETE | /api/admin/control/dictionaries/{id} | Delete dictionary entry | Admin token | 204 |
| GET | /api/admin/observability/snapshot | Monitoring snapshot including queue backlog and p95 metrics | Admin token | snapshot object |
| GET | /api/admin/observability/alerts | Recent local diagnostic alerts | Admin token | alert list |
| GET | /api/dispatcher/ping | RBAC probe endpoint for dispatcher scope | Bearer token | message |
| GET | /api/admin/ping | RBAC probe endpoint for admin scope | Bearer token | message |
| POST | /api/passenger/messages/desensitize | Apply sensitivity masking by role | content, sensitivityLevel | original, masked, sensitivityLevel, visibleToRole |

## Login Example
Request:
```json
{
  "username": "admin",
  "password": "admin1234"
}
```

Response:
```json
{
  "token": "<jwt>",
  "tokenType": "Bearer",
  "expiresInSeconds": 3600,
  "username": "admin",
  "role": "ADMIN"
}
```
