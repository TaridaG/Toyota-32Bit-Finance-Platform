package com.company.finance_api.portfolio.goal;

import com.company.finance_api.domain.User;
import com.company.finance_api.portfolio.external.domain.ExternalPortfolio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/** Kullanıcı veya external portfolio kapsamında tanımlanan hedef kaydını temsil eden JPA entity. */
@Entity
@Table(name = "portfolio_goals")
public class PortfolioGoal {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "external_portfolio_id")
  private ExternalPortfolio externalPortfolio;

  @Enumerated(EnumType.STRING)
  @Column(name = "goal_type", nullable = false, length = 32)
  private GoalType goalType;

  @Enumerated(EnumType.STRING)
  @Column(name = "profit_target_mode", length = 16)
  private ProfitTargetMode profitTargetMode;

  @Column(name = "target_amount", precision = 19, scale = 4)
  private BigDecimal targetAmount;

  @Column(name = "target_percent", precision = 9, scale = 4)
  private BigDecimal targetPercent;

  @Column(name = "title", nullable = false, length = 200)
  private String title = "";

  @Column(name = "description", length = 2000)
  private String description;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  void onCreate() {
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  /** Birincil anahtar. */
  public Long getId() {
    return id;
  }

  /** Hedefin sahibi kullanıcı. */
  public User getUser() {
    return user;
  }

  /** Hedefin sahibi kullanıcıyı ayarlar. */
  public void setUser(User user) {
    this.user = user;
  }

  /** Hedefin bağlı olduğu external portfolio; null ise kullanıcı geneli kapsam. */
  public ExternalPortfolio getExternalPortfolio() {
    return externalPortfolio;
  }

  /** Hedefin bağlı olduğu external portfolio'yu ayarlar. */
  public void setExternalPortfolio(ExternalPortfolio externalPortfolio) {
    this.externalPortfolio = externalPortfolio;
  }

  /** Hedef türü (portfolio değeri veya kar). */
  public GoalType getGoalType() {
    return goalType;
  }

  /** Hedef türünü ayarlar. */
  public void setGoalType(GoalType goalType) {
    this.goalType = goalType;
  }

  /** Kar hedefi için hedefleme modu; portfolio değer hedeflerinde null. */
  public ProfitTargetMode getProfitTargetMode() {
    return profitTargetMode;
  }

  /** Kar hedefi hedefleme modunu ayarlar. */
  public void setProfitTargetMode(ProfitTargetMode profitTargetMode) {
    this.profitTargetMode = profitTargetMode;
  }

  /** Mutlak hedef tutarı. */
  public BigDecimal getTargetAmount() {
    return targetAmount;
  }

  /** Mutlak hedef tutarını ayarlar. */
  public void setTargetAmount(BigDecimal targetAmount) {
    this.targetAmount = targetAmount;
  }

  /** Yüzde hedef değeri. */
  public BigDecimal getTargetPercent() {
    return targetPercent;
  }

  /** Yüzde hedef değerini ayarlar. */
  public void setTargetPercent(BigDecimal targetPercent) {
    this.targetPercent = targetPercent;
  }

  /** Hedef başlığı. */
  public String getTitle() {
    return title;
  }

  /** Hedef başlığını ayarlar. */
  public void setTitle(String title) {
    this.title = title;
  }

  /** İsteğe bağlı açıklama metni. */
  public String getDescription() {
    return description;
  }

  /** Açıklama metnini ayarlar. */
  public void setDescription(String description) {
    this.description = description;
  }

  /** Oluşturulma zamanı. */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /** Son güncelleme zamanı. */
  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
