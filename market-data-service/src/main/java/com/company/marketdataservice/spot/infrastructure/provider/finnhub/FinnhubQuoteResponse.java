package com.company.marketdataservice.spot.infrastructure.provider.finnhub;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * `spot fiyat` infrastructure katmanı adaptörü.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FinnhubQuoteResponse(
        Double c,
        Double d,
        Double dp,
        Double h,
        Double l,
        Double o,
        Double pc,
        Long t
) {
}
