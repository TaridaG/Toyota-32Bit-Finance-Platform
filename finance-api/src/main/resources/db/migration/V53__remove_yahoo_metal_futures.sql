-- Remove Yahoo metal futures instruments (vadeli maden); keep TRY spot metals (XAUTRY, …).

DELETE FROM watchlist_items
WHERE instrument_id IN (
    SELECT id FROM instruments WHERE symbol IN ('GC=F', 'SI=F', 'HG=F', 'PA=F', 'PL=F')
);

DELETE FROM alarm_rules
WHERE instrument_id IN (
    SELECT id FROM instruments WHERE symbol IN ('GC=F', 'SI=F', 'HG=F', 'PA=F', 'PL=F')
);

DELETE FROM chart_drawing_saves
WHERE instrument_id IN (
    SELECT id FROM instruments WHERE symbol IN ('GC=F', 'SI=F', 'HG=F', 'PA=F', 'PL=F')
)
   OR asset_symbol IN ('GC=F', 'SI=F', 'HG=F', 'PA=F', 'PL=F');

DELETE FROM external_position_lots
WHERE instrument_id IN (
    SELECT id FROM instruments WHERE symbol IN ('GC=F', 'SI=F', 'HG=F', 'PA=F', 'PL=F')
);

DELETE FROM instrument_prices
WHERE instrument_id IN (
    SELECT id FROM instruments WHERE symbol IN ('GC=F', 'SI=F', 'HG=F', 'PA=F', 'PL=F')
);

DELETE FROM transactions
WHERE instrument_id IN (
    SELECT id FROM instruments WHERE symbol IN ('GC=F', 'SI=F', 'HG=F', 'PA=F', 'PL=F')
);

DELETE FROM instruments
WHERE symbol IN ('GC=F', 'SI=F', 'HG=F', 'PA=F', 'PL=F');
