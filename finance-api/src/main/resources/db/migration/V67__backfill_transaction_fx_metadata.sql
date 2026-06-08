-- Default payment currency for legacy rows missing trade metadata.
UPDATE transactions t
SET input_currency = 'TRY'
FROM instruments i
WHERE t.instrument_id = i.id
  AND t.input_currency IS NULL
  AND (
    i.type = 'FX'
    OR i.symbol LIKE '%TRY'
    OR i.exchange IN ('BIST', 'YAHOO', 'TEFAS', 'TCMB')
  );

-- Domestic TRY stocks: same-currency trades store unit FX of 1.
UPDATE transactions t
SET fx_rate_used = 1
FROM instruments i
WHERE t.instrument_id = i.id
  AND t.fx_rate_used IS NULL
  AND t.input_currency = 'TRY'
  AND i.type = 'STOCK'
  AND i.symbol NOT LIKE '%TRY'
  AND (
    i.exchange IN ('BIST', 'YAHOO', 'TEFAS', 'TCMB')
    OR i.symbol IN ('GARAN', 'ASELS', 'THYAO', 'AKBNK', 'YKBNK', 'ISCTR', 'EKGYO', 'KCHOL', 'TUPRS', 'SAHOL')
  );

-- TRY-listed FX / metal pairs: hub snapshot when available, else unit price.
UPDATE transactions t
SET fx_rate_used = CASE
    WHEN i.symbol = 'USDTRY' THEN taf.usd_try
    WHEN i.symbol = 'EURTRY' THEN taf.eur_try
    WHEN i.symbol = 'GBPTRY' THEN taf.gbp_try
    WHEN i.symbol = 'JPYTRY' THEN taf.jpy_try
    WHEN i.symbol = 'AEDTRY' THEN taf.aed_try
    ELSE COALESCE(t.unit_price, t.price)
  END
FROM instruments i,
     transaction_acquisition_fx taf
WHERE t.instrument_id = i.id
  AND taf.transaction_id = t.id
  AND t.fx_rate_used IS NULL
  AND t.input_currency = 'TRY'
  AND i.type <> 'STOCK'
  AND (i.type = 'FX' OR i.symbol LIKE '%TRY');

UPDATE transactions t
SET fx_rate_used = COALESCE(t.unit_price, t.price)
FROM instruments i
WHERE t.instrument_id = i.id
  AND t.fx_rate_used IS NULL
  AND t.input_currency = 'TRY'
  AND i.type <> 'STOCK'
  AND (i.type = 'FX' OR i.symbol LIKE '%TRY');
