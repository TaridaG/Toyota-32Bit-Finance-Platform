INSERT INTO mds_instrument_catalog (instrument_id, canonical_symbol, asset_class, base_currency, quote_currency, active)
SELECT i.id,
       i.symbol,
       'STOCK',
       NULL,
       'USD',
       TRUE
FROM instruments i
WHERE i.symbol IN ('GC=F', 'SI=F', 'HG=F', 'PA=F', 'PL=F')
  AND NOT EXISTS (
    SELECT 1
    FROM mds_instrument_catalog c
    WHERE c.instrument_id = i.id
  );

INSERT INTO mds_provider_instrument_mapping (provider, provider_symbol, instrument_id, priority, active)
SELECT 'YAHOO', i.symbol, i.id, 0, TRUE
FROM instruments i
WHERE i.symbol IN ('GC=F', 'SI=F', 'HG=F', 'PA=F', 'PL=F')
  AND NOT EXISTS (
    SELECT 1
    FROM mds_provider_instrument_mapping m
    WHERE m.provider = 'YAHOO'
      AND m.provider_symbol = i.symbol
  );
