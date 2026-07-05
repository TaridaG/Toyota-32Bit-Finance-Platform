package com.company.marketdataservice.viop.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * VIOP aktif sözleşme listesi için pazar segment filtresi.
 */
public enum ViopContractSegment {
    RATES("rates", List.of("D_FI")),
    BONDS("bonds", List.of("D_BO")),
    EQUITY("equity", List.of("D_EQ")),
    COMMODITY("commodity", List.of("D_CM", "D_EMTIA", "D_COM")),
    INDEX("index", List.of("D_IX", "D_IN")),
    RATES_BONDS("rates_bonds", List.of("D_FI", "D_BO")),
    ALL("all", List.of());

    private final String queryValue;
    private final List<String> pazarCodes;

    ViopContractSegment(String queryValue, List<String> pazarCodes) {
        this.queryValue = queryValue;
        this.pazarCodes = pazarCodes;
    }

    public String queryValue() {
        return queryValue;
    }

    public List<String> pazarCodes() {
        return pazarCodes;
    }

    public boolean filtersByPazar() {
        return !pazarCodes.isEmpty();
    }

    public static Optional<ViopContractSegment> fromQuery(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.of(RATES_BONDS);
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(s -> s.queryValue.equals(normalized)).findFirst();
    }
}
