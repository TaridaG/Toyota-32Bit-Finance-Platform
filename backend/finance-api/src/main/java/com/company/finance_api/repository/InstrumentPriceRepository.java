package com.company.finance_api.repository;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.enums.PriceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface InstrumentPriceRepository
        extends JpaRepository<InstrumentPrice, Long> {

    interface SymbolPriceView {
        String getSymbol();
        java.math.BigDecimal getPrice();
    }

    // Son fiyat (grafik, dashboard, alarm için kritik)
    Optional<InstrumentPrice> findTopByInstrumentAndPriceTypeOrderByTimestampDesc(
            Instrument instrument,
            PriceType priceType
    );

    // Zaman aralığı (grafik çizimi için)
    List<InstrumentPrice> findByInstrumentAndPriceTypeAndTimestampBetweenOrderByTimestampAsc(
            Instrument instrument,
            PriceType priceType,
            Instant start,
            Instant end
    );

    @Query(value = """
            SELECT DISTINCT ON (i.symbol)
                i.symbol AS symbol,
                ip.price AS price
            FROM instrument_prices ip
            JOIN instruments i ON i.id = ip.instrument_id
            WHERE i.symbol IN (:symbols)
              AND ip.price_type = :priceType
              AND ip.timestamp <= :target
            ORDER BY i.symbol, ip.timestamp DESC, ip.id DESC
            """, nativeQuery = true)
    List<SymbolPriceView> findLatestPricesAtOrBefore(
            @Param("symbols") List<String> symbols,
            @Param("priceType") String priceType,
            @Param("target") Instant target
    );
}
