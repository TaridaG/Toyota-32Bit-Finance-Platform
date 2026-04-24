package com.company.finance_api.portfolio.valuation.impl;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.portfolio.valuation.InstrumentPriceProvider;
import com.company.finance_api.repository.InstrumentRepository;
import com.company.finance_api.service.PriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class InstrumentPriceProviderImpl implements InstrumentPriceProvider {

    private final PriceService priceService;
    private final InstrumentRepository instrumentRepository;

    @Override
    public BigDecimal getCurrentPrice(Long instrumentId) {
        Instrument instrument = instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new RuntimeException("Instrument not found for instrumentId=" + instrumentId));
        return priceService.getLatestValuationPrice(instrument)
                .orElseThrow(() -> new RuntimeException("Price not found for instrumentId=" + instrumentId))
                .getPrice();
    }
}

