ALTER TABLE news_articles
    ADD COLUMN IF NOT EXISTS related_symbols JSONB NOT NULL DEFAULT '[]'::jsonb;

CREATE INDEX IF NOT EXISTS idx_news_related_symbols_gin
    ON news_articles USING GIN (related_symbols);
