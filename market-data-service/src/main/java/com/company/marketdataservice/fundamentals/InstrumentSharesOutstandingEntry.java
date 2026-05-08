package com.company.marketdataservice.fundamentals;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "mds_instrument_shares_outstanding")
@Getter
@Setter
public class InstrumentSharesOutstandingEntry {
    @Id
    @Column(name = "instrument_id", nullable = false)
    private Long instrumentId;

    @Column(name = "canonical_symbol", nullable = false, length = 64, unique = true)
    private String canonicalSymbol;

    @Column(name = "shares_outstanding", nullable = false, precision = 28, scale = 4)
    private BigDecimal sharesOutstanding;

    @Column(name = "source", nullable = false, length = 64)
    private String source;

    @Column(name = "provider_symbol", length = 128)
    private String providerSymbol;

    @Column(name = "verified_at", nullable = false)
    private Instant verifiedAt;

    public InstrumentSharesOutstandingEntry() {
    }
}

