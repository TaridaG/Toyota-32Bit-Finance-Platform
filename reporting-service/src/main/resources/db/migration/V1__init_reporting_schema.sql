CREATE TABLE report_metadata
(
    id BIGSERIAL PRIMARY KEY,
    report_uuid UUID NOT NULL UNIQUE,
    report_type VARCHAR(32) NOT NULL,
    export_format VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    instrument_symbol VARCHAR(32),
    content BYTEA,
    generated_file_name VARCHAR(255),
    failure_reason VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_report_metadata_uuid
    ON report_metadata (report_uuid);

CREATE INDEX idx_report_metadata_symbol
    ON report_metadata (instrument_symbol);