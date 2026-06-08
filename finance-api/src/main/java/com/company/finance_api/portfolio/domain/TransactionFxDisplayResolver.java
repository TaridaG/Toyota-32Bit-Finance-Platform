package com.company.finance_api.portfolio.domain;

import com.company.finance_api.instrument.domain.Instrument;
import com.company.finance_api.instrument.domain.enums.InstrumentType;
import com.company.finance_api.portfolio.domain.enums.PurchaseMode;
import com.company.finance_api.pricing.application.CurrencyConversionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Resolves the FX leg shown in transaction history ("Alım kuru").
 *
 * <p>Prefers TRY-hub convention ({@code 1 USD = X TRY}) when TRY is involved; uses persisted
 * acquisition snapshot when available.
 */
@Component
public class TransactionFxDisplayResolver {

  private static final Set<String> SUPPORTED_PAYMENT = Set.of("TRY", "USD", "EUR", "GBP", "JPY", "AED");

  private final CurrencyConversionService currencyConversionService;

  public TransactionFxDisplayResolver(CurrencyConversionService currencyConversionService) {
    this.currencyConversionService = currencyConversionService;
  }

  public FxDisplayLeg resolve(Transaction tx, TransactionAcquisitionFx acquisitionFx) {
    if (tx == null || tx.getInstrument() == null) {
      return null;
    }
    Instrument instrument = tx.getInstrument();
    String listingCurrency = InstrumentListingCurrency.resolve(instrument);
    String paymentCurrency = normalizePaymentCurrency(tx.getInputCurrency());
    if (paymentCurrency == null) {
      paymentCurrency = "TRY";
    }

    if (isDomesticTryStock(instrument, listingCurrency)) {
      return null;
    }

    if (isTryListedFxOrMetal(instrument, listingCurrency)) {
      return resolveTryListedFxOrMetal(tx, acquisitionFx, instrument);
    }

    if (paymentCurrency != null
        && !paymentCurrency.equals(listingCurrency)
        && isPositive(tx.getFxRateUsed())) {
      return resolveCrossCurrency(paymentCurrency, listingCurrency, tx.getFxRateUsed(), acquisitionFx);
    }

    if (paymentCurrency != null && !paymentCurrency.equals(listingCurrency)) {
      return resolveCrossCurrencyFallback(tx, paymentCurrency, listingCurrency);
    }

    return null;
  }

  private FxDisplayLeg resolveTryListedFxOrMetal(
      Transaction tx, TransactionAcquisitionFx acquisitionFx, Instrument instrument) {
    String base = extractTryPairBase(symbolUpper(instrument));
    if (base == null) {
      return null;
    }
    BigDecimal rate = hubTryRate(acquisitionFx, base);
    if (!isPositive(rate)) {
      rate = positiveUnitPrice(tx);
    }
    if (!isPositive(rate)) {
      return null;
    }
    return new FxDisplayLeg(base, "TRY", rate);
  }

  private FxDisplayLeg resolveCrossCurrency(
      String paymentCurrency,
      String listingCurrency,
      BigDecimal fxRateUsed,
      TransactionAcquisitionFx acquisitionFx) {
    if ("TRY".equals(paymentCurrency) || "TRY".equals(listingCurrency)) {
      String foreign = "TRY".equals(listingCurrency) ? paymentCurrency : listingCurrency;
      BigDecimal tryPerForeign = hubTryRate(acquisitionFx, foreign);
      if (!isPositive(tryPerForeign) && "TRY".equals(paymentCurrency)) {
        tryPerForeign = invertRate(fxRateUsed);
      } else if (!isPositive(tryPerForeign) && "TRY".equals(listingCurrency)) {
        tryPerForeign = fxRateUsed;
      }
      if (isPositive(tryPerForeign)) {
        return new FxDisplayLeg(foreign, "TRY", tryPerForeign);
      }
    }
    return new FxDisplayLeg(paymentCurrency, listingCurrency, fxRateUsed);
  }

  private FxDisplayLeg resolveCrossCurrencyFallback(
      Transaction tx, String paymentCurrency, String listingCurrency) {
    Instant anchor = effectiveFxAnchor(tx);
    if (anchor == null) {
      return null;
    }
    boolean historical = tx.getPurchaseMode() == PurchaseMode.PAST;
    BigDecimal fxRate =
        historical
            ? currencyConversionService.convertAt(
                anchor, BigDecimal.ONE, paymentCurrency, listingCurrency)
            : currencyConversionService.convert(BigDecimal.ONE, paymentCurrency, listingCurrency);
    if (!isPositive(fxRate)) {
      return null;
    }
    return resolveCrossCurrency(paymentCurrency, listingCurrency, fxRate, null);
  }

  private static Instant effectiveFxAnchor(Transaction tx) {
    Instant acquired = tx.getAcquiredAt();
    if (acquired != null) {
      return FxHistoricalAnchor.normalizeEndOfAcquisitionDay(acquired);
    }
    if (tx.getCreatedAt() != null) {
      return FxHistoricalAnchor.normalizeEndOfAcquisitionDay(tx.getCreatedAt());
    }
    return null;
  }

  private static boolean isDomesticTryStock(Instrument instrument, String listingCurrency) {
    if (!"TRY".equals(listingCurrency) || instrument.getType() != InstrumentType.STOCK) {
      return false;
    }
    return !symbolUpper(instrument).endsWith("TRY");
  }

  private static boolean isTryListedFxOrMetal(Instrument instrument, String listingCurrency) {
    if (!"TRY".equals(listingCurrency)) {
      return false;
    }
    if (instrument.getType() == InstrumentType.FX) {
      return true;
    }
    String symbol = symbolUpper(instrument);
    return symbol.endsWith("TRY") && symbol.length() > 3;
  }

  private static String extractTryPairBase(String symbol) {
    if (symbol == null || !symbol.endsWith("TRY") || symbol.length() <= 3) {
      return null;
    }
    return symbol.substring(0, symbol.length() - 3);
  }

  private static BigDecimal hubTryRate(TransactionAcquisitionFx snap, String baseCurrency) {
    if (snap == null || baseCurrency == null) {
      return null;
    }
    return switch (baseCurrency) {
      case "USD" -> snap.getUsdTry();
      case "EUR" -> snap.getEurTry();
      case "GBP" -> snap.getGbpTry();
      case "JPY" -> snap.getJpyTry();
      case "AED" -> snap.getAedTry();
      default -> null;
    };
  }

  private static BigDecimal positiveUnitPrice(Transaction tx) {
    BigDecimal unitPrice = tx.getUnitPrice() != null ? tx.getUnitPrice() : tx.getPrice();
    return isPositive(unitPrice) ? unitPrice : null;
  }

  private static BigDecimal invertRate(BigDecimal rate) {
    if (!isPositive(rate)) {
      return null;
    }
    return BigDecimal.ONE.divide(rate, 10, RoundingMode.HALF_UP);
  }

  private static String normalizePaymentCurrency(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String normalized = raw.trim().toUpperCase(Locale.ROOT);
    if ("USDT".equals(normalized)) {
      normalized = "USD";
    }
    return SUPPORTED_PAYMENT.contains(normalized) ? normalized : null;
  }

  private static String symbolUpper(Instrument instrument) {
    return instrument.getSymbol() == null
        ? ""
        : instrument.getSymbol().trim().toUpperCase(Locale.ROOT);
  }

  private static boolean isPositive(BigDecimal value) {
    return value != null && value.compareTo(BigDecimal.ZERO) > 0;
  }
}
