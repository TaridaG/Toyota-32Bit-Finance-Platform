package com.company.marketdataservice.fund;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TefasProvider implements FundProvider {

    private static final String SRC = "TEFAS_MOCK";

    @Override
    public String source() {
        return SRC;
    }

    @Override
    public List<FundSnapshot> fetchLatestNavs(List<String> fundCodes) {
        if (fundCodes == null || fundCodes.isEmpty()) {
            return List.of();
        }
        Instant ts = Instant.now();
        List<FundSnapshot> out = new ArrayList<>();
        for (String raw : fundCodes) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String code = raw.trim().toUpperCase(Locale.ROOT);
            BigDecimal nav = BigDecimal.valueOf(1.0 + (Math.abs(code.hashCode() % 10_000)) / 10_000.0);
            out.add(new FundSnapshot(null, code, nav, ts, SRC));
        }
        return out;
    }
}
