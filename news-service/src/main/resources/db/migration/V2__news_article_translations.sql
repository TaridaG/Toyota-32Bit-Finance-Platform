CREATE TABLE news_article_translations (
    id BIGSERIAL PRIMARY KEY,
    news_article_id BIGINT NOT NULL,
    language_code VARCHAR(16) NOT NULL,
    title_translated VARCHAR(500) NOT NULL,
    summary_translated VARCHAR(2000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_news_translation_article
        FOREIGN KEY (news_article_id)
        REFERENCES news_articles(id)
        ON DELETE CASCADE
);

ALTER TABLE news_article_translations
    ADD CONSTRAINT uk_news_translation_article_lang
        UNIQUE (news_article_id, language_code);

CREATE INDEX idx_news_translation_article_lang
    ON news_article_translations (news_article_id, language_code);
