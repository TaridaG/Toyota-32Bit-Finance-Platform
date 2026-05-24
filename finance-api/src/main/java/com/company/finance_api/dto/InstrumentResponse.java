package com.company.finance_api.dto;

/** InstrumentResponse — API transfer nesnesi (DTO/response/request). */
public class InstrumentResponse {

  private Long id;
  private String symbol;
  private String name;
  private String type;
  private String exchange;

  public InstrumentResponse(Long id, String symbol, String name, String type, String exchange) {
    this.id = id;
    this.symbol = symbol;
    this.name = name;
    this.type = type;
    this.exchange = exchange;
  }

  public Long getId() {
    return id;
  }

  public String getSymbol() {
    return symbol;
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public String getExchange() {
    return exchange;
  }
}
