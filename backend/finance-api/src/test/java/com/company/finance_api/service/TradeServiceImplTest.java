package com.company.finance_api.service;

import com.company.finance_api.domain.*;
import com.company.finance_api.domain.enums.PriceType;
import com.company.finance_api.event.publisher.TransactionEventPublisher;
import com.company.finance_api.repository.*;
import com.company.finance_api.security.CurrentUserResolver;
import com.company.finance_api.service.impl.TradeServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TradeServiceImplTest {

    @Mock InstrumentRepository instrumentRepository;
    @Mock DemoBalanceRepository demoBalanceRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock PriceService priceService;
    @Mock CurrentUserResolver currentUserResolver;
    @Mock UserRepository userRepository;
    @Mock TransactionEventPublisher transactionEventPublisher;

    @InjectMocks
    TradeServiceImpl tradeService;

    @Test
    void buy_shouldSucceed_whenBalanceIsEnough() {

        UUID userId = UUID.randomUUID();

        User user = mock(User.class);
        Instrument instrument = mock(Instrument.class);

        InstrumentPrice price =
                new InstrumentPrice(instrument, PriceType.MARKET,
                        BigDecimal.valueOf(100), Instant.now());

        DemoBalance balance = mock(DemoBalance.class);

        when(currentUserResolver.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(instrumentRepository.findById(1L)).thenReturn(Optional.of(instrument));
        when(priceService.getLatestValuationPrice(instrument))
                .thenReturn(Optional.of(price));
        when(demoBalanceRepository.findByUser(user)).thenReturn(Optional.of(balance));
        when(balance.getBalance()).thenReturn(BigDecimal.valueOf(1000));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Transaction result = tradeService.buy(1L, BigDecimal.ONE);

        assertNotNull(result);
        verify(balance).decrease(BigDecimal.valueOf(100));
        verify(transactionEventPublisher).publish(any());
    }

    @Test
    void buy_shouldFail_whenBalanceIsInsufficient() {

        UUID userId = UUID.randomUUID();

        User user = mock(User.class);
        Instrument instrument = mock(Instrument.class);

        InstrumentPrice price =
                new InstrumentPrice(instrument, PriceType.MARKET,
                        BigDecimal.valueOf(1000), Instant.now());

        DemoBalance balance = mock(DemoBalance.class);

        when(currentUserResolver.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(instrumentRepository.findById(1L)).thenReturn(Optional.of(instrument));
        when(priceService.getLatestValuationPrice(instrument))
                .thenReturn(Optional.of(price));
        when(demoBalanceRepository.findByUser(user)).thenReturn(Optional.of(balance));
        when(balance.getBalance()).thenReturn(BigDecimal.valueOf(10));

        assertThrows(IllegalStateException.class,
                () -> tradeService.buy(1L, BigDecimal.ONE));
    }

    @Test
    void sell_shouldFail_whenPositionIsInsufficient() {

        UUID userId = UUID.randomUUID();

        User user = mock(User.class);
        Instrument instrument = mock(Instrument.class);

        when(currentUserResolver.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(instrumentRepository.findById(1L)).thenReturn(Optional.of(instrument));
        when(transactionRepository.findByUserAndInstrument(user, instrument))
                .thenReturn(Collections.emptyList());

        assertThrows(IllegalStateException.class,
                () -> tradeService.sell(1L, BigDecimal.ONE));
    }
}
