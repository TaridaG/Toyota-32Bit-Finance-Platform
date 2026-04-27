package com.company.analytics.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AnalyticsProcessingRouter {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsProcessingRouter.class);
    private final Set<String> unknownPriceTypeWarned = ConcurrentHashMap.newKeySet();

    public AnalyticsProcessingDecision decide(String rawPriceType) {
        String normalized = normalizePriceType(rawPriceType);
        return switch (normalized) {
            case "MARKET" -> new AnalyticsProcessingDecision(true, true, true, true);
            case "FX_MID" -> new AnalyticsProcessingDecision(true, true, false, false);
            case "FUND_NAV" -> new AnalyticsProcessingDecision(true, false, false, false);
            default -> {
                if (unknownPriceTypeWarned.add(rawPriceType == null ? "null" : rawPriceType)) {
                    log.warn("ANALYTICS_UNKNOWN_PRICE_TYPE priceType={}", rawPriceType);
                }
                yield new AnalyticsProcessingDecision(true, false, false, false);
            }
        };
    }

    public String normalizePriceType(String rawPriceType) {
        if (rawPriceType == null || rawPriceType.isBlank()) {
            return "UNKNOWN";
        }
        return switch (rawPriceType.trim().toUpperCase()) {
            case "MARKET" -> "MARKET";
            case "FX_MID" -> "FX_MID";
            case "FUND_NAV" -> "FUND_NAV";
            default -> "UNKNOWN";
        };
    }
}
