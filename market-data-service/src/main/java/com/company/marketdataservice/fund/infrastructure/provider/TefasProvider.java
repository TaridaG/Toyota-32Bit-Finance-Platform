package com.company.marketdataservice.fund.infrastructure.provider;
import com.company.marketdataservice.fund.domain.FundProvider;
import com.company.marketdataservice.fund.domain.FundSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * `fon (TEFAS NAV)` harici veri kaynağından fiyat veya snapshot fetch eden provider adaptörü.
 */
public class TefasProvider implements FundProvider {

    private static final String SRC = "TEFAS_MOCK";

    /**
     * Geliştirme/test fallback mock provider tanımlayıcısını ({@code TEFAS_MOCK}) döner.
     */
    @Override
    public String source() {
        return SRC;
    }

    /**
     * Gerçek TEFAS API'si kullanılamadığında deterministik sentetik NAV üretir.
     *
     * @param fundCodes sentetik NAV üretilecek fon kodları
     * @return her geçerli kod için mock {@link FundSnapshot} listesi
     */
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
