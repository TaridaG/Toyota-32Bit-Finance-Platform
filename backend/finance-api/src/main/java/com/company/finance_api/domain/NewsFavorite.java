package com.company.finance_api.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "news_favorites",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_news_favorites_user_news", columnNames = {"user_id", "news_id"})
        },
        indexes = {
                @Index(name = "idx_news_favorites_user", columnList = "user_id"),
                @Index(name = "idx_news_favorites_news", columnList = "news_id"),
                @Index(name = "idx_news_favorites_active", columnList = "active")
        }
)
public class NewsFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "news_id", nullable = false)
    private Long newsId;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NewsFavorite() {
    }

    private NewsFavorite(User user, Long newsId) {
        this.user = user;
        this.newsId = newsId;
        this.active = true;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static NewsFavorite create(User user, Long newsId) {
        return new NewsFavorite(user, newsId);
    }

    public void activate() {
        this.active = true;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Long getNewsId() {
        return newsId;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
