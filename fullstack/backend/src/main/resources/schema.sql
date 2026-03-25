CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('PASSENGER', 'DISPATCHER', 'ADMIN')),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    arrival_reminder_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    reservation_success_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    reminder_lead_minutes INTEGER NOT NULL DEFAULT 10,
    dnd_start TIME,
    dnd_end TIME,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS workflow_tasks (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(40) NOT NULL CHECK (type IN ('ROUTE_DATA_CHANGE', 'REMINDER_CONFIGURATION', 'ABNORMAL_DATA_REVIEW')),
    mode VARCHAR(20) NOT NULL CHECK (mode IN ('CONDITIONAL', 'JOINT', 'PARALLEL')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('SUBMITTED', 'IN_REVIEW', 'APPROVED', 'REJECTED', 'RETURNED')),
    title VARCHAR(200) NOT NULL,
    payload VARCHAR(3000) NOT NULL,
    submitted_by VARCHAR(120) NOT NULL,
    assigned_to VARCHAR(120) NOT NULL,
    current_step INTEGER NOT NULL,
    total_steps INTEGER NOT NULL,
    required_approvals INTEGER NOT NULL,
    received_approvals INTEGER NOT NULL,
    escalated BOOLEAN NOT NULL DEFAULT FALSE,
    last_action_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS workflow_task_collaborators (
    task_id BIGINT NOT NULL,
    collaborator VARCHAR(120) NOT NULL,
    PRIMARY KEY (task_id, collaborator),
    CONSTRAINT fk_workflow_collaborators_task FOREIGN KEY (task_id) REFERENCES workflow_tasks (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS messages (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(120) NOT NULL,
    type VARCHAR(40) NOT NULL CHECK (type IN ('RESERVATION_SUCCESS', 'ARRIVAL_REMINDER', 'MISSED_CHECK_IN')),
    title VARCHAR(160) NOT NULL,
    content VARCHAR(4000) NOT NULL,
    sensitivity_level VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' CHECK (sensitivity_level IN ('LOW', 'MEDIUM', 'HIGH')),
    trace_id VARCHAR(80) NOT NULL,
    read_flag BOOLEAN NOT NULL DEFAULT FALSE,
    masked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS message_queue_events (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(120) NOT NULL,
    type VARCHAR(40) NOT NULL CHECK (type IN ('RESERVATION_SUCCESS', 'ARRIVAL_REMINDER', 'MISSED_CHECK_IN')),
    title VARCHAR(160) NOT NULL,
    content VARCHAR(4000) NOT NULL,
    sensitivity_level VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' CHECK (sensitivity_level IN ('LOW', 'MEDIUM', 'HIGH')),
    trace_id VARCHAR(80) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'PROCESSED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS booking_events (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(120) NOT NULL,
    route_number VARCHAR(40) NOT NULL,
    passenger_phone_token VARCHAR(120) NOT NULL,
    passenger_id_card_token VARCHAR(120) NOT NULL,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    reservation_success_sent BOOLEAN NOT NULL DEFAULT FALSE,
    arrival_reminder_sent BOOLEAN NOT NULL DEFAULT FALSE,
    missed_check_in_sent BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS import_jobs (
    id BIGSERIAL PRIMARY KEY,
    format VARCHAR(20) NOT NULL CHECK (format IN ('JSON', 'HTML')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED')),
    source_name VARCHAR(200) NOT NULL,
    total_rows INTEGER NOT NULL DEFAULT 0,
    success_rows INTEGER NOT NULL DEFAULT 0,
    failed_rows INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS cleaned_records (
    id BIGSERIAL PRIMARY KEY,
    import_job_id BIGINT NOT NULL,
    source_ref VARCHAR(255) NOT NULL,
    stop_name VARCHAR(200) NOT NULL,
    address VARCHAR(255) NOT NULL,
    apartment_type VARCHAR(120) NOT NULL,
    area_standardized VARCHAR(120) NOT NULL,
    price_standardized VARCHAR(120) NOT NULL,
    raw_snapshot VARCHAR(4000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_cleaned_records_job FOREIGN KEY (import_job_id) REFERENCES import_jobs (id)
);

CREATE TABLE IF NOT EXISTS cleaning_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    import_job_id BIGINT NOT NULL,
    source_ref VARCHAR(255) NOT NULL,
    field_name VARCHAR(120) NOT NULL,
    raw_value VARCHAR(1000) NOT NULL,
    cleaned_value VARCHAR(1000) NOT NULL,
    action VARCHAR(120) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_cleaning_audit_job FOREIGN KEY (import_job_id) REFERENCES import_jobs (id)
);

CREATE TABLE IF NOT EXISTS stop_structure_versions (
    id BIGSERIAL PRIMARY KEY,
    stop_name VARCHAR(200) NOT NULL,
    field_name VARCHAR(120) NOT NULL,
    old_value VARCHAR(1000) NOT NULL,
    new_value VARCHAR(1000) NOT NULL,
    version_number INTEGER NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    import_job_id BIGINT NOT NULL,
    CONSTRAINT fk_stop_versions_job FOREIGN KEY (import_job_id) REFERENCES import_jobs (id)
);

CREATE TABLE IF NOT EXISTS transit_stops (
    id BIGSERIAL PRIMARY KEY,
    route_number VARCHAR(40) NOT NULL,
    stop_name VARCHAR(200) NOT NULL,
    keywords VARCHAR(500) NOT NULL,
    pinyin VARCHAR(200) NOT NULL,
    initials VARCHAR(80) NOT NULL,
    frequency_priority INTEGER NOT NULL,
    stop_popularity INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS system_configs (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(120) NOT NULL UNIQUE,
    config_value VARCHAR(200) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS notification_templates (
    id BIGSERIAL PRIMARY KEY,
    template_key VARCHAR(120) NOT NULL UNIQUE,
    subject VARCHAR(200) NOT NULL,
    body VARCHAR(3000) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS field_dictionaries (
    id BIGSERIAL PRIMARY KEY,
    category VARCHAR(120) NOT NULL,
    code VARCHAR(120) NOT NULL,
    item_value VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS alert_diagnostics (
    id BIGSERIAL PRIMARY KEY,
    alert_type VARCHAR(120) NOT NULL,
    severity VARCHAR(40) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

INSERT INTO system_configs (config_key, config_value)
VALUES ('search.weight.relevance', '1000000')
ON CONFLICT (config_key) DO NOTHING;

INSERT INTO system_configs (config_key, config_value)
VALUES ('search.weight.frequency', '1000')
ON CONFLICT (config_key) DO NOTHING;

INSERT INTO system_configs (config_key, config_value)
VALUES ('search.weight.popularity', '1')
ON CONFLICT (config_key) DO NOTHING;

INSERT INTO system_configs (config_key, config_value)
VALUES ('search.ranking.mode', 'BLENDED')
ON CONFLICT (config_key) DO NOTHING;

INSERT INTO system_configs (config_key, config_value)
VALUES ('cleaning.rule.area.unit', '㎡')
ON CONFLICT (config_key) DO NOTHING;

INSERT INTO system_configs (config_key, config_value)
VALUES ('cleaning.rule.price.unit', 'yuan/month')
ON CONFLICT (config_key) DO NOTHING;

INSERT INTO system_configs (config_key, config_value)
VALUES ('cleaning.rule.missing.marker', 'NULL')
ON CONFLICT (config_key) DO NOTHING;

INSERT INTO system_configs (config_key, config_value)
VALUES ('cleaning.rule.trim.enabled', 'true')
ON CONFLICT (config_key) DO NOTHING;

INSERT INTO notification_templates (template_key, subject, body)
VALUES ('reservation.success', 'Reservation Confirmed', 'Reservation for route {route} confirmed. Reference token {phoneToken}')
ON CONFLICT (template_key) DO NOTHING;

INSERT INTO notification_templates (template_key, subject, body)
VALUES ('arrival.reminder', 'Arrival Reminder', 'Your bus on route {route} arrives in {leadMinutes} minutes. Ref {phoneToken}')
ON CONFLICT (template_key) DO NOTHING;

INSERT INTO notification_templates (template_key, subject, body)
VALUES ('missed.checkin', 'Missed Check-In', 'You missed check-in for route {route}. Please rebook if needed.')
ON CONFLICT (template_key) DO NOTHING;
