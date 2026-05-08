package com.company.marketdataservice.fundamentals.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.company.marketdataservice.dto.AnnualFinancialStatementDto;
import com.company.marketdataservice.dto.InstrumentFundamentalsDto;
import com.company.marketdataservice.instrument.InstrumentCatalogEntry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class FmpInstrumentFundamentalsProvider implements InstrumentFundamentalsProvider {
    private static final String PROVIDER = "FMP";
    private final RestClient restClient = RestClient.create();

    @Value("${providers.fmp.enabled:false}")
    private boolean enabled;

    @Value("${providers.fmp.base-url:https://financialmodelingprep.com/api/v3}")
    private String baseUrl;

    @Value("${providers.fmp.api-key:}")
    private String apiKey;

    @Value("${market.fundamentals.max-annual-reports:5}")
    private int maxAnnualReports;

    @Override
    public String providerCode() {
        return PROVIDER;
    }

    @Override
    public boolean supports(InstrumentCatalogEntry instrument) {
        return enabled && "STOCK".equalsIgnoreCase(instrument.getAssetClass());
    }

    @Override
    public InstrumentFundamentalsDto fetch(InstrumentCatalogEntry instrument, String providerSymbol) {
        String symbol = normalize(providerSymbol);
        String key = apiKey == null ? "" : apiKey.trim();
        if (key.isBlank()) {
            throw new IllegalStateException("FMP API key is not configured");
        }
        List<String> candidateSymbols = new ArrayList<>();
        candidateSymbols.add(symbol);
        if (!symbol.contains(".") && symbol.length() <= 6) {
            candidateSymbols.add(symbol + ".IS");
        }
        for (String candidate : candidateSymbols) {
            try {
                InstrumentFundamentalsDto dto = fetchCandidate(instrument, candidate, key);
                if (dto != null) {
                    return dto;
                }
            } catch (Exception ignored) {
                // try next candidate
            }
        }
        throw new IllegalStateException("FMP fundamentals not found for symbol=" + symbol);
    }

    private InstrumentFundamentalsDto fetchCandidate(InstrumentCatalogEntry instrument, String symbol, String key) {
        JsonNode profileArray = getJson("/profile/" + symbol + "?apikey=" + key);
        if (profileArray == null || !profileArray.isArray() || profileArray.isEmpty()) {
            return null;
        }
        JsonNode profile = profileArray.get(0);
        JsonNode income = getJson("/income-statement/" + symbol + "?limit=" + Math.max(maxAnnualReports, 1) + "&apikey=" + key);
        JsonNode balance = getJson("/balance-sheet-statement/" + symbol + "?limit=" + Math.max(maxAnnualReports, 1) + "&apikey=" + key);
        JsonNode cashFlow = getJson("/cash-flow-statement/" + symbol + "?limit=" + Math.max(maxAnnualReports, 1) + "&apikey=" + key);
        return new InstrumentFundamentalsDto(
                instrument.getCanonicalSymbol(),
                PROVIDER,
                symbol,
                text(profile, "companyName"),
                text(profile, "country"),
                text(profile, "currency"),
                text(profile, "exchangeShortName"),
                text(profile, "ipoDate"),
                text(profile, "industry"),
                text(profile, "website"),
                decimal(profile, "mktCap"),
                decimal(profile, "sharesOutstanding"),
                decimal(profile, "pe"),
                null,
                Instant.now(),
                false,
                toAnnualStatements(income, balance, cashFlow)
        );
    }

    private List<AnnualFinancialStatementDto> toAnnualStatements(JsonNode income, JsonNode balance, JsonNode cashFlow) {
        int cap = Math.max(maxAnnualReports, 1);
        List<AnnualFinancialStatementDto> rows = new ArrayList<>();
        for (int i = 0; i < cap; i++) {
            JsonNode inc = arrayNodeAt(income, i);
            JsonNode bal = arrayNodeAt(balance, i);
            JsonNode cf = arrayNodeAt(cashFlow, i);
            if (inc == null && bal == null && cf == null) {
                continue;
            }
            Integer year = integer(inc, "calendarYear");
            if (year == null) {
                year = integer(bal, "calendarYear");
            }
            if (year == null) {
                year = integer(cf, "calendarYear");
            }
            rows.add(new AnnualFinancialStatementDto(
                    year,
                    decimal(inc, "revenue"),
                    decimal(inc, "netIncome"),
                    decimal(bal, "totalAssets"),
                    decimal(bal, "totalLiabilities"),
                    decimal(cf, "operatingCashFlow")
            ));
        }
        return rows;
    }

    private JsonNode arrayNodeAt(JsonNode node, int index) {
        if (node == null || !node.isArray() || index < 0 || index >= node.size()) {
            return null;
        }
        return node.get(index);
    }

    private JsonNode getJson(String pathAndQuery) {
        String root = baseUrl.replaceAll("/+$", "");
        return restClient.get()
                .uri(root + pathAndQuery)
                .retrieve()
                .body(JsonNode.class);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("providerSymbol is blank");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.path(field).isMissingNode() || node.path(field).isNull()) {
            return null;
        }
        String value = node.path(field).asText(null);
        return (value == null || value.isBlank()) ? null : value;
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.path(field).isMissingNode() || node.path(field).isNull()) {
            return null;
        }
        try {
            if (node.path(field).isNumber()) {
                return node.path(field).decimalValue();
            }
            String raw = node.path(field).asText();
            if (raw == null || raw.isBlank()) {
                return null;
            }
            return new BigDecimal(raw);
        } catch (Exception ex) {
            return null;
        }
    }

    private static Integer integer(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.path(field).isMissingNode() || node.path(field).isNull()) {
            return null;
        }
        if (node.path(field).isInt()) {
            return node.path(field).asInt();
        }
        try {
            String raw = node.path(field).asText(null);
            if (raw == null || raw.isBlank()) {
                return null;
            }
            return Integer.valueOf(raw);
        } catch (Exception ex) {
            return null;
        }
    }
}

