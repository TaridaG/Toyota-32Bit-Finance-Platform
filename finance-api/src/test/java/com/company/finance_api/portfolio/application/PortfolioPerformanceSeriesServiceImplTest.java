package com.company.finance_api.portfolio.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.finance_api.domain.PortfolioDailyPerformance;
import com.company.finance_api.dto.PortfolioPerformanceSeriesResponse;
import com.company.finance_api.portfolio.external.domain.ExternalPortfolio;
import com.company.finance_api.portfolio.external.repository.ExternalPortfolioRepository;
import com.company.finance_api.repository.PortfolioDailyPerformanceRepository;
import com.company.finance_api.repository.TransactionRepository;
import com.company.finance_api.pricing.application.CurrencyConversionService;
import com.company.finance_api.pricing.application.PriceService;
import com.company.finance_api.shared.security.CurrentUserResolver;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PortfolioPerformanceSeriesServiceImplTest {

  @Mock private PortfolioDailyPerformanceRepository dailyPerformanceRepository;
  @Mock private ExternalPortfolioRepository externalPortfolioRepository;
  @Mock private TransactionRepository transactionRepository;
  @Mock private PriceService priceService;
  @Mock private CurrencyConversionService currencyConversionService;
  @Mock private CurrentUserResolver currentUserResolver;

  @InjectMocks private PortfolioPerformanceSeriesServiceImpl service;

  @Test
  void getMyPerformanceSeries_returnsFullPortfolioHistoryEvenWhenShortRangeRequested() {
    UUID userId = UUID.randomUUID();
    Long portfolioId = 7L;
    String currency = "USD";
    ExternalPortfolio portfolio = new ExternalPortfolio();
    portfolio.setId(portfolioId);
    List<PortfolioDailyPerformance> rows =
        List.of(
            row(userId, portfolioId, currency, LocalDate.of(2026, 3, 1), "100.00", "1.0000000000"),
            row(userId, portfolioId, currency, LocalDate.of(2026, 4, 1), "110.00", "1.0500000000"),
            row(userId, portfolioId, currency, LocalDate.of(2026, 5, 1), "120.00", "1.1000000000"));

    when(currentUserResolver.getCurrentUserId()).thenReturn(userId);
    when(currencyConversionService.normalizeCurrency(currency)).thenReturn(currency);
    when(externalPortfolioRepository.findByIdAndUserId(portfolioId, userId))
        .thenReturn(Optional.of(portfolio));
    when(dailyPerformanceRepository.existsForDay(
            eq(userId), eq(portfolioId), eq(currency), any(LocalDate.class)))
        .thenReturn(true);
    when(dailyPerformanceRepository.findByUserIdAndExternalPortfolioIdAndCurrencyOrderByDayUtcAsc(
            userId, portfolioId, currency))
        .thenReturn(rows);

    PortfolioPerformanceSeriesResponse response =
        service.getMyPerformanceSeries(currency, portfolioId, "1w");

    assertEquals(LocalDate.of(2026, 3, 1), response.inceptionDay());
    assertEquals(3, response.points().size());
    assertEquals(LocalDate.of(2026, 3, 1), response.points().getFirst().day());
    assertEquals(LocalDate.of(2026, 5, 1), response.points().getLast().day());
    verify(dailyPerformanceRepository)
        .findByUserIdAndExternalPortfolioIdAndCurrencyOrderByDayUtcAsc(
            userId, portfolioId, currency);
    verify(dailyPerformanceRepository, never())
        .findByUserIdAndExternalPortfolioIdAndCurrencyAndDayUtcGreaterThanEqualOrderByDayUtcAsc(
            any(), any(), any(), any());
  }

  @Test
  void getMyPerformanceSeries_returnsFullAggregateHistoryEvenWhenShortRangeRequested() {
    UUID userId = UUID.randomUUID();
    String currency = "USD";
    ExternalPortfolio portfolio = new ExternalPortfolio();
    portfolio.setId(11L);
    List<PortfolioDailyPerformance> rows =
        List.of(
            row(userId, 11L, currency, LocalDate.of(2026, 2, 1), "80.00", "1.0000000000"),
            row(userId, 11L, currency, LocalDate.of(2026, 5, 1), "150.00", "1.2000000000"));

    when(currentUserResolver.getCurrentUserId()).thenReturn(userId);
    when(currencyConversionService.normalizeCurrency(currency)).thenReturn(currency);
    when(externalPortfolioRepository.findAllByUserIdOrderByCreatedAtDesc(userId))
        .thenReturn(List.of(portfolio));
    when(dailyPerformanceRepository.existsForDay(
            eq(userId), eq(11L), eq(currency), any(LocalDate.class)))
        .thenReturn(true);
    when(dailyPerformanceRepository.findByUserIdAndCurrencyOrderByDayUtcAsc(userId, currency))
        .thenReturn(rows);

    PortfolioPerformanceSeriesResponse response = service.getMyPerformanceSeries(currency, null, "1m");

    assertEquals(LocalDate.of(2026, 2, 1), response.inceptionDay());
    assertEquals(2, response.points().size());
    assertEquals(LocalDate.of(2026, 2, 1), response.points().getFirst().day());
    assertEquals(LocalDate.of(2026, 5, 1), response.points().getLast().day());
    verify(dailyPerformanceRepository).findByUserIdAndCurrencyOrderByDayUtcAsc(userId, currency);
    verify(dailyPerformanceRepository, never())
        .findByUserIdAndCurrencyAndDayUtcGreaterThanEqualOrderByDayUtcAsc(any(), any(), any());
  }

  private static PortfolioDailyPerformance row(
      UUID userId,
      Long portfolioId,
      String currency,
      LocalDate dayUtc,
      String marketValue,
      String twrIndex) {
    PortfolioDailyPerformance row = new PortfolioDailyPerformance();
    row.setUserId(userId);
    row.setExternalPortfolioId(portfolioId);
    row.setCurrency(currency);
    row.setDayUtc(dayUtc);
    row.setMarketValue(new BigDecimal(marketValue));
    row.setNetFlow(BigDecimal.ZERO.setScale(6));
    row.setDailyPnl(BigDecimal.ZERO.setScale(6));
    row.setDailyReturnPct(BigDecimal.ZERO.setScale(6));
    row.setTwrIndex(new BigDecimal(twrIndex));
    row.setComplete(true);
    row.setComputedAt(Instant.parse("2026-05-25T00:00:00Z"));
    return row;
  }
}
