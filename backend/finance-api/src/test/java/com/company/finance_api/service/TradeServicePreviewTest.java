package com.company.finance_api.service;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.enums.Exchange;
import com.company.finance_api.domain.enums.InstrumentType;
import com.company.finance_api.domain.enums.PriceType;
import com.company.finance_api.domain.enums.PurchaseMode;
import com.company.finance_api.domain.enums.TradeInputMode;
import com.company.finance_api.dto.TradeExecutionRequest;
import com.company.finance_api.dto.TradePreviewResponse;
import com.company.finance_api.event.publisher.TransactionEventPublisher;
import com.company.finance_api.portfolio.external.repository.ExternalPortfolioRepository;
import com.company.finance_api.repository.InstrumentPriceRepository;
import com.company.finance_api.repository.InstrumentRepository;
import com.company.finance_api.repository.TransactionRepository;
import com.company.finance_api.repository.UserRepository;
import com.company.finance_api.security.CurrentUserResolver;
import com.company.finance_api.service.impl.TradeServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeServicePreviewTest {

    @Mock
    InstrumentRepository instrumentRepository;
    @Mock
    TransactionRepository transactionRepository;
    @Mock
    InstrumentPriceRepository instrumentPriceRepository;
    @Mock
    JdbcTemplate jdbcTemplate;
    @Mock
    CurrencyConversionService currencyConversionService;
    @Mock
    CurrentUserResolver currentUserResolver;
    @Mock
    UserRepository userRepository;
    @Mock
    ExternalPortfolioRepository externalPortfolioRepository;
    @Mock
    TransactionEventPublisher transactionEventPublisher;

    @InjectMocks
    TradeServiceImpl tradeService;

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

        InstrumentPrice market = new InstrumentPrice(g, PriceType.MARKET, new BigDecimal("184.50"), Instant.now());

        when(instrumentRepository.findById(instrumentId)).thenReturn(Optional.of(g));
        when(instrumentPriceRepository.findTopByInstrumentAndPriceTypeOrderByTimestampDesc(g, PriceType.MARKET))
                .thenReturn(Optional.of(market));
        when(currencyConversionService.normalizeCurrency("TRY")).thenReturn("TRY");
        when(currencyConversionService.convert(eq(BigDecimal.ONE), eq("TRY"), eq("TRY"))).thenReturn(BigDecimal.ONE);

        TradePreviewResponse preview = tradeService.preview(request);

        assertEquals(0, new BigDecimal("184.50").compareTo(preview.unitPriceUsed()));
        verify(currencyConversionService, never()).convert(any(), eq("USD"), eq("TRY"));
    }
}
