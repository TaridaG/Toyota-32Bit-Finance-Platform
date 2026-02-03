package com.company.finance_api.service.impl;

import com.company.finance_api.cache.PriceCacheService;
import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.enums.PriceType;
import com.company.finance_api.repository.InstrumentPriceRepository;
import com.company.finance_api.service.PriceService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class PriceServiceImpl implements PriceService {

    private final InstrumentPriceRepository priceRepository;
    private final PriceCacheService priceCacheService;

    public PriceServiceImpl(InstrumentPriceRepository priceRepository, PriceCacheService priceCacheService) {
        this.priceRepository = priceRepository;
        this.priceCacheService = priceCacheService;
    }


    @Override
    public Optional<InstrumentPrice> getLatestPrice(
            Instrument instrument,
            PriceType priceType
    ) {
        // 1️⃣ Cache
        Optional<InstrumentPrice> cached =
                priceCacheService.getLatestPrice(instrument.getId(), priceType);

        if (cached.isPresent()) {
            return cached;
        }

        // 2️⃣ DB fallback
        Optional<InstrumentPrice> fromDb =
                priceRepository.findTopByInstrumentAndPriceTypeOrderByTimestampDesc(
                        instrument,
                        priceType
                );

        // 3️⃣ Cache write
        fromDb.ifPresent(priceCacheService::putLatestPrice);

        return fromDb;
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

        InstrumentPrice saved = priceRepository.save(price);

        // 🔄 Cache refresh
        priceCacheService.putLatestPrice(saved);

        return saved;
    }
}
