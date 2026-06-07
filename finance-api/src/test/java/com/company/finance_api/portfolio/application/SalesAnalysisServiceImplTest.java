package com.company.finance_api.portfolio.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.company.finance_api.instrument.domain.Instrument;
import com.company.finance_api.portfolio.domain.PositionCostBasisCalculator;
import com.company.finance_api.portfolio.domain.PositionCostBasisCalculator.PositionCostBasis;
import com.company.finance_api.portfolio.domain.Transaction;
import com.company.finance_api.portfolio.infrastructure.http.dto.SalesAnalysisPageResponse;
import com.company.finance_api.portfolio.infrastructure.persistence.TransactionRepository;
import com.company.finance_api.pricing.application.PriceService;
import com.company.finance_api.pricing.domain.InstrumentPrice;
import com.company.finance_api.pricing.domain.enums.PriceType;
import com.company.finance_api.profile.domain.User;
import com.company.finance_api.profile.infrastructure.persistence.UserRepository;
import com.company.finance_api.instrument.domain.enums.Exchange;
import com.company.finance_api.instrument.domain.enums.InstrumentType;
import com.company.finance_api.portfolio.external.infrastructure.persistence.ExternalPortfolioRepository;
import com.company.finance_api.shared.security.CurrentUserResolver;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SalesAnalysisServiceImplTest {

  @Mock private TransactionRepository transactionRepository;
  @Mock private CurrentUserResolver currentUserResolver;
  @Mock private UserRepository userRepository;
  @Mock private ExternalPortfolioRepository externalPortfolioRepository;
  @Mock private PositionCostBasisCalculator positionCostBasisCalculator;
  @Mock private PriceService priceService;

  @InjectMocks private SalesAnalysisServiceImpl service;

  @Test
  void getMySalesAnalysisPage_shouldComputeRealizedPnlAndOpportunityDelta() {
    UUID userId = UUID.randomUUID();
    User user = new User("trader@example.com", "trader");
    ReflectionTestUtils.setField(user, "id", userId);
    Instrument instrument =
        new Instrument("AAPL", "Apple", InstrumentType.STOCK, Exchange.NASDAQ);
    ReflectionTestUtils.setField(instrument, "id", 7L);

    Transaction buy =
        Transaction.buy(user, instrument, new BigDecimal("100"), new BigDecimal("10"));
    ReflectionTestUtils.setField(buy, "id", 1L);
    ReflectionTestUtils.setField(buy, "createdAt", Instant.parse("2026-01-01T10:00:00Z"));
    Transaction sell =
        Transaction.sell(user, instrument, new BigDecimal("120"), new BigDecimal("4"));
    ReflectionTestUtils.setField(sell, "id", 2L);
    ReflectionTestUtils.setField(sell, "createdAt", Instant.parse("2026-01-02T10:00:00Z"));

    when(currentUserResolver.getCurrentUserId()).thenReturn(userId);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(transactionRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(sell)));
    when(transactionRepository.findByUserAndInstrumentAndExternalPortfolio(user, instrument, null))
        .thenReturn(List.of(buy, sell));
    when(positionCostBasisCalculator.calculate(any()))
        .thenReturn(new PositionCostBasis(new BigDecimal("10"), new BigDecimal("1000"), new BigDecimal("100")));
    when(priceService.getLatestValuationPrice(instrument))
        .thenReturn(
            Optional.of(
                new InstrumentPrice(
                    instrument, PriceType.MARKET, new BigDecimal("130"), Instant.now())));

    SalesAnalysisPageResponse page = service.getMySalesAnalysisPage(0, 20, null, null, null, null);

    assertEquals(1, page.content().size());
    var row = page.content().get(0);
    assertEquals(new BigDecimal("4"), row.quantity());
    assertEquals(new BigDecimal("100"), row.avgCostAtSell());
    assertEquals(new BigDecimal("480"), row.sellProceeds());
    assertEquals(new BigDecimal("400"), row.costBasis());
    assertEquals(new BigDecimal("80"), row.realizedPnl());
    assertEquals(new BigDecimal("520"), row.hypotheticalValueNow());
    assertEquals(new BigDecimal("40"), row.opportunityDelta());
  }
}
