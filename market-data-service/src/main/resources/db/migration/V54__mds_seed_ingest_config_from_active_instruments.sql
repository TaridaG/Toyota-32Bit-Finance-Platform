INSERT INTO mds_ingest_config (instrument_id, segment, enabled)
SELECT i.id,
       CASE
           WHEN i.type = 'CRYPTO' THEN 'CRYPTO'
           WHEN i.type = 'STOCK' AND i.exchange = 'BIST' THEN 'BIST'
           WHEN i.type = 'STOCK' AND i.exchange IN ('NASDAQ', 'FINNHUB', 'YAHOO') THEN 'NASDAQ'
       END AS segment,
       TRUE
FROM instruments i
WHERE i.active = TRUE
  AND (
    i.type = 'CRYPTO'
        OR (i.type = 'STOCK' AND i.exchange IN ('BIST', 'NASDAQ', 'FINNHUB', 'YAHOO'))
    )
  AND NOT EXISTS (
    SELECT 1
    FROM mds_ingest_config c
    WHERE c.instrument_id = i.id
      AND c.segment = CASE
                          WHEN i.type = 'CRYPTO' THEN 'CRYPTO'
                          WHEN i.type = 'STOCK' AND i.exchange = 'BIST' THEN 'BIST'
                          WHEN i.type = 'STOCK' AND i.exchange IN ('NASDAQ', 'FINNHUB', 'YAHOO') THEN 'NASDAQ'
          END
);
