CREATE TABLE outbox_events (
                               id BIGSERIAL PRIMARY KEY,

                               topic VARCHAR(200) NOT NULL,
                               message_key VARCHAR(200) NULL,

                               event_type VARCHAR(300) NOT NULL,      -- e.g. com.company.finance_api.event.TransactionExecutedEvent
                               payload_json TEXT NOT NULL,

                               status VARCHAR(30) NOT NULL,           -- NEW, SENT, RETRY, DEAD
                               attempts INT NOT NULL DEFAULT 0,

                               next_attempt_at TIMESTAMP NULL,
                               last_error TEXT NULL,

                               created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                               sent_at TIMESTAMP NULL
);

CREATE INDEX idx_outbox_status_next_attempt
    ON outbox_events (status, next_attempt_at);

CREATE INDEX idx_outbox_created_at
    ON outbox_events (created_at);