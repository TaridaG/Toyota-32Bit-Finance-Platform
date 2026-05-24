-- Remove synthetic eurobond seed; live data is ingested via Yahoo chart (see market.tr-usd-eurobond-yahoo).

DELETE FROM eurobond_history WHERE source_provider = 'MOCK_SEED';
DELETE FROM eurobond_quotes WHERE source_provider = 'MOCK_SEED';

UPDATE eurobond_instruments
SET source_provider = 'YAHOO',
    updated_at      = NOW()
WHERE source_provider = 'MOCK_SEED';
