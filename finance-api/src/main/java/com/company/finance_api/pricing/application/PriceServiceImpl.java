package com.company.finance_api.pricing.application;

import com.company.finance_api.instrument.domain.Instrument;
import com.company.finance_api.pricing.domain.InstrumentPrice;
import com.company.finance_api.pricing.domain.enums.PriceType;
import com.company.finance_api.shared.messaging.event.PriceUpdatedEvent;
import com.company.finance_api.pricing.infrastructure.persistence.InstrumentPriceRepository;
import com.company.finance_api.pricing.infrastructure.query.TlDepositIndexQueryService;
import com.company.finance_api.shared.cache.PriceCacheService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** PriceServiceImpl iş mantığını uygular (price service). */
@Service
@Transactional(readOnly = true)
public class PriceServiceImpl implements PriceService {

  private static final List<PriceType> VALUATION_PRICE_TYPES =
      List.of(PriceType.MARKET, PriceType.FX_MID, PriceType.FUND_NAV);

  private final InstrumentPriceRepository priceRepository;
  private final PriceCacheService priceCacheService;
  private final ApplicationEventPublisher eventPublisher;
  private final TlDepositIndexQueryService tlDepositIndexQueryService;

  public PriceServiceImpl(
      InstrumentPriceRepository priceRepository,
      PriceCacheService priceCacheService,
      ApplicationEventPublisher eventPublisher,
      TlDepositIndexQueryService tlDepositIndexQueryService) {
    this.priceRepository = priceRepository;
    this.priceCacheService = priceCacheService;
    this.eventPublisher = eventPublisher;
    this.tlDepositIndexQueryService = tlDepositIndexQueryService;
  }

  /** LatestPrice sorgusunu döner. */
  @Override
  public Optional<InstrumentPrice> getLatestPrice(Instrument instrument, PriceType priceType) {
    // 1️⃣ Cache
    Optional<InstrumentPrice> cached =
        priceCacheService.getLatestPrice(instrument.getId(), priceType);

    if (cached.isPresent()) {
      return cached;
    }

    // 2️⃣ DB fallback
    Optional<InstrumentPrice> fromDb =
        priceRepository.findTopByInstrumentAndPriceTypeOrderByTimestampDesc(instrument, priceType);

    // 3️⃣ Cache write
    fromDb.ifPresent(priceCacheService::putLatestPrice);

    return fromDb;
  }

  /** LatestValuationPrice sorgusunu döner. */
  @Override
  public Optional<InstrumentPrice> getLatestValuationPrice(Instrument instrument) {
    if (tlDepositIndexQueryService.supports(instrument)) {
      return tlDepositIndexQueryService.getLatestPrice(instrument);
    }
    for (PriceType priceType : VALUATION_PRICE_TYPES) {
      Optional<InstrumentPrice> found = getLatestPrice(instrument, priceType);
      if (found.isPresent()) {
        return found;
      }
    }
    return Optional.empty();
  }

  /** LatestValuationPriceBefore sorgusunu döner. */
  @Override
  public Optional<InstrumentPrice> getLatestValuationPriceBefore(
      Instrument instrument, Instant exclusiveEnd) {
    if (tlDepositIndexQueryService.supports(instrument)) {
      return tlDepositIndexQueryService.getLatestPriceBefore(instrument, exclusiveEnd);
    }
    for (PriceType priceType : VALUATION_PRICE_TYPES) {
      Optional<InstrumentPrice> fromDb =
          priceRepository.findFirstByInstrumentAndPriceTypeAndTimestampLessThanOrderByTimestampDesc(
              instrument, priceType, exclusiveEnd);
      if (fromDb.isPresent()) {
        return fromDb;
      }
    }
    return Optional.empty();
  }

  /** PriceHistory sorgusunu döner. */
  @Override
  public List<InstrumentPrice> getPriceHistory(
      Instrument instrument, PriceType priceType, Instant start, Instant end) {
    return priceRepository.findByInstrumentAndPriceTypeAndTimestampBetweenOrderByTimestampAsc(
        instrument, priceType, start, end);
  }

  @Override
  @Transactional
  /** InstrumentPrice kaydını persist eder. */
  public InstrumentPrice savePrice(InstrumentPrice price) {

    InstrumentPrice saved = priceRepository.save(price);
    priceCacheService.evictLatestPrice(saved.getInstrument().getId(), saved.getPriceType());

    eventPublisher.publishEvent(PriceUpdatedEvent.of(saved));

    return saved;
  }
}
