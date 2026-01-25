package com.company.finance_api.repository;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.enums.PriceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface InstrumentPriceRepository
        extends JpaRepository<InstrumentPrice, Long> {

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
}
