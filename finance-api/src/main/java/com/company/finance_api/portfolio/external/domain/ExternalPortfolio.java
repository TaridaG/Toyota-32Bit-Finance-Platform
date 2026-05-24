package com.company.finance_api.portfolio.external.domain;

import com.company.finance_api.domain.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Kullanıcıya ait harici (external) portfolio kaydını temsil eden JPA entity. */
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

  @Column(name = "base_currency", nullable = false, length = 16)
  private String baseCurrency = "MIXED";

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  /** UI için parasal tutarların gizlenip gizlenmeyeceğini belirtir. */
  @Column(name = "amounts_hidden", nullable = false)
  private boolean amountsHidden = false;

  /** Kullanıcı, isim ve baz para birimi ile yeni portfolio oluşturur. */
  public ExternalPortfolio(User user, String name, String baseCurrency) {
    this.user = user;
    this.name = name;
    if (baseCurrency == null || baseCurrency.isBlank()) {
      this.baseCurrency = "MIXED";
    } else {
      this.baseCurrency = baseCurrency.trim().toUpperCase(Locale.ROOT);
    }
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

  /** Portfolio adını günceller. */
  public void rename(String newName) {
    this.name = newName;
  }
}
