CREATE TABLE analytics_processed_events
(
    id BIGSERIAL PRIMARY KEY,
    event_key VARCHAR(255) UNIQUE,
    processed_at TIMESTAMP NOT NULL
);