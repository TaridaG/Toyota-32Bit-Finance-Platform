package com.company.marketdataservice.fund.infrastructure.provider;
import com.company.marketdataservice.fund.domain.FundProvider;
import com.company.marketdataservice.fund.domain.FundSnapshot;
import com.company.marketdataservice.bootstrap.config.FundMarketProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * `fon (TEFAS NAV)` harici veri kaynağından fiyat veya snapshot fetch eden provider adaptörü.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TefasFundPriceProvider implements FundProvider {

    private static final String SRC = "TEFAS";
    private static final ZoneId TURKEY = ZoneId.of("Europe/Istanbul");

    private final FundMarketProperties fundMarketProperties;
    private final TefasBindHistoryClient bindHistoryClient;

    /**
     * İş mantığı operasyonunu çalıştırır.
         */
    @Override
    public String source() {
        return SRC;
    }

    /**
     * Harici kaynaktan veri fetch eder.
         * @param fundCodes girdi parametresi
         * @return işlem sonucu
         */
    @Override
    public List<FundSnapshot> fetchLatestNavs(List<String> fundCodes) {
        if (fundCodes == null || fundCodes.isEmpty()) {
            return List.of();
        }
        List<FundSnapshot> out = new ArrayList<>();
        for (String raw : fundCodes) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String code = raw.trim().toUpperCase(Locale.ROOT);
            try {
                Optional<FundSnapshot> snap = fetchOne(code);
                if (snap.isEmpty()) {
                    continue;
                }
                out.add(snap.get());
            } catch (Exception ex) {
                log.warn(
                        "TEFAS_BIND_HISTORY_FAILED fundCode={} reason={}",
                        code,
                        ex.toString(),
                        ex
                );
            }
        }
        return out;
    }

    private Optional<FundSnapshot> fetchOne(String normalizedCode) {
        if (!StringUtils.hasText(fundMarketProperties.getTefasFonGnlBlgUrl())
                && !StringUtils.hasText(fundMarketProperties.getTefasBindHistoryUrl())) {
            return Optional.empty();
        }
        LocalDate overallEnd = LocalDate.now(TURKEY);
        LocalDate overallStart = overallEnd.minusDays(fundMarketProperties.getTefasHistoryLookbackDays());
        JsonNode merged = bindHistoryClient.fetchMergedBindHistory(normalizedCode, overallStart, overallEnd);
        Optional<TefasBindHistoryParsing.ParsedLatest> parsed =
                TefasBindHistoryParsing.latestNav(merged, normalizedCode);
        if (parsed.isEmpty()) {
            return Optional.empty();
        }
        TefasBindHistoryParsing.ParsedLatest p = parsed.get();
        return Optional.of(new FundSnapshot(null, p.fundCode(), p.nav(), p.timestamp(), SRC));
    }
}
