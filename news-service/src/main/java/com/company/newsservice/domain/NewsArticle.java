package com.company.newsservice.domain;

import com.company.newsservice.domain.enums.NewsCategory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        name = "news_articles",
        indexes = {
                @Index(name = "idx_news_category_published_at", columnList = "category,publishedAt"),
                @Index(name = "idx_news_source_name", columnList = "sourceName")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_news_article_url", columnNames = "articleUrl")
        }
)
public class NewsArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500)
    private String externalId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 2000)
    private String summary;

    @Column(nullable = false, length = 1200)
    private String articleUrl;

    @Column(nullable = false, length = 150)
    private String sourceName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NewsCategory category;

    @Column(nullable = false)
    private Instant publishedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private boolean active = true;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.publishedAt == null) {
            this.publishedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}