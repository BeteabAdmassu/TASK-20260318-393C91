# Business Logic Questions Log

Required Document Description: Business Logic Questions Log

1. Authentication token strategy in an offline LAN
- **Question**: The prompt allows "JWTs or session tokens" but does not define a required mechanism for decoupled frontend/backend APIs.
- **My understanding/hypothesis**: A stateless JWT flow is the best fit for Angular + REST in an offline LAN because it avoids sticky session handling and keeps API authorization uniform.
- **Solution**: Implemented JWT-based authentication with role claims and Spring Security RBAC checks for Passenger/Dispatcher/Admin APIs.

2. Sensitivity masking policy across roles
- **Question**: The prompt says message content must be desensitized by sensitivity level, but does not define exact role-based visibility.
- **My understanding/hypothesis**: Three levels are needed: `LOW` (no masking), `MEDIUM` (partial mask for passenger view), and `HIGH` (redacted for passenger/dispatcher, full for admin only).
- **Solution**: Added sensitivity-aware masking rules and role-dependent response shaping in message-related services.

3. Search ranking formula interpretation
- **Question**: "Frequency priority + stop popularity" is specified, but exact tie-breaking and dedup behavior are not explicitly defined.
- **My understanding/hypothesis**: Composite scoring should prioritize frequently searched entries first, then stop popularity, with deterministic fallback ordering and duplicate suppression by stop identity.
- **Solution**: Implemented weighted ranking with deterministic sort keys and deduplication in the search pipeline.

4. Pinyin/initial matching behavior scope
- **Question**: The prompt requires pinyin/initial matching (example: `bj`) but does not clarify whether matching should apply to routes, stops, or both.
- **My understanding/hypothesis**: Matching should be supported for stop names and relevant searchable text fields, while route-number exact/partial matching remains supported.
- **Solution**: Added multi-strategy matching (route number, keyword, pinyin/initial-style input) and integrated with autocomplete suggestions.

5. Do-not-disturb interval crossing midnight
- **Question**: DND example is `22:00-07:00`, but cross-day boundary handling is not explicitly defined.
- **My understanding/hypothesis**: DND windows can span midnight and should suppress reminder delivery for any time inside the interval, regardless of date boundary.
- **Solution**: Implemented DND evaluation logic that correctly handles both same-day and overnight ranges.

6. Missed check-in trigger semantics
- **Question**: The prompt states missed check-in is triggered 5 minutes after start time, but does not define processing model for delayed scheduler execution.
- **My understanding/hypothesis**: Scheduler should evaluate eligibility using persisted booking start times and generate events idempotently when threshold is reached.
- **Solution**: Implemented scheduled generation logic with threshold checks and queue-based message creation.

7. Workflow "joint/parallel approvals" semantics
- **Question**: The prompt requires conditional branching and joint/parallel approvals, but does not define minimum collaboration rules or assignment ownership model.
- **My understanding/hypothesis**: Workflow states should support multi-step progress and approval actions beyond a single linear path, with explicit authorization checks.
- **Solution**: Implemented workflow state-machine behavior with approve/reject/return actions, progress tracking, and dispatcher authorization boundaries.

8. Timeout escalation automation
- **Question**: Prompt requires escalation for tasks unprocessed for 24 hours but does not specify whether this is automatic or manually invoked.
- **My understanding/hypothesis**: Escalation should be evaluated automatically on a schedule to prevent missed stale tasks.
- **Solution**: Added stale-task evaluation and escalation handling path aligned with 24-hour timeout policy.

9. Missing-value standardization in integration pipeline
- **Question**: Prompt says missing fields are "marked as NULL with source logging," but raw-template parsing can produce empty strings or textual placeholders.
- **My understanding/hypothesis**: Database persistence should use actual SQL `NULL` values while preserving audit/source metadata for traceability.
- **Solution**: Applied normalization rules that convert missing/blank values to nullable fields and retain cleaning audit logs.

10. Backup strategy operability in containerized local deployment
- **Question**: Prompt requires a local PostgreSQL backup strategy but does not define operational assumptions for container names or service wiring.
- **My understanding/hypothesis**: Backup/restore scripts must align with docker-compose service/container naming to be runnable out of the box.
- **Solution**: Updated backup/restore scripts and docs to match compose-managed PostgreSQL runtime conventions.
