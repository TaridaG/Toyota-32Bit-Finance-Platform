ALTER TABLE mds_viop_contract_catalog
    ADD COLUMN IF NOT EXISTS pazar VARCHAR(16);

CREATE INDEX IF NOT EXISTS idx_mds_viop_contract_pazar
    ON mds_viop_contract_catalog(pazar)
    WHERE is_active = TRUE;
