ALTER TABLE info_cards
    ADD COLUMN IF NOT EXISTS target_instrument_symbols JSONB NOT NULL DEFAULT '[]'::jsonb;

CREATE INDEX IF NOT EXISTS idx_info_cards_target_instruments_gin
    ON info_cards USING GIN (target_instrument_symbols);
