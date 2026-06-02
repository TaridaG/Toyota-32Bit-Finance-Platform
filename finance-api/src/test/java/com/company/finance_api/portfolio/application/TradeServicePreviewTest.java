package com.company.finance_api.portfolio.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.finance_api.instrument.domain.Instrument;
import com.company.finance_api.pricing.domain.InstrumentPrice;
import com.company.finance_api.instrument.domain.enums.Exchange;
import com.company.finance_api.instrument.domain.enums.InstrumentType;
import com.company.finance_api.pricing.domain.enums.PriceType;
import com.company.finance_api.portfolio.domain.enums.PurchaseMode;
import com.company.finance_api.portfolio.domain.enums.TradeInputMode;
import com.company.finance_api.portfolio.infrastructure.http.dto.AcquisitionFxRatesSnapshot;
import com.company.finance_api.portfolio.infrastructure.http.dto.TradeExecutionRequest;
import com.company.finance_api.portfolio.infrastructure.http.dto.TradePreviewResponse;
import com.company.finance_api.shared.messaging.event.publisher.TransactionEventPublisher;
import com.company.finance_api.portfolio.external.infrastructure.persistence.ExternalPortfolioRepository;
import com.company.finance_api.pricing.infrastructure.persistence.InstrumentPriceRepository;
import com.company.finance_api.instrument.infrastructure.persistence.InstrumentRepository;
import com.company.finance_api.portfolio.infrastructure.persistence.TransactionRepository;
import com.company.finance_api.profile.infrastructure.persistence.UserRepository;
import com.company.finance_api.pricing.application.CurrencyConversionService;
import com.company.finance_api.shared.security.CurrentUserResolver;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TradeServicePreviewTest {

  @Mock InstrumentRepository instrumentRepository;
  @Mock TransactionRepository transactionRepository;
  @Mock InstrumentPriceRepository instrumentPriceRepository;
  @Mock JdbcTemplate jdbcTemplate;
  @Mock CurrencyConversionService currencyConversionService;
  @Mock CurrentUserResolver currentUserResolver;
  @Mock UserRepository userRepository;
  @Mock ExternalPortfolioRepository externalPortfolioRepository;
  @Mock TransactionEventPublisher transactionEventPublisher;

  @InjectMocks TradeServiceImpl tradeService;

  @Test
  void preview_liveBist_tryInput_usesRawTryMarketPriceWithoutUsdTryRequote() {
    long instrumentId = 901L;
    Instrument g = new Instrument("GARAN", "Garanti BBVA", InstrumentType.STOCK, Exchange.BIST);
    ReflectionTestUtils.setField(g, "id", instrumentId);

    TradeExecutionRequest request = new TradeExecutionRequest();
    request.setInstrumentId(instrumentId);
    request.setInputMode(TradeInputMode.LOTS);
    request.setLots(new BigDecimal("1"));
    request.setInputCurrency("TRY");
    request.setPurchaseMode(PurchaseMode.NOW);

    InstrumentPrice market =
        new InstrumentPrice(g, PriceType.MARKET, new BigDecimal("184.50"), Instant.now());

    when(instrumentRepository.findByIdAndActiveTrue(instrumentId)).thenReturn(Optional.of(g));
    when(instrumentPriceRepository.findTopByInstrumentAndPriceTypeOrderByTimestampDesc(
            g, PriceType.MARKET))
        .thenReturn(Optional.of(market));
    when(currencyConversionService.normalizeCurrency("TRY")).thenReturn("TRY");
    when(currencyConversionService.convert(eq(BigDecimal.ONE), eq("TRY"), eq("TRY")))
        .thenReturn(BigDecimal.ONE);
    when(currencyConversionService.acquisitionFxHubSnapshot(any(), eq(false)))
        .thenReturn(
            new AcquisitionFxRatesSnapshot(
                Instant.parse("2020-01-01T00:00:00Z").toString(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null));

    TradePreviewResponse preview = tradeService.preview(request);

    assertEquals(0, new BigDecimal("184.50").compareTo(preview.unitPriceUsed()));
    verify(currencyConversionService, never()).convert(any(), eq("USD"), eq("TRY"));
  }
}
