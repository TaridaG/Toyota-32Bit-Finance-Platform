-- Temporary/manual seed for BIST shares outstanding to enable market-cap derivation
-- when external endpoints are throttled/blocked.
INSERT INTO mds_instrument_shares_outstanding (
    instrument_id,
    canonical_symbol,
    shares_outstanding,
    source,
    provider_symbol,
    verified_at
)
SELECT c.instrument_id,
       c.canonical_symbol,
       s.shares_outstanding,
       'MANUAL_SEED',
       c.canonical_symbol,
       NOW()
FROM mds_instrument_catalog c
JOIN (
    VALUES
        ('GARAN', 4200000000::numeric),
        ('THYAO', 1380000000::numeric),
        ('ASELS', 4560000000::numeric)
) AS s(symbol, shares_outstanding)
    ON c.canonical_symbol = s.symbol
WHERE UPPER(c.asset_class) = 'STOCK'
ON CONFLICT (instrument_id) DO UPDATE
SET canonical_symbol = EXCLUDED.canonical_symbol,
    shares_outstanding = EXCLUDED.shares_outstanding,
    source = EXCLUDED.source,
    provider_symbol = EXCLUDED.provider_symbol,
    verified_at = EXCLUDED.verified_at;
