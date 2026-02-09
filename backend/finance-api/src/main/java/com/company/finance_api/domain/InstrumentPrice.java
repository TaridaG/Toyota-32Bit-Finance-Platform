package com.company.finance_api.domain;

import com.company.finance_api.domain.enums.PriceType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "instrument_prices",
        indexes = {
                @Index(name = "idx_price_instrument_time", columnList = "instrument_id, timestamp"),
                @Index(name = "idx_price_timestamp", columnList = "timestamp")
        }
)
@Builder
@AllArgsConstructor // Lombok'un Builder için ihtiyaç duyduğu tüm parametreli constructor
public class InstrumentPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PriceType priceType;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal price;

    @Column(nullable = false)
    private Instant timestamp;

    protected InstrumentPrice() {}

    public InstrumentPrice(
            Instrument instrument,
            PriceType priceType,
            BigDecimal price,
            Instant timestamp
    ) {
        this.instrument = instrument;
        this.priceType = priceType;
        this.price = price;
        this.timestamp = timestamp;
    }

    // GETTERS
    public Long getId() { return id; }
    public Instrument getInstrument() { return instrument; }
    public PriceType getPriceType() { return priceType; }
    public BigDecimal getPrice() { return price; }
    public Instant getTimestamp() { return timestamp; }
}
