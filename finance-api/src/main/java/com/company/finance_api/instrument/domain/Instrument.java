package com.company.finance_api.instrument.domain;

import com.company.finance_api.instrument.domain.enums.Exchange;
import com.company.finance_api.instrument.domain.enums.InstrumentType;
import jakarta.persistence.*;

/** Instrument — JPA domain entity (instrument). */
@Entity
@Table(name = "instruments")
public class Instrument {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String symbol; // BTCUSDT, ASELS, USDTRY

  @Column(nullable = false)
  private String name; // Bitcoin, Aselsan, US Dollarııı

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private InstrumentType type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Exchange exchange;

  @Column(nullable = false)
  private boolean active = true;

  protected Instrument() {
    // JPA için
  }

  public Instrument(String symbol, String name, InstrumentType type, Exchange exchange) {
    this.symbol = symbol;
    this.name = name;
    this.type = type;
    this.exchange = exchange;
  }

  // --- GETTERS ---

  public Long getId() {
    return id;
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

  public Exchange getExchange() {
    return exchange;
  }

  public boolean isActive() {
    return active;
  }
}
