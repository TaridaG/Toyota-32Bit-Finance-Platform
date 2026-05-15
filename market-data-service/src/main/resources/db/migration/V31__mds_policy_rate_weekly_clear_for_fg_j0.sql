-- Policy rate cache previously defaulted to TP.APIFON4 (weighted avg funding, not headline repo rate).
-- Switch to TP.FG.J0 (official policy) requires a clean resync from EVDS.
DELETE FROM mds_tcmb_policy_rate_weekly;
