package com.company.finance_api.portfolio.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.company.finance_api.instrument.domain.Instrument;
import com.company.finance_api.instrument.domain.enums.Exchange;
import com.company.finance_api.instrument.domain.enums.InstrumentType;
import com.company.finance_api.portfolio.domain.enums.PurchaseMode;
import com.company.finance_api.portfolio.domain.enums.TradeInputMode;
import com.company.finance_api.pricing.application.CurrencyConversionService;
import com.company.finance_api.profile.domain.User;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TransactionFxDisplayResolverTest {

  @Mock private CurrencyConversionService currencyConversionService;

  private TransactionFxDisplayResolver resolver;
  private User user;

  @BeforeEach
  void setUp() {
    resolver = new TransactionFxDisplayResolver(currencyConversionService);
    user = new User("trader@example.com", "trader");
  }

  @Test
  void resolve_returnsNull_forDomesticTryStock() {
    Instrument instrument =
        new Instrument("AKBNK", "Akbank", InstrumentType.STOCK, Exchange.BIST);
    Transaction tx = buyTx(instrument, "TRY", new BigDecimal("1"), new BigDecimal("69.00"));

    assertNull(resolver.resolve(tx, null));
  }

  @Test
  void resolve_returnsAedTry_fromAcquisitionSnapshot() {
    Instrument instrument = new Instrument("AEDTRY", "AED/TRY", InstrumentType.FX, Exchange.TCMB);
    Transaction tx = buyTx(instrument, "TRY", new BigDecimal("1"), new BigDecimal("11.25"));
    TransactionAcquisitionFx snap =
        new TransactionAcquisitionFx(
            1L,
            Instant.parse("2026-06-07T20:59:59Z"),
            null,
            null,
            null,
            null,
            new BigDecimal("11.2500000000"),
            null,
            null,
            null);

    FxDisplayLeg leg = resolver.resolve(tx, snap);

    assertEquals(new FxDisplayLeg("AED", "TRY", new BigDecimal("11.2500000000")), leg);
  }

  @Test
  void resolve_returnsXauTry_fromUnitPrice_whenSnapshotMissingHubLeg() {
    Instrument instrument = new Instrument("XAUTRY", "Gold / TRY", InstrumentType.FX, Exchange.TCMB);
    Transaction tx = buyTx(instrument, "TRY", new BigDecimal("1"), new BigDecimal("37300.95"));

    FxDisplayLeg leg = resolver.resolve(tx, null);

    assertEquals(new FxDisplayLeg("XAU", "TRY", new BigDecimal("37300.95")), leg);
  }

  @Test
  void resolve_returnsUsdTry_forTryPaymentOnUsdListedStock() {
    Instrument instrument =
        new Instrument("AAPL", "Apple", InstrumentType.STOCK, Exchange.NASDAQ);
    Transaction tx = buyTx(instrument, "TRY", new BigDecimal("0.025"), new BigDecimal("40.00"));
    TransactionAcquisitionFx snap =
        new TransactionAcquisitionFx(
            1L,
            Instant.parse("2026-06-07T20:59:59Z"),
            new BigDecimal("40.0000000000"),
            null,
            null,
            null,
            null,
            null,
            null,
            null);

    FxDisplayLeg leg = resolver.resolve(tx, snap);

    assertEquals(new FxDisplayLeg("USD", "TRY", new BigDecimal("40.0000000000")), leg);
  }

  @Test
  void resolve_invertsFxRateUsed_whenTryPaymentWithoutSnapshot() {
    Instrument instrument =
        new Instrument("AAPL", "Apple", InstrumentType.STOCK, Exchange.NASDAQ);
    Transaction tx = buyTx(instrument, "TRY", new BigDecimal("0.025"), null);

    FxDisplayLeg leg = resolver.resolve(tx, null);

    assertLegEquals("USD", "TRY", "40", leg);
  }

  @Test
  void resolve_fallsBackToConversionService_whenFxMetadataMissing() {
    Instrument instrument =
        new Instrument("AAPL", "Apple", InstrumentType.STOCK, Exchange.NASDAQ);
    Transaction tx = buyTx(instrument, null, null, null);
    ReflectionTestUtils.setField(tx, "purchaseMode", PurchaseMode.PAST);
    ReflectionTestUtils.setField(tx, "acquiredAt", Instant.parse("2026-06-07T12:00:00Z"));
    when(currencyConversionService.convertAt(any(), eq(BigDecimal.ONE), eq("TRY"), eq("USD")))
        .thenReturn(new BigDecimal("0.025"));

    FxDisplayLeg leg = resolver.resolve(tx, null);

    assertLegEquals("USD", "TRY", "40", leg);
  }

  private static void assertLegEquals(String from, String to, String rate, FxDisplayLeg leg) {
    assertEquals(from, leg.fromCurrency());
    assertEquals(to, leg.toCurrency());
    assertEquals(0, new BigDecimal(rate).compareTo(leg.rate()));
  }

  private Transaction buyTx(
      Instrument instrument, String inputCurrency, BigDecimal fxRateUsed, BigDecimal unitPrice) {
    Transaction tx =
        Transaction.buy(
            user,
            instrument,
            null,
            unitPrice == null ? new BigDecimal("10") : unitPrice,
            new BigDecimal("1"),
            PurchaseMode.NOW,
            Instant.parse("2026-06-07T12:00:00Z"),
            unitPrice == null ? new BigDecimal("10") : unitPrice,
            TradeInputMode.LOTS,
            inputCurrency,
            new BigDecimal("100"),
            fxRateUsed,
            "NOW_BOUGHT");
    ReflectionTestUtils.setField(tx, "id", 1L);
    return tx;
  }
}
