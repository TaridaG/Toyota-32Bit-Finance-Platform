package com.company.reporting.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReportingMetricsConfig {

    @Bean
    public Counter reportGeneratedCounter(MeterRegistry registry) {
        return Counter.builder("report.generated.total")
                .description("Total reports generated")
                .register(registry);
    }

}