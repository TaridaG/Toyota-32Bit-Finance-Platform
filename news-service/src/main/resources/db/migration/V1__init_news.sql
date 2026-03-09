CREATE TABLE news_articles (
                               id BIGSERIAL PRIMARY KEY,
                               external_id VARCHAR(500),
                               title VARCHAR(500) NOT NULL,
                               summary VARCHAR(2000),
                               article_url VARCHAR(1200) NOT NULL,
                               source_name VARCHAR(150) NOT NULL,
                               category VARCHAR(50) NOT NULL,
                               published_at TIMESTAMP NOT NULL,
                               created_at TIMESTAMP NOT NULL,
                               updated_at TIMESTAMP NOT NULL,
                               active BOOLEAN NOT NULL DEFAULT TRUE
);

ALTER TABLE news_articles
    ADD CONSTRAINT uk_news_article_url UNIQUE (article_url);

CREATE INDEX idx_news_category_published_at
    ON news_articles (category, published_at DESC);

CREATE INDEX idx_news_source_name
    ON news_articles (source_name);