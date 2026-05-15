package com.company.marketdataservice.fund;

import com.company.marketdataservice.historical.HistoricalFundPoint;
import com.company.marketdataservice.historical.HistoricalFundProvider;
import com.company.marketdataservice.instrument.InstrumentMappingService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Historical NAV series from TEFAS ({@code fonGnlBlgSiraliGetir} JSON API, legacy BindHistory fallback) for
 * orchestrated {@code FUND} backfill.
 */
@Component
@RequiredArgsConstructor
public class TefasHistoricalFundProvider implements HistoricalFundProvider {

    private static final String PROVIDER = "TEFAS";

    private final TefasBindHistoryClient bindHistoryClient;
    private final InstrumentMappingService instrumentMappingService;

    @Override
    public List<HistoricalFundPoint> fetchRange(String symbol, LocalDate startDate, LocalDate endDate) {
        if (symbol == null || symbol.isBlank() || startDate == null || endDate == null) {
            return List.of();
        }
        String code = symbol.trim().toUpperCase(Locale.ROOT);
        JsonNode merged = bindHistoryClient.fetchMergedBindHistory(code, startDate, endDate);
        List<TefasBindHistoryParsing.ParsedLatest> parsed =
                TefasBindHistoryParsing.allNavPointsSorted(merged, code);
        Long instrumentId = instrumentMappingService.resolveInstrument(PROVIDER, code).orElse(null);
        List<HistoricalFundPoint> out = new ArrayList<>(parsed.size());
        for (TefasBindHistoryParsing.ParsedLatest p : parsed) {
            if (p.nav() == null || p.nav().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            out.add(new HistoricalFundPoint(p.fundCode(), instrumentId, p.nav(), p.timestamp(), PROVIDER));
        }
        return out;
    }
}
