package com.company.marketdataservice.provider.finnhub;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

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
