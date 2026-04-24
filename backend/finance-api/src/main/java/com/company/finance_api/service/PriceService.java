package com.company.finance_api.service;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.enums.PriceType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PriceService {

    // Son fiyat (dashboard, alarm, notification)
    Optional<InstrumentPrice> getLatestPrice(
            Instrument instrument,
            PriceType priceType
    );

    Optional<InstrumentPrice> getLatestValuationPrice(Instrument instrument);

    // Grafik için zaman serisi
    List<InstrumentPrice> getPriceHistory(
            Instrument instrument,
            PriceType priceType,
            Instant start,
            Instant end
    );

    // İleride scheduler / kafka burayı kullanacak
    InstrumentPrice savePrice(InstrumentPrice price);
}
