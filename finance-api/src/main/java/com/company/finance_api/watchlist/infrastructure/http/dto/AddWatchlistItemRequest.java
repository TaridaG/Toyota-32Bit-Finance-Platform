package com.company.finance_api.watchlist.infrastructure.http.dto;

import jakarta.validation.constraints.NotNull;

/** AddWatchlistItemRequest — API transfer nesnesi (DTO/response/request). */
public class AddWatchlistItemRequest {

  @NotNull private Long instrumentId;

  public Long getInstrumentId() {
    return instrumentId;
  }
}
