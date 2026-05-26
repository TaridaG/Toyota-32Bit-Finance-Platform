package com.company.marketdataservice.rates.infrastructure.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "mds_evds_tl_deposit_daily_index",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_mds_tl_dep_daily_idx_day_maturity",
            columnNames = {"day", "maturity_code"}))
@Getter
@Setter
public class TlDepositDailyIndexEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "day", nullable = false)
  private LocalDate day;

  @Column(name = "maturity_code", nullable = false, length = 16)
  private String maturityCode;

  @Column(name = "index_value", nullable = false, precision = 19, scale = 10)
  private BigDecimal indexValue;

  @Column(name = "annual_rate_percent", nullable = false, precision = 12, scale = 4)
  private BigDecimal annualRatePercent;

  @Column(name = "source_observation_date")
  private LocalDate sourceObservationDate;

  @Column(name = "source_provider", nullable = false, length = 32)
  private String sourceProvider = "TCMB_EVDS";

  @Column(name = "computed_at", nullable = false)
  private Instant computedAt = Instant.now();
}
