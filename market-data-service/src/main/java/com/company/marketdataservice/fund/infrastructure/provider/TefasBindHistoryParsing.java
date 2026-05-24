package com.company.marketdataservice.fund.infrastructure.provider;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * `fon (TEFAS NAV)` infrastructure katmanı adaptörü.
 */
final class TefasBindHistoryParsing {

    private TefasBindHistoryParsing() {
    }

    /**
     * Picks latest row by TARIH (epoch millis); falls back to last array element when comparable.
     */
    static Optional<ParsedLatest> latestNav(JsonNode root, String normalizedFundCodeFallback) {
        if (root == null || root.isNull()) {
            return Optional.empty();
        }
        JsonNode data = root.get("data");
        if (data == null || !data.isArray() || data.isEmpty()) {
            return Optional.empty();
        }

        JsonNode bestRow = null;
        long maxMillis = Long.MIN_VALUE;
        for (JsonNode row : data) {
            if (row == null || !row.isObject()) {
                continue;
            }
            long millis = tarihEpochMillis(row);
            if (millis != Long.MIN_VALUE && millis >= maxMillis) {
                maxMillis = millis;
                bestRow = row;
            }
        }
        if (bestRow == null) {
            JsonNode fallback = null;
            for (JsonNode row : data) {
                if (row != null && row.isObject()) {
                    fallback = row;
                }
            }
            if (fallback != null && tarihEpochMillis(fallback) == Long.MIN_VALUE) {
                bestRow = fallback;
            }
        }
        if (bestRow == null) {
            return Optional.empty();
        }

        JsonNode fk = bestRow.get("FONKODU");
        String code = normalizedFundCodeFallback;
        if (fk != null && !fk.isNull()) {
            String fromJson = fk.asText("").trim().toUpperCase(Locale.ROOT);
            if (!fromJson.isEmpty()) {
                code = fromJson;
            }
        }

        Optional<BigDecimal> nav = decimalField(bestRow.get("FIYAT"));
        if (nav.isEmpty()) {
            return Optional.empty();
        }

        long rowMillis = tarihEpochMillis(bestRow);
        Instant ts =
                rowMillis != Long.MIN_VALUE
                        ? Instant.ofEpochMilli(rowMillis)
                        : Instant.now();
        return Optional.of(new ParsedLatest(code, nav.get(), ts));
    }

    /**
     * All daily NAV points for the requested fund code. De-duplicates by {@code observedAt} (keeps last row per instant).
     */
    static List<ParsedLatest> allNavPointsSorted(JsonNode root, String normalizedFundCodeFallback) {
        if (root == null || root.isNull()) {
            return List.of();
        }
        JsonNode data = root.get("data");
        if (data == null || !data.isArray() || data.isEmpty()) {
            return List.of();
        }
        String want = normalizedFundCodeFallback == null ? "" : normalizedFundCodeFallback.trim().toUpperCase(Locale.ROOT);
        Map<Long, ParsedLatest> byMillis = new LinkedHashMap<>();
        for (JsonNode row : data) {
            if (row == null || !row.isObject()) {
                continue;
            }
            JsonNode fk = row.get("FONKODU");
            String code = want;
            if (fk != null && !fk.isNull()) {
                String fromJson = fk.asText("").trim().toUpperCase(Locale.ROOT);
                if (!fromJson.isEmpty()) {
                    code = fromJson;
                }
            }
            if (!want.isEmpty() && !want.equals(code)) {
                continue;
            }
            Optional<BigDecimal> nav = decimalField(row.get("FIYAT"));
            if (nav.isEmpty()) {
                continue;
            }
            long rowMillis = tarihEpochMillis(row);
            Instant ts = rowMillis != Long.MIN_VALUE ? Instant.ofEpochMilli(rowMillis) : Instant.now();
            long key = rowMillis != Long.MIN_VALUE ? rowMillis : ts.toEpochMilli();
            byMillis.put(key, new ParsedLatest(code, nav.get(), ts));
        }
        List<ParsedLatest> out = new ArrayList<>(byMillis.values());
        out.sort(Comparator.comparing(ParsedLatest::timestamp));
        return out;
    }

    private static long tarihEpochMillis(JsonNode row) {
        JsonNode t = row.get("TARIH");
        if (t == null || t.isNull()) {
            return Long.MIN_VALUE;
        }
        if (t.isNumber()) {
            return t.longValue();
        }
        try {
            return Long.parseLong(t.asText("").trim());
        } catch (Exception ex) {
            return Long.MIN_VALUE;
        }
    }

    static Optional<BigDecimal> decimalField(JsonNode n) {
        if (n == null || n.isNull()) {
            return Optional.empty();
        }
        try {
            if (n.isNumber()) {
                return Optional.of(n.decimalValue());
            }
            String s = n.asText("").trim().replace(',', '.');
            if (s.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new BigDecimal(s));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    /**
     * İş mantığı operasyonunu çalıştırır.
         * @param fundCode girdi parametresi
         * @param nav girdi parametresi
         * @param timestamp girdi parametresi
         */
    public record ParsedLatest(String fundCode, BigDecimal nav, Instant timestamp) {}
}
