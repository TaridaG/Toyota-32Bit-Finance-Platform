package com.company.marketdataservice.config;

import com.company.marketdataservice.fund.CompositeFundProvider;
import com.company.marketdataservice.fund.FundBatchTelemetry;
import com.company.marketdataservice.fund.FundProvider;
import com.company.marketdataservice.fund.TefasFundPriceProvider;
import com.company.marketdataservice.fund.TefasHttpProvider;
import com.company.marketdataservice.fund.TefasProvider;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class FundProviderConfiguration {

    @Bean
    public TefasProvider tefasMockFundProvider() {
        return new TefasProvider();
    }

    @Bean
    @Primary
    public FundProvider fundProvider(
            TefasFundPriceProvider tefasFundPriceProvider,
            TefasHttpProvider tefasHttpProvider,
            TefasProvider tefasMockFundProvider,
            MeterRegistry meterRegistry,
            FundBatchTelemetry fundBatchTelemetry
    ) {
        return new CompositeFundProvider(
                tefasFundPriceProvider,
                tefasHttpProvider,
                tefasMockFundProvider,
                meterRegistry,
                fundBatchTelemetry
        );
    }
}
