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
     * Yahoo chart REST yanıtının üst seviye chart payload'ını temsil eder.
         * @param result sembol kotasyonu ve mum verisi içeren sonuç listesi
         * @param error Yahoo API hata detayı; başarılı yanıtta null olabilir
         */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Chart(
            List<Result> result,
            Error error
    ) {
    }

    /**
     * Yahoo chart API hata gövdesini deserialize eden DTO.
         * @param code Yahoo hata kodu
         * @param description insan okunur hata açıklaması
         */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Error(
            String code,
            String description
    ) {
    }

    /**
     * Tek bir sembol için Yahoo chart sonucu; meta, zaman serisi ve indicator verisini taşır.
         * @param meta regular market fiyatı ve borsa meta verileri
         * @param timestamp mum veya kotasyon epoch saniye listesi
         * @param indicators OHLC quote serilerini içeren indicator bloğu
         */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(
            Meta meta,
            List<Long> timestamp,
            Indicators indicators
    ) {
    }

    /**
     * Yahoo chart meta bloğu; spot kotasyon ve piyasa özet alanlarını deserialize eder.
         * @param symbol enstrüman sembolü
         * @param regularMarketPrice güncel regular market spot fiyatı
         * @param regularMarketTime regular market fiyatının epoch saniye zaman damgası
         * @param regularMarketVolume günlük işlem hacmi
         * @param regularMarketOpen gün açılış fiyatı
         * @param regularMarketDayHigh gün içi en yüksek fiyat
         * @param regularMarketDayLow gün içi en düşük fiyat
         * @param openInterest açık pozisyon (varant/vadeli için)
         * @param exchangeName kısa borsa adı
         * @param fullExchangeName tam borsa adı
         * @param shortName enstrüman kısa adı
         * @param underlyingSymbol türev enstrümanın dayanak sembolü
         * @param expireDate vade bitiş epoch saniyesi
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
     * Yahoo chart indicator bloğu; OHLC quote serilerini gruplar.
         * @param quote open/high/low/close zaman serisi listelerini taşıyan quote kaydı
         */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Indicators(
            List<Quote> quote
    ) {
    }

    /**
     * Yahoo chart OHLC quote serilerini deserialize eden DTO.
         * @param open açılış fiyatı zaman serisi
         * @param high en yüksek fiyat zaman serisi
         * @param low en düşük fiyat zaman serisi
         * @param close kapanış fiyatı zaman serisi
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
