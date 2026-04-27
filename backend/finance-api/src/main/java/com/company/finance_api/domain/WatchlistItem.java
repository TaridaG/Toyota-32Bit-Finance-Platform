package com.company.finance_api.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "watchlist_items",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_watchlist_user_instrument", columnNames = {"user_id", "instrument_id"})
        },
        indexes = {
                @Index(name = "idx_watchlist_user", columnList = "user_id"),
                @Index(name = "idx_watchlist_instrument", columnList = "instrument_id"),
                @Index(name = "idx_watchlist_active", columnList = "active")
        }
)
public class WatchlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WatchlistItem() {
    }

    private WatchlistItem(User user, Instrument instrument) {
        this.user = user;
        this.instrument = instrument;
        this.active = true;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static WatchlistItem create(User user, Instrument instrument) {
        return new WatchlistItem(user, instrument);
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

    public Instrument getInstrument() {
        return instrument;
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
