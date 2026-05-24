CREATE TABLE ai_interaction_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_type       VARCHAR(80) NOT NULL,
    language        VARCHAR(10),
    source_language VARCHAR(10),
    target_language VARCHAR(10),
    model           VARCHAR(100),
    status          VARCHAR(30) NOT NULL,
    error_message   TEXT,
    created_by      VARCHAR(150),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    input_hash      VARCHAR(128),
    response_chars  INT
);

CREATE INDEX idx_ai_interaction_log_created_at ON ai_interaction_log (created_at DESC);
CREATE INDEX idx_ai_interaction_log_task_type ON ai_interaction_log (task_type);
