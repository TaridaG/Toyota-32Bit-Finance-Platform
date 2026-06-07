package com.company.marketdataservice.catalog.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.IdClass;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "mds_ingest_config")
@IdClass(IngestConfigEntry.IngestConfigId.class)
@Getter
@Setter
public class IngestConfigEntry {

    @Id
    @Column(name = "instrument_id", nullable = false)
    private Long instrumentId;

    @Id
    @Column(name = "segment", nullable = false, length = 32)
    private String segment;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "provider_override", length = 64)
    private String providerOverride;

    @Column(name = "provider_symbol_override", length = 128)
    private String providerSymbolOverride;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "last_error_at")
    private Instant lastErrorAt;

    public IngestConfigEntry() {
    }

    /**
     * {@link IngestConfigEntry} için bileşik JPA anahtarı ({@code instrumentId} + {@code segment}).
     */
    @Getter
    @Setter
    public static class IngestConfigId implements Serializable {
        private Long instrumentId;
        private String segment;

        public IngestConfigId() {
        }

        public IngestConfigId(Long instrumentId, String segment) {
            this.instrumentId = instrumentId;
            this.segment = segment;
        }
    }
}

