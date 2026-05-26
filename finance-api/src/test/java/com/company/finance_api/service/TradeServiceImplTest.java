package com.company.finance_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.Transaction;
import com.company.finance_api.domain.User;
import com.company.finance_api.domain.enums.Exchange;
import com.company.finance_api.domain.enums.InstrumentType;
import com.company.finance_api.domain.enums.PriceType;
import com.company.finance_api.domain.enums.PurchaseMode;
import com.company.finance_api.domain.enums.TradeInputMode;
import com.company.finance_api.domain.enums.TransactionType;
import com.company.finance_api.dto.AcquisitionFxRatesSnapshot;
import com.company.finance_api.dto.TradePreviewResponse;
import com.company.finance_api.dto.TradeExecutionRequest;
import com.company.finance_api.event.publisher.TransactionEventPublisher;
import com.company.finance_api.portfolio.external.repository.ExternalPortfolioRepository;
import com.company.finance_api.repository.InstrumentPriceRepository;
import com.company.finance_api.repository.InstrumentRepository;
import com.company.finance_api.repository.TransactionAcquisitionFxRepository;
import com.company.finance_api.repository.TransactionRepository;
import com.company.finance_api.repository.UserRepository;
import com.company.finance_api.service.impl.TlDepositIndexQueryService;
import com.company.finance_api.service.impl.TradeServiceImpl;
import com.company.finance_api.shared.security.CurrentUserResolver;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TradeServiceImplTest {

  @Mock InstrumentRepository instrumentRepository;
  @Mock TransactionRepository transactionRepository;
  @Mock TransactionAcquisitionFxRepository transactionAcquisitionFxRepository;
  @Mock InstrumentPriceRepository instrumentPriceRepository;
  @Mock JdbcTemplate jdbcTemplate;
  @Mock CurrencyConversionService currencyConversionService;
  @Mock CurrentUserResolver currentUserResolver;
  @Mock UserRepository userRepository;
  @Mock ExternalPortfolioRepository externalPortfolioRepository;
  @Mock TransactionEventPublisher transactionEventPublisher;
  @Mock TlDepositIndexQueryService tlDepositIndexQueryService;

  @InjectMocks TradeServiceImpl tradeService;

  @Test
  void preview_shouldThrow_whenInstrumentMissing() {
    TradeExecutionRequest request = liveLotsRequest(999L, "1");

    when(instrumentRepository.findById(999L)).thenReturn(Optional.empty());

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> tradeService.preview(request));
    assertEquals("Instrument not found", ex.getMessage());
  }

  @Test
  void buy_shouldPersistTransaction_andPublishEvent() {
    UUID userId = UUID.randomUUID();
    User user = new User("trader@example.com", "trader");
    long instrumentId = 42L;
    Instrument instrument =
        new Instrument("AAPL", "Apple", InstrumentType.STOCK, Exchange.NASDAQ);
    ReflectionTestUtils.setField(instrument, "id", instrumentId);

    TradeExecutionRequest request = liveLotsRequest(instrumentId, "2");
    InstrumentPrice market =
        new InstrumentPrice(instrument, PriceType.MARKET, new BigDecimal("150"), Instant.now());
    Transaction saved =
        Transaction.buy(
            user,
            instrument,
            null,
            new BigDecimal("150"),
            new BigDecimal("2"),
            PurchaseMode.NOW,
            Instant.now(),
            new BigDecimal("150"),
            TradeInputMode.LOTS,
            "USD",
            new BigDecimal("300"),
            BigDecimal.ONE,
            "NOW_BOUGHT");
    ReflectionTestUtils.setField(saved, "id", 100L);

    when(currentUserResolver.getCurrentUserId()).thenReturn(userId);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(instrumentRepository.findById(instrumentId)).thenReturn(Optional.of(instrument));
    when(instrumentPriceRepository.findTopByInstrumentAndPriceTypeOrderByTimestampDesc(
            instrument, PriceType.MARKET))
        .thenReturn(Optional.of(market));
    when(currencyConversionService.normalizeCurrency("USD")).thenReturn("USD");
    when(currencyConversionService.convert(eq(BigDecimal.ONE), eq("USD"), eq("USD")))
        .thenReturn(BigDecimal.ONE);
    when(currencyConversionService.acquisitionFxHubSnapshot(any(), eq(false)))
        .thenReturn(emptyFxSnapshot());
    when(transactionRepository.save(any(Transaction.class))).thenReturn(saved);

    Transaction result = tradeService.buy(request);

    assertNotNull(result);
    assertEquals(TransactionType.BUY, result.getType());
    verify(transactionAcquisitionFxRepository).save(any());
    verify(transactionEventPublisher).publish(any());
  }

  @Test
  void sell_shouldFail_whenPositionInsufficient() {
    UUID userId = UUID.randomUUID();
    User user = new User("trader@example.com", "trader");
    long instrumentId = 7L;
    Instrument instrument =
        new Instrument("AAPL", "Apple", InstrumentType.STOCK, Exchange.NASDAQ);
    ReflectionTestUtils.setField(instrument, "id", instrumentId);

    TradeExecutionRequest request = liveLotsRequest(instrumentId, "5");
    InstrumentPrice market =
        new InstrumentPrice(instrument, PriceType.MARKET, new BigDecimal("150"), Instant.now());

    when(currentUserResolver.getCurrentUserId()).thenReturn(userId);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(instrumentRepository.findById(instrumentId)).thenReturn(Optional.of(instrument));
    when(instrumentPriceRepository.findTopByInstrumentAndPriceTypeOrderByTimestampDesc(
            instrument, PriceType.MARKET))
        .thenReturn(Optional.of(market));
    when(currencyConversionService.normalizeCurrency("USD")).thenReturn("USD");
    when(currencyConversionService.convert(eq(BigDecimal.ONE), eq("USD"), eq("USD")))
        .thenReturn(BigDecimal.ONE);
    when(currencyConversionService.acquisitionFxHubSnapshot(any(), eq(false)))
        .thenReturn(emptyFxSnapshot());
    when(transactionRepository.findByUserAndInstrumentAndExternalPortfolio(user, instrument, null))
        .thenReturn(Collections.emptyList());

    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> tradeService.sell(request));
    assertEquals("Insufficient position for sell", ex.getMessage());
    verify(transactionRepository, never()).save(any());
  }

  @Test
  void preview_shouldRejectLotsInput_forTlDepositInstrument() {
    long instrumentId = 99L;
    Instrument instrument =
        new Instrument("TLDEP_MT04", "TL Mevduat", InstrumentType.DEPOSIT, Exchange.TCMB);
    ReflectionTestUtils.setField(instrument, "id", instrumentId);
    TradeExecutionRequest request = liveLotsRequest(instrumentId, "5");

    when(instrumentRepository.findById(instrumentId)).thenReturn(Optional.of(instrument));

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> tradeService.preview(request));
    assertEquals("TL deposit supports amount input only", ex.getMessage());
  }

  @Test
  void preview_shouldComputeNowTlDepositAmountUsingIndexPrice() {
    long instrumentId = 101L;
    Instrument instrument =
        new Instrument("TLDEP_MT04", "TL Mevduat", InstrumentType.DEPOSIT, Exchange.TCMB);
    ReflectionTestUtils.setField(instrument, "id", instrumentId);
    TradeExecutionRequest request = new TradeExecutionRequest();
    request.setInstrumentId(instrumentId);
    request.setInputMode(TradeInputMode.AMOUNT);
    request.setAmount(new BigDecimal("100.00"));
    request.setInputCurrency("USD");
    request.setPurchaseMode(PurchaseMode.NOW);

    when(instrumentRepository.findById(instrumentId)).thenReturn(Optional.of(instrument));
    when(currencyConversionService.normalizeCurrency("USD")).thenReturn("USD");
    when(currencyConversionService.convert(eq(BigDecimal.ONE), eq("USD"), eq("TRY")))
        .thenReturn(new BigDecimal("40.000000"));
    when(currencyConversionService.acquisitionFxHubSnapshot(any(), eq(false)))
        .thenReturn(emptyFxSnapshot());
    when(tlDepositIndexQueryService.getLatestPrice(instrument))
        .thenReturn(
            Optional.of(
                new InstrumentPrice(
                    instrument,
                    PriceType.MARKET,
                    new BigDecimal("1.050000"),
                    Instant.parse("2026-05-20T21:59:59Z"))));

    TradePreviewResponse response = tradeService.preview(request);

    assertEquals(new BigDecimal("1.050000"), response.unitPriceUsed());
    assertEquals(new BigDecimal("40.000000"), response.fxRateUsed());
    assertEquals(new BigDecimal("3809.523810"), response.computedLots());
    assertEquals("TL_DEPOSIT_INDEX_LIVE", response.unitPriceSource());
    assertEquals("TRY", response.instrumentQuoteCurrency());
  }

  @Test
  void preview_shouldComputePastTlDepositAmountUsingHistoricalIndexAndFx() {
    long instrumentId = 102L;
    Instrument instrument =
        new Instrument("TLDEP_MT05", "TL Mevduat", InstrumentType.DEPOSIT, Exchange.TCMB);
    ReflectionTestUtils.setField(instrument, "id", instrumentId);
    Instant acquiredAt = Instant.parse("2026-05-15T12:00:00Z");
    TradeExecutionRequest request = new TradeExecutionRequest();
    request.setInstrumentId(instrumentId);
    request.setInputMode(TradeInputMode.AMOUNT);
    request.setAmount(new BigDecimal("100.00"));
    request.setInputCurrency("EUR");
    request.setPurchaseMode(PurchaseMode.PAST);
    request.setAcquiredAt(acquiredAt);

    when(instrumentRepository.findById(instrumentId)).thenReturn(Optional.of(instrument));
    when(currencyConversionService.normalizeCurrency("EUR")).thenReturn("EUR");
    when(currencyConversionService.convertAt(any(), eq(BigDecimal.ONE), eq("EUR"), eq("TRY")))
        .thenReturn(new BigDecimal("43.000000"));
    when(currencyConversionService.acquisitionFxHubSnapshot(any(), eq(true)))
        .thenReturn(emptyFxSnapshot());
    when(tlDepositIndexQueryService.getLatestPriceBefore(eq(instrument), any()))
        .thenReturn(
            Optional.of(
                new InstrumentPrice(
                    instrument,
                    PriceType.MARKET,
                    new BigDecimal("1.020000"),
                    Instant.parse("2026-05-15T20:59:59Z"))));

    TradePreviewResponse response = tradeService.preview(request);

    assertEquals(new BigDecimal("1.020000"), response.unitPriceUsed());
    assertEquals(new BigDecimal("43.000000"), response.fxRateUsed());
    assertEquals(new BigDecimal("4215.686275"), response.computedLots());
    assertEquals("TL_DEPOSIT_INDEX_HISTORICAL", response.unitPriceSource());
    assertEquals(acquiredAt, response.effectiveAcquiredAt());
  }

  private static TradeExecutionRequest liveLotsRequest(long instrumentId, String lots) {
    TradeExecutionRequest request = new TradeExecutionRequest();
    request.setInstrumentId(instrumentId);
    request.setInputMode(TradeInputMode.LOTS);
    request.setLots(new BigDecimal(lots));
    request.setInputCurrency("USD");
    request.setPurchaseMode(PurchaseMode.NOW);
    return request;
  }

  private static AcquisitionFxRatesSnapshot emptyFxSnapshot() {
    return new AcquisitionFxRatesSnapshot(
        Instant.parse("2020-01-01T00:00:00Z").toString(),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }
}
