package com.company.marketdataservice.instrument;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "mds_provider_instrument_mapping",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_mds_mapping_provider_symbol",
                columnNames = {"provider", "provider_symbol"}
        )
)
@Getter
@Setter
public class ProviderInstrumentMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String provider;

    @Column(name = "provider_symbol", nullable = false, length = 128)
    private String providerSymbol;

    @Column(name = "instrument_id", nullable = false)
    private Long instrumentId;

    @Column(nullable = false)
    private int priority;

    @Column(nullable = false)
    private boolean active = true;

    protected ProviderInstrumentMapping() {
    }
}
