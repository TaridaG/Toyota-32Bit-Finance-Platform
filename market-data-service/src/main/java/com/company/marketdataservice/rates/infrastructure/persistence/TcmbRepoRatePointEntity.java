package com.company.marketdataservice.rates.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "mds_tcmb_repo_rate_point",
        uniqueConstraints = @UniqueConstraint(name = "uk_mds_tcmb_repo_rate_observation", columnNames = "observation_date")
)
@Getter
@Setter
public class TcmbRepoRatePointEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "observation_date", nullable = false)
    private LocalDate observationDate;

    @Column(name = "rate_percent", nullable = false, precision = 12, scale = 4)
    private BigDecimal ratePercent;

    @Column(name = "source_provider", nullable = false, length = 32)
    private String sourceProvider = "TCMB_EVDS";

    @Column(name = "ingest_time", nullable = false)
    private Instant ingestTime = Instant.now();
}
