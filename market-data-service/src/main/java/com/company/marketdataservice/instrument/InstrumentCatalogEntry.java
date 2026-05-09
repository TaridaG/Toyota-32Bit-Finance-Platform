package com.company.marketdataservice.instrument;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "mds_instrument_catalog")
@Getter
@Setter
public class InstrumentCatalogEntry {

    @Id
    @Column(name = "instrument_id", nullable = false)
    private Long instrumentId;

    @Column(name = "canonical_symbol", nullable = false, unique = true, length = 64)
    private String canonicalSymbol;

    @Column(name = "asset_class", nullable = false, length = 64)
    private String assetClass;

    @Column(name = "base_currency", length = 16)
    private String baseCurrency;

    @Column(name = "quote_currency", length = 16)
    private String quoteCurrency;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected InstrumentCatalogEntry() {
    }

    /** Catalog row backed by {@code public.instruments} when MDS catalog was not seeded yet. */
    public static InstrumentCatalogEntry fxFromFinance(long instrumentId, String canonicalSymbol, String baseCurrency, String quoteCurrency) {
        InstrumentCatalogEntry e = new InstrumentCatalogEntry();
        e.setInstrumentId(instrumentId);
        e.setCanonicalSymbol(canonicalSymbol);
        e.setAssetClass("FX");
        e.setBaseCurrency(baseCurrency);
        e.setQuoteCurrency(quoteCurrency);
        e.setActive(true);
        return e;
    }
}
