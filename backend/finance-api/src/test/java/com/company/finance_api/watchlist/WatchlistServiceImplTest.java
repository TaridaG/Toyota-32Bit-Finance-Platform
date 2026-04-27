package com.company.finance_api.watchlist;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.User;
import com.company.finance_api.domain.WatchlistItem;
import com.company.finance_api.domain.enums.Exchange;
import com.company.finance_api.domain.enums.InstrumentType;
import com.company.finance_api.repository.InstrumentRepository;
import com.company.finance_api.repository.UserRepository;
import com.company.finance_api.repository.WatchlistItemRepository;
import com.company.finance_api.security.CurrentUserResolver;
import com.company.finance_api.service.OutboxService;
import com.company.finance_api.service.impl.WatchlistServiceImpl;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatchlistServiceImplTest {

    @Mock
    WatchlistItemRepository watchlistItemRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    InstrumentRepository instrumentRepository;
    @Mock
    CurrentUserResolver currentUserResolver;
    @Mock
    OutboxService outboxService;

    WatchlistServiceImpl watchlistService;

    @BeforeEach
    void setUp() {
        watchlistService = new WatchlistServiceImpl(
                watchlistItemRepository,
                userRepository,
                instrumentRepository,
                currentUserResolver,
                outboxService,
                new SimpleMeterRegistry()
        );
    }

    @Test
    void add_new_should_save_and_publish() {
        UUID userId = UUID.randomUUID();
        User user = mock(User.class);
        Instrument instrument = new Instrument("BTCUSDT", "Bitcoin", InstrumentType.CRYPTO, Exchange.BINANCE);

        when(currentUserResolver.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(instrumentRepository.findById(1L)).thenReturn(Optional.of(instrument));
        when(watchlistItemRepository.findByUserIdAndInstrumentId(userId, 1L)).thenReturn(Optional.empty());

        watchlistService.addToWatchlist(1L);

        verify(watchlistItemRepository).save(any(WatchlistItem.class));
        verify(outboxService).enqueue(anyString(), eq(userId.toString()), any());
    }

    @Test
    void add_existing_active_should_be_idempotent() {
        UUID userId = UUID.randomUUID();
        User user = mock(User.class);
        Instrument instrument = new Instrument("BTCUSDT", "Bitcoin", InstrumentType.CRYPTO, Exchange.BINANCE);
        WatchlistItem activeItem = WatchlistItem.create(user, instrument);

        when(currentUserResolver.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(instrumentRepository.findById(1L)).thenReturn(Optional.of(instrument));
        when(watchlistItemRepository.findByUserIdAndInstrumentId(userId, 1L)).thenReturn(Optional.of(activeItem));

        watchlistService.addToWatchlist(1L);

        verify(watchlistItemRepository, never()).save(any(WatchlistItem.class));
        verify(outboxService, never()).enqueue(anyString(), anyString(), any());
    }

    @Test
    void reactivate_inactive_should_save_and_publish() {
        UUID userId = UUID.randomUUID();
        User user = mock(User.class);
        Instrument instrument = new Instrument("BTCUSDT", "Bitcoin", InstrumentType.CRYPTO, Exchange.BINANCE);
        WatchlistItem inactiveItem = WatchlistItem.create(user, instrument);
        inactiveItem.deactivate();

        when(currentUserResolver.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(instrumentRepository.findById(1L)).thenReturn(Optional.of(instrument));
        when(watchlistItemRepository.findByUserIdAndInstrumentId(userId, 1L)).thenReturn(Optional.of(inactiveItem));

        watchlistService.addToWatchlist(1L);

        verify(watchlistItemRepository).save(inactiveItem);
        verify(outboxService).enqueue(anyString(), eq(userId.toString()), any());
    }

    @Test
    void remove_active_should_deactivate_and_publish() {
        UUID userId = UUID.randomUUID();
        User user = mock(User.class);
        Instrument instrument = new Instrument("BTCUSDT", "Bitcoin", InstrumentType.CRYPTO, Exchange.BINANCE);
        WatchlistItem activeItem = WatchlistItem.create(user, instrument);

        when(currentUserResolver.getCurrentUserId()).thenReturn(userId);
        when(watchlistItemRepository.findByUserIdAndInstrumentId(userId, 1L)).thenReturn(Optional.of(activeItem));

        watchlistService.removeFromWatchlist(1L);

        verify(watchlistItemRepository).save(activeItem);
        verify(outboxService).enqueue(anyString(), eq(userId.toString()), any());
    }

    @Test
    void remove_missing_should_be_idempotent() {
        UUID userId = UUID.randomUUID();
        when(currentUserResolver.getCurrentUserId()).thenReturn(userId);
        when(watchlistItemRepository.findByUserIdAndInstrumentId(userId, 1L)).thenReturn(Optional.empty());

        watchlistService.removeFromWatchlist(1L);

        verify(watchlistItemRepository, never()).save(any(WatchlistItem.class));
        verify(outboxService, never()).enqueue(anyString(), anyString(), any());
    }

    @Test
    void get_my_watchlist_should_return_active_items() {
        UUID userId = UUID.randomUUID();
        User user = mock(User.class);
        Instrument instrument = new Instrument("BTCUSDT", "Bitcoin", InstrumentType.CRYPTO, Exchange.BINANCE);
        WatchlistItem item = WatchlistItem.create(user, instrument);

        when(currentUserResolver.getCurrentUserId()).thenReturn(userId);
        when(watchlistItemRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of(item));

        assertEquals(1, watchlistService.getMyWatchlist().size());
    }
}
