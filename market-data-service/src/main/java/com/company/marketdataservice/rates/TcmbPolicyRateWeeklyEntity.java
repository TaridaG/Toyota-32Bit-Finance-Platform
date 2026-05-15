package com.company.marketdataservice.rates;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "mds_tcmb_policy_rate_weekly",
        uniqueConstraints = @UniqueConstraint(name = "uk_mds_tcmb_policy_rate_week_week", columnNames = "week_start")
)
@Getter
@Setter
public class TcmbPolicyRateWeeklyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "rate_percent", nullable = false, precision = 12, scale = 4)
    private BigDecimal ratePercent;

    /** EVDS observation date that supplied {@link #ratePercent} for this ISO week (nullable for rows pre-migration). */
    @Column(name = "source_observation_date")
    private LocalDate sourceObservationDate;

    @Column(name = "source_provider", nullable = false, length = 32)
    private String sourceProvider = "TCMB_EVDS";

    @Column(name = "ingest_time", nullable = false)
    private Instant ingestTime = Instant.now();
}
