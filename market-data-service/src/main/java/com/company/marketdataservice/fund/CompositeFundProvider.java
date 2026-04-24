package com.company.marketdataservice.fund;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

import java.util.List;

public class CompositeFundProvider implements FundProvider {

    private static final String SRC = "TEFAS";

    private final TefasHttpProvider tefasHttpProvider;
    private final TefasProvider tefasMockFundProvider;
    private final MeterRegistry meterRegistry;
    private final FundBatchTelemetry fundBatchTelemetry;

    public CompositeFundProvider(
            TefasHttpProvider tefasHttpProvider,
            TefasProvider tefasMockFundProvider,
            MeterRegistry meterRegistry,
            FundBatchTelemetry fundBatchTelemetry
    ) {
        this.tefasHttpProvider = tefasHttpProvider;
        this.tefasMockFundProvider = tefasMockFundProvider;
        this.meterRegistry = meterRegistry;
        this.fundBatchTelemetry = fundBatchTelemetry;
    }

    @Override
    public String source() {
        return SRC;
    }

    @Override
    public List<FundSnapshot> fetchLatestNavs(List<String> fundCodes) {
        boolean httpConfigured = tefasHttpProvider.isConfigured();
        if (!httpConfigured) {
            return tefasMockFundProvider.fetchLatestNavs(fundCodes);
        }
        List<FundSnapshot> httpSnapshots = List.of();
        try {
            httpSnapshots = tefasHttpProvider.fetchLatestNavs(fundCodes);
        } catch (Exception ex) {
            meterRegistry.counter(
                    "fund_fetch_fallback_total",
                    Tags.of("service", "market-data-service", "reason", "http_exception")
            ).increment();
            fundBatchTelemetry.markHttpFallback();
            return tefasMockFundProvider.fetchLatestNavs(fundCodes);
        }
        if (httpSnapshots == null || httpSnapshots.isEmpty()) {
            meterRegistry.counter(
                    "fund_fetch_fallback_total",
                    Tags.of("service", "market-data-service", "reason", "http_empty")
            ).increment();
            fundBatchTelemetry.markHttpFallback();
            return tefasMockFundProvider.fetchLatestNavs(fundCodes);
        }
        return httpSnapshots;
    }
}
