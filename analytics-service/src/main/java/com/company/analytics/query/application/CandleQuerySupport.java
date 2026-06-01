package com.company.analytics.query.application;

import com.company.analytics.processing.domain.AnalyticsPriceCandle;
import com.company.analytics.processing.domain.AnalyticsPriceCandleDaily;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Sembol bazlı mum sorgularında aynı bucket için yinelenen satırları birleştirir. */
final class CandleQuerySupport {

    private CandleQuerySupport() {
    }

    static List<AnalyticsPriceCandle> dedupeIntervalCandlesByOpenTime(List<AnalyticsPriceCandle> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        Map<Instant, AnalyticsPriceCandle> best = new LinkedHashMap<>();
        for (AnalyticsPriceCandle row : rows) {
            if (row.getOpenTime() == null) {
                continue;
            }
            AnalyticsPriceCandle existing = best.get(row.getOpenTime());
            if (existing == null || prefersIntervalRow(row, existing)) {
                best.put(row.getOpenTime(), row);
            }
        }
        return new ArrayList<>(best.values());
    }

    static List<AnalyticsPriceCandleDaily> dedupeDailyCandlesByDate(List<AnalyticsPriceCandleDaily> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        Map<java.time.LocalDate, AnalyticsPriceCandleDaily> best = new LinkedHashMap<>();
        for (AnalyticsPriceCandleDaily row : rows) {
            if (row.getCandleDate() == null) {
                continue;
            }
            AnalyticsPriceCandleDaily existing = best.get(row.getCandleDate());
            if (existing == null || prefersDailyRow(row, existing)) {
                best.put(row.getCandleDate(), row);
            }
        }
        return new ArrayList<>(best.values());
    }

    private static boolean prefersIntervalRow(AnalyticsPriceCandle candidate, AnalyticsPriceCandle existing) {
        int tradeCount = Long.compare(
                candidate.getTradeCount() == null ? 0L : candidate.getTradeCount(),
                existing.getTradeCount() == null ? 0L : existing.getTradeCount()
        );
        if (tradeCount != 0) {
            return tradeCount > 0;
        }
        Instant candidateUpdated = candidate.getUpdatedAt();
        Instant existingUpdated = existing.getUpdatedAt();
        if (candidateUpdated == null || existingUpdated == null) {
            return true;
        }
        return candidateUpdated.isAfter(existingUpdated);
    }

    private static boolean prefersDailyRow(AnalyticsPriceCandleDaily candidate, AnalyticsPriceCandleDaily existing) {
        int tradeCount = Long.compare(
                candidate.getTradeCount() == null ? 0L : candidate.getTradeCount(),
                existing.getTradeCount() == null ? 0L : existing.getTradeCount()
        );
        if (tradeCount != 0) {
            return tradeCount > 0;
        }
        Instant candidateUpdated = candidate.getUpdatedAt();
        Instant existingUpdated = existing.getUpdatedAt();
        if (candidateUpdated == null || existingUpdated == null) {
            return true;
        }
        return candidateUpdated.isAfter(existingUpdated);
    }
}
