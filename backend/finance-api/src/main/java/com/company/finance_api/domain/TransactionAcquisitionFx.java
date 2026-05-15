package com.company.finance_api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * TRY-hub FX mid snapshot at transaction acquisition time (audit / replay).
 */
@Entity
@Table(name = "transaction_acquisition_fx")
@Getter
@NoArgsConstructor
public class TransactionAcquisitionFx {

    @Id
    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(nullable = false)
    private Instant asOf;

    @Column(name = "usd_try", precision = 19, scale = 10)
    private BigDecimal usdTry;

    @Column(name = "eur_try", precision = 19, scale = 10)
    private BigDecimal eurTry;

    @Column(name = "gbp_try", precision = 19, scale = 10)
    private BigDecimal gbpTry;

    @Column(name = "jpy_try", precision = 19, scale = 10)
    private BigDecimal jpyTry;

    @Column(name = "aed_try", precision = 19, scale = 10)
    private BigDecimal aedTry;

    @Column(name = "eur_usd", precision = 19, scale = 10)
    private BigDecimal eurUsd;

    @Column(name = "gbp_usd", precision = 19, scale = 10)
    private BigDecimal gbpUsd;

    @Column(name = "jpy_usd", precision = 19, scale = 10)
    private BigDecimal jpyUsd;

    public TransactionAcquisitionFx(
            Long transactionId,
            Instant asOf,
            BigDecimal usdTry,
            BigDecimal eurTry,
            BigDecimal gbpTry,
            BigDecimal jpyTry,
            BigDecimal aedTry,
            BigDecimal eurUsd,
            BigDecimal gbpUsd,
            BigDecimal jpyUsd
    ) {
        this.transactionId = transactionId;
        this.asOf = asOf;
        this.usdTry = usdTry;
        this.eurTry = eurTry;
        this.gbpTry = gbpTry;
        this.jpyTry = jpyTry;
        this.aedTry = aedTry;
        this.eurUsd = eurUsd;
        this.gbpUsd = gbpUsd;
        this.jpyUsd = jpyUsd;
    }
}
