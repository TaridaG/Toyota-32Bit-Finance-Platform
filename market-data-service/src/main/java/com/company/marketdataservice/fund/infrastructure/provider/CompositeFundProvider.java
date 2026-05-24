package com.company.marketdataservice.fund.infrastructure.provider;
import com.company.marketdataservice.fund.domain.FundProvider;
import com.company.marketdataservice.fund.domain.FundSnapshot;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Fon NAV provider zincirini sırayla dener.
 */
@Slf4j
public class CompositeFundProvider implements FundProvider {

    private static final String SRC = "TEFAS";

    private final TefasFundPriceProvider tefasFundPriceProvider;
    private final TefasHttpProvider tefasHttpProvider;
    private final TefasProvider tefasMockFundProvider;
    private final MeterRegistry meterRegistry;
    private final FundBatchTelemetry fundBatchTelemetry;

    public CompositeFundProvider(
            TefasFundPriceProvider tefasFundPriceProvider,
            TefasHttpProvider tefasHttpProvider,
            TefasProvider tefasMockFundProvider,
            MeterRegistry meterRegistry,
            FundBatchTelemetry fundBatchTelemetry
    ) {
        this.tefasFundPriceProvider = tefasFundPriceProvider;
        this.tefasHttpProvider = tefasHttpProvider;
        this.tefasMockFundProvider = tefasMockFundProvider;
        this.meterRegistry = meterRegistry;
        this.fundBatchTelemetry = fundBatchTelemetry;
    }

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
        List<FundSnapshot> bindSnapshots = List.of();
        try {
            bindSnapshots = tefasFundPriceProvider.fetchLatestNavs(fundCodes);
        } catch (Exception ex) {
            log.warn("TEFAS_BIND_BATCH_FAILED reason={}", ex.toString(), ex);
        }
        if (bindSnapshots != null && !bindSnapshots.isEmpty()) {
            return bindSnapshots;
        }
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
