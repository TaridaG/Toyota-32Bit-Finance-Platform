package com.company.marketdataservice.rates.infrastructure.persistence;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "mds_evds_tl_deposit_weekly",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_mds_tl_dep_week_maturity",
                columnNames = {"week_start", "maturity_code"}
        )
)
/**
 * `makro oran` verisi için JPA entity.
 */
@Getter
@Setter
public class TlDepositWeeklyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "maturity_code", nullable = false, length = 16)
    private String maturityCode;

    @Column(name = "rate_percent", nullable = false, precision = 12, scale = 4)
    private BigDecimal ratePercent;

    @Column(name = "source_observation_date")
    private LocalDate sourceObservationDate;

    @Column(name = "source_provider", nullable = false, length = 32)
    private String sourceProvider = "TCMB_EVDS";

    @Column(name = "ingest_time", nullable = false)
    private Instant ingestTime = Instant.now();
}
