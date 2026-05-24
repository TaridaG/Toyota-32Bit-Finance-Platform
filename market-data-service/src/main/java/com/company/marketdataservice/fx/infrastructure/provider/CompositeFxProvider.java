package com.company.marketdataservice.fx.infrastructure.provider;
import com.company.marketdataservice.fx.domain.FxProvider;
import com.company.marketdataservice.fx.domain.FxSnapshot;
import com.company.marketdataservice.bootstrap.config.FxMarketProperties;
import com.company.marketdataservice.fx.infrastructure.provider.exchangerate.ExchangeRateApiFxProvider;
import com.company.marketdataservice.fx.infrastructure.provider.stooq.StooqMetalSpotFxProvider;
import com.company.marketdataservice.fx.infrastructure.provider.tcmb.TcmbFxProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * FX provider zincirini sırayla dener; ilk başarılı snapshot'ı döner.
 */
@Slf4j
@Component
@Primary
public class CompositeFxProvider implements FxProvider {

    private final TcmbFxProvider tcmbFxProvider;
    private final ExchangeRateApiFxProvider exchangeRateApiFxProvider;
    private final StooqMetalSpotFxProvider stooqMetalSpotFxProvider;
    private final FxMarketProperties fxMarketProperties;

    public CompositeFxProvider(
            TcmbFxProvider tcmbFxProvider,
            ExchangeRateApiFxProvider exchangeRateApiFxProvider,
            StooqMetalSpotFxProvider stooqMetalSpotFxProvider,
            FxMarketProperties fxMarketProperties
    ) {
        this.tcmbFxProvider = tcmbFxProvider;
        this.exchangeRateApiFxProvider = exchangeRateApiFxProvider;
        this.stooqMetalSpotFxProvider = stooqMetalSpotFxProvider;
        this.fxMarketProperties = fxMarketProperties;
    }

    /**
     * İş mantığı operasyonunu çalıştırır.
         */
    @Override
    public String source() {
        return "COMPOSITE_FX";
    }

    /**
     * Harici kaynaktan veri fetch eder.
         * @return işlem sonucu
         */
    @Override
    public List<FxSnapshot> fetchLatestRates() {
        List<FxSnapshot> baseRates = List.of();
        for (String name : fxMarketProperties.getProviderOrder()) {
            if (name == null || name.isBlank()) {
                continue;
            }
            String key = name.trim().toUpperCase(Locale.ROOT);
            if ("TCMB".equals(key)) {
                try {
                    List<FxSnapshot> tcmb = tcmbFxProvider.fetchLatestRates();
                    if (tcmb != null && !tcmb.isEmpty()) {
                        baseRates = tcmb;
                        break;
                    }
                    log.warn("FX_COMPOSITE_TCMB_EMPTY trying_next");
                } catch (Exception ex) {
                    log.warn("FX_COMPOSITE_TCMB_FAILED reason={} trying_next", ex.getMessage());
                }
            } else if ("EXCHANGE_API".equals(key)) {
                try {
                    List<FxSnapshot> api = exchangeRateApiFxProvider.fetchLatestRates();
                    if (api != null && !api.isEmpty()) {
                        log.info("FX_COMPOSITE_USING_FALLBACK source={}", exchangeRateApiFxProvider.source());
                        baseRates = api;
                        break;
                    }
                    log.warn("FX_COMPOSITE_EXCHANGE_API_EMPTY");
                } catch (Exception ex) {
                    log.warn("FX_COMPOSITE_EXCHANGE_API_FAILED reason={}", ex.getMessage());
                }
            }
        }
        List<FxSnapshot> metals;
        try {
            metals = stooqMetalSpotFxProvider.fetchLatestRates();
        } catch (Exception ex) {
            log.warn("FX_COMPOSITE_STOOQ_SPOT_FAILED reason={}", ex.getMessage());
            metals = List.of();
        }
        if (baseRates.isEmpty() && metals.isEmpty()) {
            return List.of();
        }
        if (metals.isEmpty()) {
            return baseRates;
        }
        List<FxSnapshot> merged = new ArrayList<>(baseRates);
        merged.addAll(metals);
        return merged;
    }
}
