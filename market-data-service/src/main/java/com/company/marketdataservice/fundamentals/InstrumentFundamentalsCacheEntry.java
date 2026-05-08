package com.company.marketdataservice.fundamentals;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "mds_instrument_fundamentals_cache")
@Getter
@Setter
public class InstrumentFundamentalsCacheEntry {

    @Id
    @Column(name = "instrument_id", nullable = false)
    private Long instrumentId;

    @Column(name = "canonical_symbol", nullable = false, length = 64)
    private String canonicalSymbol;

    @Column(name = "provider", nullable = false, length = 64)
    private String provider;

    @Column(name = "provider_symbol", nullable = false, length = 128)
    private String providerSymbol;

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public InstrumentFundamentalsCacheEntry() {
    }
}
