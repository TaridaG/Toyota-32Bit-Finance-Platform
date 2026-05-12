package com.company.finance_api.portfolio.external.domain;

import com.company.finance_api.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "external_portfolios")
public class ExternalPortfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency = "TRY";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** UI: hide monetary amounts for this portfolio (reserved; default false). */
    @Column(name = "amounts_hidden", nullable = false)
    private boolean amountsHidden = false;

    public ExternalPortfolio(User user, String name, String baseCurrency) {
        this.user = user;
        this.name = name;
        this.baseCurrency = baseCurrency == null || baseCurrency.isBlank() ? "TRY" : baseCurrency;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void rename(String newName) {
        this.name = newName;
    }
}

