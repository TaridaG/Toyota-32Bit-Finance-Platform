package com.company.finance_api.portfolio.external.domain;

import com.company.finance_api.domain.Instrument;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "external_position_lots")
public class ExternalPositionLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private ExternalPortfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 8)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 8)
    private BigDecimal unitPrice;

    @Column(name = "fee_amount", precision = 19, scale = 8)
    private BigDecimal feeAmount;

    @Column(name = "fee_currency", length = 3)
    private String feeCurrency;

    @Column(name = "acquired_at", nullable = false)
    private LocalDateTime acquiredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private ExternalPositionSourceType sourceType;

    @Column(name = "source_name", length = 120)
    private String sourceName;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ExternalPositionLot(
            ExternalPortfolio portfolio,
            Instrument instrument,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal feeAmount,
            String feeCurrency,
            LocalDateTime acquiredAt,
            ExternalPositionSourceType sourceType,
            String sourceName,
            String notes
    ) {
        this.portfolio = portfolio;
        this.instrument = instrument;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.feeAmount = feeAmount;
        this.feeCurrency = feeCurrency;
        this.acquiredAt = acquiredAt;
        this.sourceType = sourceType;
        this.sourceName = sourceName;
        this.notes = notes;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void softDelete() {
        this.deleted = true;
    }

    public BigDecimal totalCost() {
        BigDecimal gross = unitPrice.multiply(quantity);
        BigDecimal fee = feeAmount == null ? BigDecimal.ZERO : feeAmount;
        return gross.add(fee);
    }
}

