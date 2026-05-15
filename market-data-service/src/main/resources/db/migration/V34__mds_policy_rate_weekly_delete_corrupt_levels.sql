-- Rows from older ingest that read kanuni faiz / wrong column (e.g. ~3683 "percent") instead of policy repo level (~37).
DELETE FROM mds_tcmb_policy_rate_weekly
WHERE rate_percent IS NOT NULL
  AND (rate_percent > 200 OR rate_percent < 0);
