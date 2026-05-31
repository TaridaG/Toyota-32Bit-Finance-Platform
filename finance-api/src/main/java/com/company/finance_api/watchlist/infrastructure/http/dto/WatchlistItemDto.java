package com.company.finance_api.watchlist.infrastructure.http.dto;

import com.company.finance_api.instrument.domain.enums.InstrumentType;
import java.time.Instant;

/** WatchlistItemDto — API transfer nesnesi (DTO/response/request). */
public class WatchlistItemDto {

  private final Long instrumentId;
  private final String symbol;
  private final String name;
  private final InstrumentType type;
  private final boolean active;
  private final Instant createdAt;

  public WatchlistItemDto(
      Long instrumentId,
      String symbol,
      String name,
      InstrumentType type,
      boolean active,
      Instant createdAt) {
    this.instrumentId = instrumentId;
    this.symbol = symbol;
    this.name = name;
    this.type = type;
    this.active = active;
    this.createdAt = createdAt;
  }

  public Long getInstrumentId() {
    return instrumentId;
  }

  public String getSymbol() {
    return symbol;
  }

  public String getName() {
    return name;
  }

  public InstrumentType getType() {
    return type;
  }

  public boolean isActive() {
    return active;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
