package com.company.marketdataservice.spot.infrastructure.provider.finnhub;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * `spot fiyat` infrastructure katmanı adaptörü.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FinnhubCandleResponse(
        String s,
        List<Double> c,
        List<Double> h,
        List<Double> l,
        List<Double> o,
        List<Long> t,
        List<Double> v
) {
}
