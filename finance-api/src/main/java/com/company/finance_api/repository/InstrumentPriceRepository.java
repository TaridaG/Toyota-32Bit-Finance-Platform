package com.company.finance_api.repository;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.enums.PriceType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** InstrumentPrice entity persistence için Spring Data repository. */
public interface InstrumentPriceRepository extends JpaRepository<InstrumentPrice, Long> {

  interface SymbolPriceView {

    String getSymbol();

    java.math.BigDecimal getPrice();
  }

  // Son fiyat (grafik, dashboard, alarm için kritik)
  Optional<InstrumentPrice> findTopByInstrumentAndPriceTypeOrderByTimestampDesc(
      Instrument instrument, PriceType priceType);

  Optional<InstrumentPrice> findTopByInstrumentAndPriceTypeOrderByTimestampAsc(
      Instrument instrument, PriceType priceType);

  Optional<InstrumentPrice>
      findFirstByInstrumentAndPriceTypeAndTimestampLessThanOrderByTimestampDesc(
          Instrument instrument, PriceType priceType, Instant instant);

  // Zaman aralığı (grafik çizimi için)
  List<InstrumentPrice> findByInstrumentAndPriceTypeAndTimestampBetweenOrderByTimestampAsc(
      Instrument instrument, PriceType priceType, Instant start, Instant end);

  @Query(
      value =
          """
            SELECT DISTINCT ON (i.symbol)
                i.symbol AS symbol,
                ip.price AS price
            FROM instrument_prices ip
            JOIN instruments i ON i.id = ip.instrument_id
            WHERE i.symbol IN (:symbols)
              AND ip.price_type = :priceType
              AND ip.timestamp <= :target
            ORDER BY i.symbol, ip.timestamp DESC, ip.id DESC
            """,
      nativeQuery = true)
  List<SymbolPriceView> findLatestPricesAtOrBefore(
      @Param("symbols") List<String> symbols,
      @Param("priceType") String priceType,
      @Param("target") Instant target);

  /**
   * Distinct instruments that received at least one price row in the window (market data “touch”).
   */
  @Query(
      "select count(distinct ip.instrument.id) from InstrumentPrice ip where ip.timestamp >= :from and ip.timestamp < :to")
  long countDistinctInstrumentsWithPriceBetween(
      @Param("from") Instant from, @Param("to") Instant to);
}
