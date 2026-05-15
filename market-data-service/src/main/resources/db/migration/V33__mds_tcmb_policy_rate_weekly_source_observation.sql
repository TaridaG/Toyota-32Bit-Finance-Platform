ALTER TABLE mds_tcmb_policy_rate_weekly
    ADD COLUMN IF NOT EXISTS source_observation_date DATE;
