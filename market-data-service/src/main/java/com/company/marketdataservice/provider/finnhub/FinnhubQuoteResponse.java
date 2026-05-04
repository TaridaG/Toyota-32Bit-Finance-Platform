package com.company.marketdataservice.provider.finnhub;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
