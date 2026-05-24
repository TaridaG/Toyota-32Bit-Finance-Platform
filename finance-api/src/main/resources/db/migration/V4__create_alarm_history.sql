CREATE TABLE alarm_history (
                               id BIGSERIAL PRIMARY KEY,
                               user_id UUID NOT NULL,
                               instrument_symbol VARCHAR(50) NOT NULL,
                               condition VARCHAR(50) NOT NULL,
                               price NUMERIC(19, 4) NOT NULL,
                               triggered_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_alarm_history_user_id
    ON alarm_history(user_id);

CREATE INDEX idx_alarm_history_symbol
    ON alarm_history(instrument_symbol);

CREATE INDEX idx_alarm_history_triggered_at
    ON alarm_history(triggered_at);
