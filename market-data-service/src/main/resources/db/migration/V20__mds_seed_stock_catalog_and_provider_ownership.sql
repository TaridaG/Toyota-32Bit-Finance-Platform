-- Seed stock catalog rows so market-data can resolve tracked symbols.
INSERT INTO mds_instrument_catalog (instrument_id, canonical_symbol, asset_class, base_currency, quote_currency, active)
SELECT i.id,
       i.symbol,
       'STOCK',
       NULL,
       CASE WHEN i.exchange = 'BIST' THEN 'TRY' ELSE 'USD' END,
       TRUE
FROM instruments i
WHERE i.symbol IN ('GARAN', 'THYAO', 'ASELS', 'AAPL', 'AMZN', 'NVDA', 'MSFT', 'GOOGL')
  AND NOT EXISTS (
    SELECT 1
    FROM mds_instrument_catalog c
    WHERE c.instrument_id = i.id
  );

-- Ensure BIST stocks are owned by Yahoo.
INSERT INTO mds_provider_instrument_mapping (provider, provider_symbol, instrument_id, priority, active)
SELECT 'YAHOO', 'GARAN.IS', i.id, 0, TRUE
FROM instruments i
WHERE i.symbol = 'GARAN'
  AND NOT EXISTS (
    SELECT 1 FROM mds_provider_instrument_mapping m
    WHERE m.provider = 'YAHOO' AND m.provider_symbol = 'GARAN.IS'
  );

INSERT INTO mds_provider_instrument_mapping (provider, provider_symbol, instrument_id, priority, active)
SELECT 'YAHOO', 'THYAO.IS', i.id, 0, TRUE
FROM instruments i
WHERE i.symbol = 'THYAO'
  AND NOT EXISTS (
    SELECT 1 FROM mds_provider_instrument_mapping m
    WHERE m.provider = 'YAHOO' AND m.provider_symbol = 'THYAO.IS'
  );

INSERT INTO mds_provider_instrument_mapping (provider, provider_symbol, instrument_id, priority, active)
SELECT 'YAHOO', 'ASELS.IS', i.id, 0, TRUE
FROM instruments i
WHERE i.symbol = 'ASELS'
  AND NOT EXISTS (
    SELECT 1 FROM mds_provider_instrument_mapping m
    WHERE m.provider = 'YAHOO' AND m.provider_symbol = 'ASELS.IS'
  );

-- Ensure US big-tech stocks are owned by Finnhub.
INSERT INTO mds_provider_instrument_mapping (provider, provider_symbol, instrument_id, priority, active)
SELECT 'FINNHUB', i.symbol, i.id, 0, TRUE
FROM instruments i
WHERE i.symbol IN ('AAPL', 'AMZN', 'NVDA', 'MSFT', 'GOOGL')
  AND NOT EXISTS (
    SELECT 1
    FROM mds_provider_instrument_mapping m
    WHERE m.provider = 'FINNHUB'
      AND m.provider_symbol = i.symbol
  );
