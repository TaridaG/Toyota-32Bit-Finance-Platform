package com.company.marketdataservice.fx;

import com.company.marketdataservice.config.FxMarketProperties;
import com.company.marketdataservice.fx.exchangerate.ExchangeRateApiFxProvider;
import com.company.marketdataservice.fx.tcmb.TcmbFxProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Slf4j
@Component
@Primary
public class CompositeFxProvider implements FxProvider {

    private final TcmbFxProvider tcmbFxProvider;
    private final ExchangeRateApiFxProvider exchangeRateApiFxProvider;
    private final FxMarketProperties fxMarketProperties;

    public CompositeFxProvider(
            TcmbFxProvider tcmbFxProvider,
            ExchangeRateApiFxProvider exchangeRateApiFxProvider,
            FxMarketProperties fxMarketProperties
    ) {
        this.tcmbFxProvider = tcmbFxProvider;
        this.exchangeRateApiFxProvider = exchangeRateApiFxProvider;
        this.fxMarketProperties = fxMarketProperties;
    }

    @Override
    public String source() {
        return "COMPOSITE_FX";
    }

    @Override
    public List<FxSnapshot> fetchLatestRates() {
        for (String name : fxMarketProperties.getProviderOrder()) {
            if (name == null || name.isBlank()) {
                continue;
            }
            String key = name.trim().toUpperCase(Locale.ROOT);
            if ("TCMB".equals(key)) {
                try {
                    List<FxSnapshot> tcmb = tcmbFxProvider.fetchLatestRates();
                    if (tcmb != null && !tcmb.isEmpty()) {
                        return tcmb;
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
                        return api;
                    }
                    log.warn("FX_COMPOSITE_EXCHANGE_API_EMPTY");
                } catch (Exception ex) {
                    log.warn("FX_COMPOSITE_EXCHANGE_API_FAILED reason={}", ex.getMessage());
                }
            }
        }
        return List.of();
    }
}
