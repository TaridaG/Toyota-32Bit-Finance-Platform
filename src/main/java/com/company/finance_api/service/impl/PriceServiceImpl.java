package com.company.finance_api.service.impl;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.enums.PriceType;
import com.company.finance_api.repository.InstrumentPriceRepository;
import com.company.finance_api.service.PriceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class PriceServiceImpl implements PriceService {

    private final InstrumentPriceRepository priceRepository;

    public PriceServiceImpl(InstrumentPriceRepository priceRepository) {
        this.priceRepository = priceRepository;
    }

    @Override
    public Optional<InstrumentPrice> getLatestPrice(
            Instrument instrument,
            PriceType priceType
    ) {
        return priceRepository
                .findTopByInstrumentAndPriceTypeOrderByTimestampDesc(
                        instrument,
                        priceType
                );
    }

    @Override
    public List<InstrumentPrice> getPriceHistory(
            Instrument instrument,
            PriceType priceType,
            Instant start,
            Instant end
    ) {
        return priceRepository
                .findByInstrumentAndPriceTypeAndTimestampBetweenOrderByTimestampAsc(
                        instrument,
                        priceType,
                        start,
                        end
                );
    }

    @Override
    @Transactional
    public InstrumentPrice savePrice(InstrumentPrice price) {
        return priceRepository.save(price);
    }
}
