package com.company.finance_api.web;

import com.company.finance_api.domain.enums.PriceType;

import java.util.Locale;

public final class PriceTypeParamResolver {

    private PriceTypeParamResolver() {
    }

    public static PriceType resolve(String raw) {
        if (raw == null || raw.isBlank()) {
            return PriceType.MARKET;
        }
        try {
            return PriceType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return PriceType.MARKET;
        }
    }
}
