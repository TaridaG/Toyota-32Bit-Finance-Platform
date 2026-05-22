-- Re-sync CRYPTO catalog after finance-api V48 adds instruments (idempotent; safe if V41 already ran).

INSERT INTO mds_instrument_catalog (
    instrument_id, canonical_symbol, asset_class, base_currency, quote_currency, active
)
SELECT i.id,
       i.symbol,
       i.type,
       CASE WHEN i.symbol LIKE '%USDT' THEN SUBSTRING(i.symbol, 1, LENGTH(i.symbol) - 4) ELSE NULL END,
       CASE WHEN i.symbol LIKE '%USDT' THEN 'USDT' ELSE NULL END,
       i.active
FROM instruments i
WHERE i.type = 'CRYPTO'
  AND i.active = TRUE
  AND NOT EXISTS (
      SELECT 1 FROM mds_instrument_catalog c WHERE c.instrument_id = i.id
  );

INSERT INTO mds_provider_instrument_mapping (provider, provider_symbol, instrument_id, priority, active)
SELECT 'BINANCE', i.symbol, i.id, 0, TRUE
FROM instruments i
WHERE i.type = 'CRYPTO'
  AND i.active = TRUE
  AND NOT EXISTS (
      SELECT 1 FROM mds_provider_instrument_mapping m
      WHERE m.provider = 'BINANCE' AND m.provider_symbol = i.symbol
  );

INSERT INTO mds_provider_instrument_mapping (provider, provider_symbol, instrument_id, priority, active)
SELECT 'COINGECKO', i.symbol, i.id, 10, TRUE
FROM instruments i
WHERE i.type = 'CRYPTO'
  AND i.active = TRUE
  AND NOT EXISTS (
      SELECT 1 FROM mds_provider_instrument_mapping m
      WHERE m.provider = 'COINGECKO' AND m.provider_symbol = i.symbol
  );
