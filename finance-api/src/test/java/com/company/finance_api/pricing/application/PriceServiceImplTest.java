package com.company.finance_api.pricing.application;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.finance_api.instrument.domain.Instrument;
import com.company.finance_api.pricing.domain.InstrumentPrice;
import com.company.finance_api.instrument.domain.enums.Exchange;
import com.company.finance_api.instrument.domain.enums.InstrumentType;
import com.company.finance_api.pricing.domain.enums.PriceType;
import com.company.finance_api.shared.messaging.event.PriceUpdatedEvent;
import com.company.finance_api.pricing.infrastructure.persistence.InstrumentPriceRepository;
import com.company.finance_api.pricing.infrastructure.query.TlDepositIndexQueryService;
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
  @Mock private TlDepositIndexQueryService tlDepositIndexQueryService;

  @Test
  void savePriceEvictsLatestCacheEntryAfterPersist() {
    PriceServiceImpl service =
        new PriceServiceImpl(
            priceRepository, priceCacheService, eventPublisher, tlDepositIndexQueryService);
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

  @Test
  void getLatestValuationPriceUsesTlDepositIndexServiceForDepositInstrument() {
    PriceServiceImpl service =
        new PriceServiceImpl(
            priceRepository, priceCacheService, eventPublisher, tlDepositIndexQueryService);
    Instrument instrument =
        new Instrument("TLDEP_MT04", "TL Mevduat - 1 yila kadar", InstrumentType.DEPOSIT, Exchange.TCMB);
    InstrumentPrice synthetic =
        new InstrumentPrice(instrument, PriceType.MARKET, new java.math.BigDecimal("1.012345"), java.time.Instant.now());

    when(tlDepositIndexQueryService.supports(instrument)).thenReturn(true);
    when(tlDepositIndexQueryService.getLatestPrice(instrument)).thenReturn(java.util.Optional.of(synthetic));

    org.junit.jupiter.api.Assertions.assertEquals(
        java.util.Optional.of(synthetic), service.getLatestValuationPrice(instrument));
    verify(tlDepositIndexQueryService).getLatestPrice(instrument);
  }
}
