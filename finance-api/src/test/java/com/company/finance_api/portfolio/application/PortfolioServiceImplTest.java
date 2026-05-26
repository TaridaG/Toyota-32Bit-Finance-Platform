package com.company.finance_api.portfolio.application;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.Transaction;
import com.company.finance_api.domain.User;
import com.company.finance_api.domain.enums.Exchange;
import com.company.finance_api.domain.enums.InstrumentType;
import com.company.finance_api.dto.PortfolioPositionResponse;
import com.company.finance_api.portfolio.PortfolioPositionBuilder;
import com.company.finance_api.repository.TransactionRepository;
import com.company.finance_api.repository.UserRepository;
import com.company.finance_api.pricing.application.PriceService;
import com.company.finance_api.shared.security.CurrentUserResolver;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceImplTest {

  @Mock private TransactionRepository transactionRepository;
  @Mock private CurrentUserResolver currentUserResolver;
  @Mock private UserRepository userRepository;
  @Mock private PriceService priceService;

  private PortfolioServiceImpl service;

  @BeforeEach
  void setUp() {
    PortfolioPositionBuilder builder =
        new PortfolioPositionBuilder(
            new com.company.finance_api.portfolio.PositionCostBasisCalculator(), priceService);
    service =
        new PortfolioServiceImpl(
            transactionRepository, currentUserResolver, userRepository, builder);
  }

  @Test
  void should_return_portfolio_when_price_missing_without_exception() {
    UUID userId = UUID.randomUUID();
    User user = user(userId);
    Instrument instrument = instrument(101L, "BTCUSDT", InstrumentType.CRYPTO, Exchange.BINANCE);
    Transaction buy =
        tx(1L, Instant.parse("2026-01-01T10:00:00Z"), user, instrument, "100", "2", true);

    when(currentUserResolver.getCurrentUserId()).thenReturn(userId);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(transactionRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(buy));
    when(priceService.getLatestValuationPrice(instrument)).thenReturn(Optional.empty());

    List<PortfolioPositionResponse> result = assertDoesNotThrow(service::getMyPortfolio);

    assertEquals(1, result.size());
    assertTrue(result.get(0).currentPrice().compareTo(BigDecimal.ZERO) == 0);
    assertTrue(result.get(0).currentValue().compareTo(BigDecimal.ZERO) == 0);
  }

  @Test
  void should_not_return_position_after_full_close() {
    UUID userId = UUID.randomUUID();
    User user = user(userId);
    Instrument instrument = instrument(102L, "ETHUSDT", InstrumentType.CRYPTO, Exchange.BINANCE);
    Transaction buy =
        tx(1L, Instant.parse("2026-01-01T10:00:00Z"), user, instrument, "100", "2", true);
    Transaction sell =
        tx(2L, Instant.parse("2026-01-01T11:00:00Z"), user, instrument, "120", "2", false);

    when(currentUserResolver.getCurrentUserId()).thenReturn(userId);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(transactionRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(sell, buy));

    List<PortfolioPositionResponse> result = service.getMyPortfolio();

    assertTrue(result.isEmpty());
  }

  @Test
  void should_return_correct_quantity_and_average_after_partial_sell() {
    UUID userId = UUID.randomUUID();
    User user = user(userId);
    Instrument instrument = instrument(103L, "SOLUSDT", InstrumentType.CRYPTO, Exchange.BINANCE);
    Transaction buy1 =
        tx(1L, Instant.parse("2026-01-01T10:00:00Z"), user, instrument, "100", "10", true);
    Transaction buy2 =
        tx(2L, Instant.parse("2026-01-01T11:00:00Z"), user, instrument, "200", "10", true);
    Transaction sell =
        tx(3L, Instant.parse("2026-01-01T12:00:00Z"), user, instrument, "300", "5", false);

    when(currentUserResolver.getCurrentUserId()).thenReturn(userId);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(transactionRepository.findByUserOrderByCreatedAtDesc(user))
        .thenReturn(List.of(sell, buy2, buy1));
    when(priceService.getLatestValuationPrice(instrument)).thenReturn(Optional.empty());

    List<PortfolioPositionResponse> result = service.getMyPortfolio();

    assertEquals(1, result.size());
    assertEquals(new BigDecimal("15.000000"), result.get(0).quantity().setScale(6));
    assertEquals(new BigDecimal("150.000000"), result.get(0).averagePrice().setScale(6));
  }

  private static User user(UUID id) {
    User user = new User("user@example.com", "user");
    setField(user, "id", id);
    return user;
  }

  private static Instrument instrument(
      Long id, String symbol, InstrumentType type, Exchange exchange) {
    Instrument instrument = new Instrument(symbol, symbol, type, exchange);
    setField(instrument, "id", id);
    return instrument;
  }

  private static Transaction tx(
      Long id,
      Instant createdAt,
      User user,
      Instrument instrument,
      String price,
      String quantity,
      boolean buy) {
    Transaction tx =
        buy
            ? Transaction.buy(user, instrument, new BigDecimal(price), new BigDecimal(quantity))
            : Transaction.sell(user, instrument, new BigDecimal(price), new BigDecimal(quantity));
    setField(tx, "id", id);
    setField(tx, "createdAt", createdAt);
    return tx;
  }

  private static void setField(Object target, String fieldName, Object value) {
    try {
      Field field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }
}
