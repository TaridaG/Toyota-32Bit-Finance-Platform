package com.company.finance_api.news.infrastructure.http.dto;

import jakarta.validation.constraints.NotNull;

/** AddNewsFavoriteRequest — API transfer nesnesi (DTO/response/request). */
public class AddNewsFavoriteRequest {

  @NotNull private Long newsId;

  public Long getNewsId() {
    return newsId;
  }

  public void setNewsId(Long newsId) {
    this.newsId = newsId;
  }
}
