package com.company.marketdataservice.rates;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "mds_cpi_monthly",
        uniqueConstraints = @UniqueConstraint(name = "uq_mds_cpi_monthly_metric_month", columnNames = {"metric", "month_start"})
)
public class MdsCpiMonthlyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CpiMetric metric;

    @Column(name = "month_start", nullable = false)
    private LocalDate monthStart;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal value;

    @Column(name = "source_provider", nullable = false, length = 32)
    private String sourceProvider = "TCMB_EVDS";

    @Column(name = "ingest_time", nullable = false)
    private Instant ingestTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CpiMetric getMetric() {
        return metric;
    }

    public void setMetric(CpiMetric metric) {
        this.metric = metric;
    }

    public LocalDate getMonthStart() {
        return monthStart;
    }

    public void setMonthStart(LocalDate monthStart) {
        this.monthStart = monthStart;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public String getSourceProvider() {
        return sourceProvider;
    }

    public void setSourceProvider(String sourceProvider) {
        this.sourceProvider = sourceProvider;
    }

    public Instant getIngestTime() {
        return ingestTime;
    }

    public void setIngestTime(Instant ingestTime) {
        this.ingestTime = ingestTime;
    }
}
