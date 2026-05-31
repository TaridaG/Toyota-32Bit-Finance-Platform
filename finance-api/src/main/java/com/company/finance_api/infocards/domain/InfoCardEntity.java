package com.company.finance_api.infocards.domain;

import com.company.finance_api.infocards.infrastructure.http.dto.InfoCardLocaleContentDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** InfoCardEntity — JPA domain entity (info card entity). */
@Entity
@Table(name = "info_cards")
public class InfoCardEntity {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(nullable = false, length = 500)
  private String title;

  @Column(nullable = false, length = 500, unique = true)
  private String slug;

  @Column(nullable = false, length = 20)
  private String status;

  @Column(name = "card_type", nullable = false, length = 50)
  private String cardType;

  @Column(nullable = false, length = 20)
  private String difficulty;

  @Column(nullable = false, length = 50)
  private String category;

  @Column(name = "short_description", nullable = false, columnDefinition = "text")
  private String shortDescription;

  @Column(name = "detailed_description", nullable = false, columnDefinition = "text")
  private String detailedDescription;

  @Column(name = "how_to_interpret", columnDefinition = "text")
  private String howToInterpret;

  @Column(name = "common_mistake", columnDefinition = "text")
  private String commonMistake;

  @Column(name = "example_text", columnDefinition = "text")
  private String exampleText;

  @Column(name = "admin_only", nullable = false)
  private boolean adminOnly;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "target_terms", nullable = false, columnDefinition = "jsonb")
  private List<String> targetTerms = new ArrayList<>();

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "target_element_ids", nullable = false, columnDefinition = "jsonb")
  private List<String> targetElementIds = new ArrayList<>();

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "target_instrument_symbols", nullable = false, columnDefinition = "jsonb")
  private List<String> targetInstrumentSymbols = new ArrayList<>();

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private List<String> pages = new ArrayList<>();

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "related_terms", nullable = false, columnDefinition = "jsonb")
  private List<String> relatedTerms = new ArrayList<>();

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private Map<String, InfoCardLocaleContentDto> translations = new LinkedHashMap<>();

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected InfoCardEntity() {}

  public static InfoCardEntity createEmpty() {
    return new InfoCardEntity();
  }

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    Instant now = Instant.now();
    if (createdAt == null) {
      createdAt = now;
    }
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getSlug() {
    return slug;
  }

  public void setSlug(String slug) {
    this.slug = slug;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getCardType() {
    return cardType;
  }

  public void setCardType(String cardType) {
    this.cardType = cardType;
  }

  public String getDifficulty() {
    return difficulty;
  }

  public void setDifficulty(String difficulty) {
    this.difficulty = difficulty;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getShortDescription() {
    return shortDescription;
  }

  public void setShortDescription(String shortDescription) {
    this.shortDescription = shortDescription;
  }

  public String getDetailedDescription() {
    return detailedDescription;
  }

  public void setDetailedDescription(String detailedDescription) {
    this.detailedDescription = detailedDescription;
  }

  public String getHowToInterpret() {
    return howToInterpret;
  }

  public void setHowToInterpret(String howToInterpret) {
    this.howToInterpret = howToInterpret;
  }

  public String getCommonMistake() {
    return commonMistake;
  }

  public void setCommonMistake(String commonMistake) {
    this.commonMistake = commonMistake;
  }

  public String getExampleText() {
    return exampleText;
  }

  public void setExampleText(String exampleText) {
    this.exampleText = exampleText;
  }

  public boolean isAdminOnly() {
    return adminOnly;
  }

  public void setAdminOnly(boolean adminOnly) {
    this.adminOnly = adminOnly;
  }

  public List<String> getTargetTerms() {
    return targetTerms;
  }

  public void setTargetTerms(List<String> targetTerms) {
    this.targetTerms = targetTerms != null ? new ArrayList<>(targetTerms) : new ArrayList<>();
  }

  public List<String> getTargetElementIds() {
    return targetElementIds;
  }

  public void setTargetElementIds(List<String> targetElementIds) {
    this.targetElementIds =
        targetElementIds != null ? new ArrayList<>(targetElementIds) : new ArrayList<>();
  }

  public List<String> getTargetInstrumentSymbols() {
    return targetInstrumentSymbols;
  }

  public void setTargetInstrumentSymbols(List<String> targetInstrumentSymbols) {
    this.targetInstrumentSymbols =
        targetInstrumentSymbols != null
            ? new ArrayList<>(targetInstrumentSymbols)
            : new ArrayList<>();
  }

  public List<String> getPages() {
    return pages;
  }

  public void setPages(List<String> pages) {
    this.pages = pages != null ? new ArrayList<>(pages) : new ArrayList<>();
  }

  public List<String> getRelatedTerms() {
    return relatedTerms;
  }

  public void setRelatedTerms(List<String> relatedTerms) {
    this.relatedTerms = relatedTerms != null ? new ArrayList<>(relatedTerms) : new ArrayList<>();
  }

  public Map<String, InfoCardLocaleContentDto> getTranslations() {
    return translations;
  }

  public void setTranslations(Map<String, InfoCardLocaleContentDto> translations) {
    this.translations =
        translations != null ? new LinkedHashMap<>(translations) : new LinkedHashMap<>();
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
