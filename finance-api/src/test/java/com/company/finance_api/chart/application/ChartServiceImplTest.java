package com.company.finance_api.chart.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.User;
import com.company.finance_api.chart.infrastructure.http.dto.CandlestickResponse;
import com.company.finance_api.domain.enums.Exchange;
import com.company.finance_api.domain.enums.InstrumentType;
import com.company.finance_api.domain.enums.PriceType;
import com.company.finance_api.alarm.infrastructure.persistence.AlarmRuleRepository;
import com.company.finance_api.repository.InstrumentPriceRepository;
import com.company.finance_api.repository.InstrumentRepository;
import com.company.finance_api.repository.TransactionRepository;
import com.company.finance_api.repository.UserRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChartServiceImplTest {

  @Mock private InstrumentPriceRepository priceRepository;
  @Mock private TransactionRepository transactionRepository;
  @Mock private AlarmRuleRepository alarmRuleRepository;
  @Mock private InstrumentRepository instrumentRepository;
  @Mock private UserRepository userRepository;
  @Mock private CurrentUserResolver currentUserResolver;

  @InjectMocks private ChartServiceImpl chartService;

  @Test
  void getCandlesticks_shouldMapPriceRows() {
    long instrumentId = 5L;
    Instrument instrument =
        new Instrument("BTCUSDT", "Bitcoin", InstrumentType.CRYPTO, Exchange.BINANCE);
    ReflectionTestUtils.setField(instrument, "id", instrumentId);
    Instant from = Instant.parse("2026-01-01T00:00:00Z");
    Instant to = Instant.parse("2026-01-02T00:00:00Z");
    InstrumentPrice price =
        new InstrumentPrice(instrument, PriceType.MARKET, new BigDecimal("42000"), from);

    when(instrumentRepository.findById(instrumentId)).thenReturn(Optional.of(instrument));
    when(priceRepository.findByInstrumentAndPriceTypeAndTimestampBetweenOrderByTimestampAsc(
            instrument, PriceType.MARKET, from, to))
        .thenReturn(List.of(price));

    List<CandlestickResponse> candles =
        chartService.getCandlesticks(instrumentId, from, to, PriceType.MARKET);

    assertEquals(1, candles.size());
    assertEquals(0, new BigDecimal("42000").compareTo(candles.get(0).close()));
  }

  @Test
  void getMyTrades_shouldFilterByInstrument() {
    UUID userId = UUID.randomUUID();
    User user = new User("trader@example.com", "trader");
    Instrument btc =
        new Instrument("BTCUSDT", "Bitcoin", InstrumentType.CRYPTO, Exchange.BINANCE);
    Instrument eth =
        new Instrument("ETHUSDT", "Ethereum", InstrumentType.CRYPTO, Exchange.BINANCE);
    ReflectionTestUtils.setField(btc, "id", 1L);
    ReflectionTestUtils.setField(eth, "id", 2L);

    when(currentUserResolver.getCurrentUserId()).thenReturn(userId);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(instrumentRepository.findById(1L)).thenReturn(Optional.of(btc));
    when(transactionRepository.findByUserOrderByCreatedAtDesc(user))
        .thenReturn(
            List.of(
                com.company.finance_api.domain.Transaction.buy(
                    user, btc, BigDecimal.TEN, BigDecimal.ONE),
                com.company.finance_api.domain.Transaction.buy(
                    user, eth, BigDecimal.ONE, BigDecimal.ONE)));

    var trades = chartService.getMyTrades(1L);

    assertEquals(1, trades.size());
    assertTrue(trades.get(0).type().name().equals("BUY"));
  }
}
