package com.company.finance_api.dto;

import java.time.Instant;

/** NewsFavoriteItemDto — API transfer nesnesi (DTO/response/request). */
public class NewsFavoriteItemDto {

  private final Long newsId;
  private final boolean active;
  private final Instant createdAt;

  public NewsFavoriteItemDto(Long newsId, boolean active, Instant createdAt) {
    this.newsId = newsId;
    this.active = active;
    this.createdAt = createdAt;
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
}
