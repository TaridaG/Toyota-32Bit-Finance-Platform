package com.company.finance_api.portfolio.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.Transaction;
import com.company.finance_api.domain.User;
import com.company.finance_api.domain.enums.Exchange;
import com.company.finance_api.domain.enums.InstrumentType;
import com.company.finance_api.dto.InsightDto;
import com.company.finance_api.dto.InsightSeverity;
import com.company.finance_api.dto.PortfolioValuationAssetDto;
import com.company.finance_api.dto.PortfolioValuationResponse;
import com.company.finance_api.portfolio.domain.PortfolioPosition;
import com.company.finance_api.portfolio.domain.PortfolioPositionBuilder;
import com.company.finance_api.repository.TransactionRepository;
import com.company.finance_api.repository.UserRepository;
import com.company.finance_api.shared.security.CurrentUserResolver;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PortfolioValuationServiceImplTest {

  @Mock private TransactionRepository transactionRepository;
  @Mock private CurrentUserResolver currentUserResolver;
  @Mock private UserRepository userRepository;
  @Mock private MeterRegistry meterRegistry;
  @Mock private PortfolioInsightService portfolioInsightService;
  @Mock private PortfolioPositionBuilder portfolioPositionBuilder;
  @Mock private Counter counter;

  private PortfolioValuationServiceImpl service;

  @BeforeEach
  void setUp() {
    when(meterRegistry.counter(anyString())).thenReturn(counter);
    service =
        new PortfolioValuationServiceImpl(
            transactionRepository,
            currentUserResolver,
            userRepository,
            meterRegistry,
            portfolioInsightService,
            portfolioPositionBuilder);
  }

  @Test
  void should_include_missing_price_asset_with_hasPrice_false_and_sum_only_priced_assets() {
    UUID userId = UUID.randomUUID();
    User user = user(userId);
    Instrument priced = instrument(201L, "BTCUSDT", InstrumentType.CRYPTO, Exchange.BINANCE);
    Instrument missing = instrument(202L, "XAUUSD", InstrumentType.FX, Exchange.TCMB);
    Transaction txPriced = tx(1L, user, priced);
    Transaction txMissing = tx(2L, user, missing);

    when(currentUserResolver.getCurrentUserId()).thenReturn(userId);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(transactionRepository.findByUserOrderByCreatedAtDesc(user))
        .thenReturn(List.of(txPriced, txMissing));

    when(portfolioPositionBuilder.build(priced, List.of(txPriced)))
        .thenReturn(
            Optional.of(
                new PortfolioPosition(
                    priced,
                    new BigDecimal("2"),
                    new BigDecimal("200"),
                    new BigDecimal("100"),
                    new BigDecimal("150"),
                    new BigDecimal("300"),
                    new BigDecimal("100"),
                    true)));
    when(portfolioPositionBuilder.build(missing, List.of(txMissing)))
        .thenReturn(
            Optional.of(
                new PortfolioPosition(
                    missing,
                    new BigDecimal("1"),
                    new BigDecimal("50"),
                    new BigDecimal("50"),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    false)));
    when(portfolioInsightService.analyze(any(), any()))
        .thenReturn(
            List.of(new InsightDto("LARGEST_POSITION", "ok", InsightSeverity.LOW, Map.of())));

    PortfolioValuationResponse result = service.getMyValuation();

    assertEquals(new BigDecimal("300"), result.totalValue());
    assertEquals(2, result.assets().size());
    PortfolioValuationAssetDto missingAsset =
        result.assets().stream()
            .filter(a -> a.instrumentId().equals(202L))
            .findFirst()
            .orElseThrow();
    assertFalse(missingAsset.hasPrice());
    assertEquals(BigDecimal.ZERO, missingAsset.currentValue());
  }

  @Test
  void should_build_segment_breakdown_without_crash() {
    UUID userId = UUID.randomUUID();
    User user = user(userId);
    Instrument crypto = instrument(301L, "SOLUSDT", InstrumentType.CRYPTO, Exchange.BINANCE);
    Instrument fx = instrument(302L, "USDTRY", InstrumentType.FX, Exchange.TCMB);
    Transaction txCrypto = tx(1L, user, crypto);
    Transaction txFx = tx(2L, user, fx);

    when(currentUserResolver.getCurrentUserId()).thenReturn(userId);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(transactionRepository.findByUserOrderByCreatedAtDesc(user))
        .thenReturn(List.of(txCrypto, txFx));

    when(portfolioPositionBuilder.build(crypto, List.of(txCrypto)))
        .thenReturn(
            Optional.of(
                new PortfolioPosition(
                    crypto,
                    new BigDecimal("1"),
                    new BigDecimal("100"),
                    new BigDecimal("100"),
                    new BigDecimal("200"),
                    new BigDecimal("200"),
                    new BigDecimal("100"),
                    true)));
    when(portfolioPositionBuilder.build(fx, List.of(txFx)))
        .thenReturn(
            Optional.of(
                new PortfolioPosition(
                    fx,
                    new BigDecimal("1"),
                    new BigDecimal("100"),
                    new BigDecimal("100"),
                    new BigDecimal("100"),
                    new BigDecimal("100"),
                    BigDecimal.ZERO,
                    true)));
    when(portfolioInsightService.analyze(any(), any())).thenReturn(List.of());

    PortfolioValuationResponse result = service.getMyValuation();

    assertEquals(new BigDecimal("300"), result.totalValue());
    assertTrue(result.segmentBreakdownPercent().containsKey("CRYPTO"));
    assertTrue(result.segmentBreakdownPercent().containsKey("FX"));
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

  private static Transaction tx(Long id, User user, Instrument instrument) {
    Transaction tx = Transaction.buy(user, instrument, new BigDecimal("100"), new BigDecimal("1"));
    setField(tx, "id", id);
    setField(tx, "createdAt", Instant.parse("2026-01-01T10:00:00Z").plusSeconds(id));
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
