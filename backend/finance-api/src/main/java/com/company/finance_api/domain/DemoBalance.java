package com.company.finance_api.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "demo_balances")
public class DemoBalance {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(optional = false)  //user one to one relationship with demobalance
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected DemoBalance() {
        // JPA only
    }

    public DemoBalance(User user, BigDecimal initialBalance, String currency) {
        this.user = user;
        this.balance = initialBalance;
        this.currency = currency;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
