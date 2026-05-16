ALTER TABLE news_articles
    ADD COLUMN IF NOT EXISTS topic_tags jsonb NOT NULL DEFAULT '[]'::jsonb;

CREATE INDEX IF NOT EXISTS idx_news_topic_tags_gin ON news_articles USING gin (topic_tags);
