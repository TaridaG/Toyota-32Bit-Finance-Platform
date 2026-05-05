package com.company.newsservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
        name = "news_article_translations",
        indexes = {
                @Index(name = "idx_news_translation_article_lang", columnList = "news_article_id,languageCode")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_news_translation_article_lang", columnNames = {"news_article_id", "languageCode"})
        }
)
public class NewsArticleTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "news_article_id", nullable = false)
    private NewsArticle newsArticle;

    @Column(nullable = false, length = 16)
    private String languageCode;

    @Column(nullable = false, length = 500)
    private String titleTranslated;

    @Column(length = 2000)
    private String summaryTranslated;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public NewsArticle getNewsArticle() {
        return newsArticle;
    }

    public void setNewsArticle(NewsArticle newsArticle) {
        this.newsArticle = newsArticle;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public String getTitleTranslated() {
        return titleTranslated;
    }

    public void setTitleTranslated(String titleTranslated) {
        this.titleTranslated = titleTranslated;
    }

    public String getSummaryTranslated() {
        return summaryTranslated;
    }

    public void setSummaryTranslated(String summaryTranslated) {
        this.summaryTranslated = summaryTranslated;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
