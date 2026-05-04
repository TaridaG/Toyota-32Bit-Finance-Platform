package com.company.marketdataservice.provider.yahoo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YahooFinanceResponse(
        Chart chart
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Chart(
            List<Result> result,
            Error error
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Error(
            String code,
            String description
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(
            Meta meta,
            List<Long> timestamp,
            Indicators indicators
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Meta(
            String symbol,
            Double regularMarketPrice,
            Long regularMarketTime
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Indicators(
            List<Quote> quote
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Quote(
            List<Double> open,
            List<Double> high,
            List<Double> low,
            List<Double> close
    ) {
    }
}
