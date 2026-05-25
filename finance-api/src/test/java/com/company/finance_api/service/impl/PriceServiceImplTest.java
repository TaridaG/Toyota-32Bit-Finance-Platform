package com.company.finance_api.service.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.enums.PriceType;
import com.company.finance_api.event.PriceUpdatedEvent;
import com.company.finance_api.repository.InstrumentPriceRepository;
import com.company.finance_api.shared.cache.PriceCacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PriceServiceImplTest {

  @Mock private InstrumentPriceRepository priceRepository;
  @Mock private PriceCacheService priceCacheService;
  @Mock private ApplicationEventPublisher eventPublisher;

  @Test
  void savePriceEvictsLatestCacheEntryAfterPersist() {
    PriceServiceImpl service =
        new PriceServiceImpl(priceRepository, priceCacheService, eventPublisher);
    Instrument instrument = mock(Instrument.class);
    InstrumentPrice input = mock(InstrumentPrice.class);
    InstrumentPrice saved = mock(InstrumentPrice.class);

    when(priceRepository.save(input)).thenReturn(saved);
    when(saved.getInstrument()).thenReturn(instrument);
    when(instrument.getId()).thenReturn(42L);
    when(saved.getPriceType()).thenReturn(PriceType.MARKET);

    service.savePrice(input);

    verify(priceCacheService).evictLatestPrice(42L, PriceType.MARKET);
    verify(eventPublisher).publishEvent(org.mockito.ArgumentMatchers.any(PriceUpdatedEvent.class));
  }
}
