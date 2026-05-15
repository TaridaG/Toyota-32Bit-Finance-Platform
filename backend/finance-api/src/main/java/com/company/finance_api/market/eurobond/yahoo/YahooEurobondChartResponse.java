package com.company.finance_api.market.eurobond.yahoo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YahooEurobondChartResponse(Chart chart) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Chart(List<Result> result, Error error) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Error(String code, String description) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(List<Long> timestamp, Indicators indicators) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Indicators(List<Quote> quote) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Quote(
            List<Double> open, List<Double> high, List<Double> low, List<Double> close) {}
}
