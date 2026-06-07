package com.company.finance_api.portfolio.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.company.finance_api.instrument.domain.Instrument;
import com.company.finance_api.instrument.domain.enums.Exchange;
import com.company.finance_api.instrument.domain.enums.InstrumentType;
import com.company.finance_api.portfolio.domain.PositionCostBasisCalculator;
import com.company.finance_api.portfolio.domain.Transaction;
import com.company.finance_api.portfolio.domain.enums.PurchaseMode;
import com.company.finance_api.portfolio.domain.enums.TradeInputMode;
import com.company.finance_api.portfolio.external.domain.ExternalPortfolio;
import com.company.finance_api.portfolio.external.infrastructure.persistence.ExternalPortfolioRepository;
import com.company.finance_api.portfolio.infrastructure.persistence.TransactionRepository;
import com.company.finance_api.profile.domain.User;
import com.company.finance_api.profile.infrastructure.persistence.UserRepository;
import com.company.finance_api.pricing.application.CurrencyConversionService;
import com.company.finance_api.pricing.application.PriceService;
import com.company.finance_api.shared.cache.JsonCacheService;
import com.company.finance_api.shared.security.CurrentUserResolver;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PortfolioOverviewServiceImplTest {

  @Mock TransactionRepository transactionRepository;
  @Mock UserRepository userRepository;
  @Mock CurrentUserResolver currentUserResolver;
  @Mock PriceService priceService;
  @Mock CurrencyConversionService currencyConversionService;
  @Mock ExternalPortfolioRepository externalPortfolioRepository;
  @Mock JsonCacheService jsonCacheService;

  @InjectMocks PortfolioOverviewServiceImpl service;

  private final PositionCostBasisCalculator calculator = new PositionCostBasisCalculator();

  private UUID userId;
  private User user;
  private ExternalPortfolio portfolio;
  private Instrument akbnk;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(service, "positionCostBasisCalculator", calculator);
    userId = UUID.randomUUID();
    user = new User("trader@example.com", "trader");
    portfolio = new ExternalPortfolio(user, "Test", "TRY");
    ReflectionTestUtils.setField(portfolio, "id", 1L);
    akbnk = new Instrument("AKBNK", "Akbank", InstrumentType.STOCK, Exchange.BIST);
    ReflectionTestUtils.setField(akbnk, "id", 10L);
  }

  @Test
  void getHoldingsAsOf_shouldReturnQuantityBeforeSameDayBuys() {
    when(currentUserResolver.getCurrentUserId()).thenReturn(userId);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(externalPortfolioRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(portfolio));
    when(currencyConversionService.normalizeCurrency("TRY")).thenReturn("TRY");
    when(currencyConversionService.convert(any(), eq("TRY"), eq("TRY")))
        .thenAnswer(inv -> inv.getArgument(0));
    when(priceService.getLatestValuationPriceBefore(eq(akbnk), any())).thenReturn(Optional.empty());
    when(transactionRepository.findByUserAndExternalPortfolioOrderByCreatedAtAsc(user, portfolio))
        .thenReturn(
            List.of(
                pastBuy(1L, LocalDate.of(2025, 9, 10), "100", "10"),
                pastBuy(2L, LocalDate.of(2025, 10, 10), "110", "10"),
                pastBuy(3L, LocalDate.of(2025, 11, 10), "120", "10")));

    var oct8 =
        service.getHoldingsAsOf("TRY", 1L, LocalDate.of(2025, 10, 8)).items().stream()
            .filter(i -> "AKBNK".equals(i.symbol()))
            .findFirst()
            .orElseThrow();
    assertEquals(new BigDecimal("10.000000"), oct8.quantity().setScale(6));

    var oct11 =
        service.getHoldingsAsOf("TRY", 1L, LocalDate.of(2025, 10, 11)).items().stream()
            .filter(i -> "AKBNK".equals(i.symbol()))
            .findFirst()
            .orElseThrow();
    assertEquals(new BigDecimal("20.000000"), oct11.quantity().setScale(6));
  }

  @Test
  void getHoldingsAsOf_shouldRejectFutureDate() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                service.getHoldingsAsOf(
                    "TRY", 1L, LocalDate.now(ZoneOffset.UTC).plusDays(1)));
    assertEquals("asOf date cannot be in the future", ex.getMessage());
  }

  private Transaction pastBuy(Long id, LocalDate day, String price, String qty) {
    Instant acquiredAt = day.atStartOfDay(ZoneOffset.UTC).toInstant();
    Transaction tx =
        Transaction.buy(
            user,
            akbnk,
            portfolio,
            new BigDecimal(price),
            new BigDecimal(qty),
            PurchaseMode.PAST,
            acquiredAt,
            new BigDecimal(price),
            TradeInputMode.LOTS,
            "TRY",
            new BigDecimal(price).multiply(new BigDecimal(qty)),
            BigDecimal.ONE,
            "PAST_BOUGHT");
    ReflectionTestUtils.setField(tx, "id", id);
    ReflectionTestUtils.setField(tx, "createdAt", acquiredAt);
    return tx;
  }
}
