package com.company.marketdataservice.fundamentals.infrastructure.provider;
import com.fasterxml.jackson.databind.JsonNode;
import com.company.marketdataservice.bootstrap.config.FinnhubProperties;
import com.company.marketdataservice.fundamentals.infrastructure.http.dto.AnnualFinancialStatementDto;
import com.company.marketdataservice.fundamentals.infrastructure.http.dto.InstrumentFundamentalsDto;
import com.company.marketdataservice.catalog.infrastructure.persistence.InstrumentCatalogEntry;
import com.company.marketdataservice.spot.infrastructure.provider.finnhub.FinnhubClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * `temel veri (fundamentals)` harici veri kaynağından fiyat veya snapshot fetch eden provider adaptörü.
 */
@Component
public class FinnhubInstrumentFundamentalsProvider implements InstrumentFundamentalsProvider {
    private static final String PROVIDER = "FINNHUB";
    private final FinnhubClient finnhubClient;
    private final FinnhubProperties finnhubProperties;

    @Value("${market.fundamentals.max-annual-reports:5}")
    private int maxAnnualReports;

    public FinnhubInstrumentFundamentalsProvider(FinnhubClient finnhubClient, FinnhubProperties finnhubProperties) {
        this.finnhubClient = finnhubClient;
        this.finnhubProperties = finnhubProperties;
    }

    /**
     * İş mantığı operasyonunu çalıştırır.
         */
    @Override
    public String providerCode() {
        return PROVIDER;
    }

    /**
     * İş mantığı operasyonunu çalıştırır.
         * @param instrument girdi parametresi
         */
    @Override
    public boolean supports(InstrumentCatalogEntry instrument) {
        return finnhubProperties.isEnabled()
                && instrument != null
                && "STOCK".equalsIgnoreCase(instrument.getAssetClass())
                && finnhubProperties.ownsSymbol(instrument.getCanonicalSymbol());
    }

    /**
     * Harici kaynaktan veri fetch eder.
         * @param instrument girdi parametresi
         * @param providerSymbol girdi parametresi
         * @return işlem sonucu
         */
    @Override
    public InstrumentFundamentalsDto fetch(InstrumentCatalogEntry instrument, String providerSymbol) {
        String normalized = normalize(providerSymbol);
        List<String> candidates = new ArrayList<>();
        candidates.add(normalized);
        if (!normalized.contains(".") && normalized.length() <= 6) {
            candidates.add(normalized + ".IS");
        }
        for (String candidate : candidates) {
            try {
                InstrumentFundamentalsDto dto = fetchSingle(instrument, candidate);
                if (dto.companyName() != null || dto.marketCapitalization() != null || !dto.annualStatements().isEmpty()) {
                    return dto;
                }
            } catch (Exception ignored) {
                // try next candidate
            }
        }
        throw new IllegalStateException("Finnhub returned empty fundamentals payload for symbol=" + normalized);
    }

    private InstrumentFundamentalsDto fetchSingle(InstrumentCatalogEntry instrument, String symbol) {
        JsonNode profile = finnhubClient.fetchCompanyProfile(symbol);
        JsonNode metric = finnhubClient.fetchBasicFinancials(symbol);
        JsonNode financialsReported = finnhubClient.fetchFinancialsReported(symbol);
        JsonNode metricNode = metric == null ? null : metric.path("metric");
        return new InstrumentFundamentalsDto(
                instrument.getCanonicalSymbol(),
                PROVIDER,
                symbol,
                text(profile, "name"),
                text(profile, "country"),
                text(profile, "currency"),
                text(profile, "exchange"),
                text(profile, "ipo"),
                text(profile, "finnhubIndustry"),
                text(profile, "weburl"),
                decimal(profile, "marketCapitalization"),
                decimal(profile, "shareOutstanding"),
                decimal(metricNode, "peTTM"),
                decimal(metricNode, "epsTTM"),
                Instant.now(),
                false,
                extractAnnualStatements(financialsReported, maxAnnualReports)
        );
    }

    private static List<AnnualFinancialStatementDto> extractAnnualStatements(JsonNode financialsReported, int maxItems) {
        if (financialsReported == null || !financialsReported.path("data").isArray()) {
            return List.of();
        }
        List<AnnualFinancialStatementDto> out = new ArrayList<>();
        for (JsonNode item : financialsReported.path("data")) {
            Integer year = integer(item, "year");
            JsonNode report = item.path("report");
            BigDecimal revenue = firstConcept(report.path("ic"),
                    "us-gaap_Revenues",
                    "us-gaap_RevenueFromContractWithCustomerExcludingAssessedTax",
                    "us-gaap_SalesRevenueNet");
            BigDecimal netIncome = firstConcept(report.path("ic"), "us-gaap_NetIncomeLoss");
            BigDecimal totalAssets = firstConcept(report.path("bs"), "us-gaap_Assets");
            BigDecimal totalLiabilities = firstConcept(report.path("bs"), "us-gaap_Liabilities");
            BigDecimal operatingCashFlow = firstConcept(report.path("cf"), "us-gaap_NetCashProvidedByUsedInOperatingActivities");
            out.add(new AnnualFinancialStatementDto(year, revenue, netIncome, totalAssets, totalLiabilities, operatingCashFlow));
            if (out.size() >= Math.max(maxItems, 1)) {
                break;
            }
        }
        return out;
    }

    private static BigDecimal firstConcept(JsonNode items, String... concepts) {
        if (items == null || !items.isArray()) {
            return null;
        }
        for (String concept : concepts) {
            for (JsonNode node : items) {
                String nodeConcept = text(node, "concept");
                if (nodeConcept == null || !nodeConcept.equalsIgnoreCase(concept)) {
                    continue;
                }
                BigDecimal val = decimal(node, "value");
                if (val != null) {
                    return val;
                }
            }
        }
        return null;
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
            return Integer.valueOf(node.path(field).asText());
        } catch (Exception ex) {
            return null;
        }
    }
}

