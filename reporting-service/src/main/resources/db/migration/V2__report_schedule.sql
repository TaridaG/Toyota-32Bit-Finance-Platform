CREATE TABLE report_schedule
(
    id BIGSERIAL PRIMARY KEY,
    schedule_uuid UUID NOT NULL UNIQUE,
    report_type VARCHAR(32) NOT NULL,
    export_format VARCHAR(16) NOT NULL,
    frequency VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    preferred_hour INTEGER NOT NULL,
    preferred_minute INTEGER NOT NULL,
    preferred_day_of_week VARCHAR(16),
    preferred_day_of_month INTEGER,
    next_run_at TIMESTAMP NOT NULL,
    last_run_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_report_schedule_status_next_run
    ON report_schedule (status, next_run_at);

CREATE INDEX idx_report_schedule_uuid
    ON report_schedule (schedule_uuid);