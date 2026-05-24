package com.company.marketdataservice.spot.infrastructure.provider.yahoo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * `spot fiyat` infrastructure katmanı adaptörü.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record YahooFinanceResponse(
        Chart chart
) {
    /**
     * İş mantığı operasyonunu çalıştırır.
         * @param result girdi parametresi
         * @param error girdi parametresi
         */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Chart(
            List<Result> result,
            Error error
    ) {
    }

    /**
     * İş mantığı operasyonunu çalıştırır.
         * @param code girdi parametresi
         * @param description girdi parametresi
         */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Error(
            String code,
            String description
    ) {
    }

    /**
     * İş mantığı operasyonunu çalıştırır.
         * @param meta girdi parametresi
         * @param timestamp girdi parametresi
         * @param indicators girdi parametresi
         */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(
            Meta meta,
            List<Long> timestamp,
            Indicators indicators
    ) {
    }

    /**
     * İş mantığı operasyonunu çalıştırır.
         * @param symbol enstrüman sembolü
         * @param regularMarketPrice girdi parametresi
         * @param regularMarketTime girdi parametresi
         * @param regularMarketVolume girdi parametresi
         * @param regularMarketOpen girdi parametresi
         * @param regularMarketDayHigh girdi parametresi
         * @param regularMarketDayLow girdi parametresi
         * @param openInterest girdi parametresi
         * @param exchangeName girdi parametresi
         * @param fullExchangeName girdi parametresi
         * @param shortName girdi parametresi
         * @param underlyingSymbol girdi parametresi
         * @param expireDate girdi parametresi
         */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Meta(
            String symbol,
            Double regularMarketPrice,
            Long regularMarketTime,
            Long regularMarketVolume,
            Double regularMarketOpen,
            Double regularMarketDayHigh,
            Double regularMarketDayLow,
            Double openInterest,
            String exchangeName,
            String fullExchangeName,
            String shortName,
            String underlyingSymbol,
            Long expireDate
    ) {
    }

    /**
     * İş mantığı operasyonunu çalıştırır.
         * @param quote girdi parametresi
         */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Indicators(
            List<Quote> quote
    ) {
    }

    /**
     * İş mantığı operasyonunu çalıştırır.
         * @param open girdi parametresi
         * @param high girdi parametresi
         * @param low girdi parametresi
         * @param close girdi parametresi
         */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Quote(
            List<Double> open,
            List<Double> high,
            List<Double> low,
            List<Double> close
    ) {
    }
}
