package com.company.marketdataservice.fund;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

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

    record ParsedLatest(String fundCode, BigDecimal nav, Instant timestamp) {}
}
