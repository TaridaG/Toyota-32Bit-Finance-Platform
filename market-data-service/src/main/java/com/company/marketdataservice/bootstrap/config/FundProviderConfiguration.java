package com.company.marketdataservice.bootstrap.config;
import com.company.marketdataservice.fund.infrastructure.provider.CompositeFundProvider;
import com.company.marketdataservice.fund.infrastructure.provider.FundBatchTelemetry;
import com.company.marketdataservice.fund.domain.FundProvider;
import com.company.marketdataservice.fund.infrastructure.provider.TefasFundPriceProvider;
import com.company.marketdataservice.fund.infrastructure.provider.TefasHttpProvider;
import com.company.marketdataservice.fund.infrastructure.provider.TefasProvider;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * `uygulama bootstrap` için Spring `@Configuration` bean tanımları.
 */
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
