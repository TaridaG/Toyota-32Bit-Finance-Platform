ALTER TABLE mds_backfill_state ADD COLUMN IF NOT EXISTS attempt_count BIGINT NOT NULL DEFAULT 0;
ALTER TABLE mds_backfill_state ADD COLUMN IF NOT EXISTS error_code VARCHAR(128);
ALTER TABLE mds_backfill_state ADD COLUMN IF NOT EXISTS error_message TEXT;
ALTER TABLE mds_backfill_state ADD COLUMN IF NOT EXISTS next_retry_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE mds_backfill_state ADD COLUMN IF NOT EXISTS run_id VARCHAR(64);

UPDATE mds_backfill_state
SET status = 'NOT_STARTED'
WHERE status = 'NEW';

UPDATE mds_backfill_state
SET attempt_count = 0
WHERE attempt_count IS NULL;

CREATE INDEX IF NOT EXISTS idx_mds_backfill_state_status_next_retry
    ON mds_backfill_state (status, next_retry_at);

CREATE INDEX IF NOT EXISTS idx_mds_backfill_state_run_id
    ON mds_backfill_state (run_id);
