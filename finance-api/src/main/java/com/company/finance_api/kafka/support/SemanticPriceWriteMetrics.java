package com.company.finance_api.kafka.support;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Kafka fiyat yazımı için Micrometer counter metriklerini kaydeder. */
@Component
@RequiredArgsConstructor
public class SemanticPriceWriteMetrics {

  private static final int SOURCE_TAG_MAX = 64;

  private final MeterRegistry meterRegistry;

  public void record(String semantic, String source) {
    String src = source == null || source.isBlank() ? "unknown" : source.trim();
    if (src.length() > SOURCE_TAG_MAX) {
      src = src.substring(0, SOURCE_TAG_MAX);
    }
    meterRegistry
        .counter(
            "semantic_price_write_total",
            Tags.of("service", "finance-api", "semantic", semantic, "source", src))
        .increment();
  }
}
