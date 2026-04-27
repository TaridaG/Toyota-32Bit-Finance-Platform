package com.company.finance_api.service.impl;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.User;
import com.company.finance_api.domain.WatchlistItem;
import com.company.finance_api.dto.WatchlistItemDto;
import com.company.finance_api.event.WatchlistItemAddedEvent;
import com.company.finance_api.event.WatchlistItemRemovedEvent;
import com.company.finance_api.event.kafka.KafkaTopics;
import com.company.finance_api.exception.ResourceNotFoundException;
import com.company.finance_api.repository.InstrumentRepository;
import com.company.finance_api.repository.UserRepository;
import com.company.finance_api.repository.WatchlistItemRepository;
import com.company.finance_api.security.CurrentUserResolver;
import com.company.finance_api.service.OutboxService;
import com.company.finance_api.service.WatchlistService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class WatchlistServiceImpl implements WatchlistService {

    private final WatchlistItemRepository watchlistItemRepository;
    private final UserRepository userRepository;
    private final InstrumentRepository instrumentRepository;
    private final CurrentUserResolver currentUserResolver;
    private final OutboxService outboxService;
    private final Counter watchlistItemAddedCounter;
    private final Counter watchlistItemRemovedCounter;

    public WatchlistServiceImpl(
            WatchlistItemRepository watchlistItemRepository,
            UserRepository userRepository,
            InstrumentRepository instrumentRepository,
            CurrentUserResolver currentUserResolver,
            OutboxService outboxService,
            MeterRegistry meterRegistry
    ) {
        this.watchlistItemRepository = watchlistItemRepository;
        this.userRepository = userRepository;
        this.instrumentRepository = instrumentRepository;
        this.currentUserResolver = currentUserResolver;
        this.outboxService = outboxService;
        this.watchlistItemAddedCounter = Counter.builder("watchlist_item_added_total")
                .tag("service", "finance-api")
                .register(meterRegistry);
        this.watchlistItemRemovedCounter = Counter.builder("watchlist_item_removed_total")
                .tag("service", "finance-api")
                .register(meterRegistry);
    }

    @Override
    public void addToWatchlist(Long instrumentId) {
        UUID userId = currentUserResolver.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        Instrument instrument = instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new ResourceNotFoundException("Instrument not found: " + instrumentId));

        WatchlistItem existingItem = watchlistItemRepository
                .findByUserIdAndInstrumentId(userId, instrumentId)
                .orElse(null);

        if (existingItem != null && existingItem.isActive()) {
            return;
        }

        WatchlistItem itemToSave;
        if (existingItem == null) {
            itemToSave = WatchlistItem.create(user, instrument);
        } else {
            existingItem.activate();
            itemToSave = existingItem;
        }

        watchlistItemRepository.save(itemToSave);
        watchlistItemAddedCounter.increment();
        outboxService.enqueue(
                KafkaTopics.WATCHLIST_ITEM_ADDED,
                userId.toString(),
                WatchlistItemAddedEvent.of(userId, instrument.getId(), instrument.getSymbol())
        );
    }

    @Override
    public void removeFromWatchlist(Long instrumentId) {
        UUID userId = currentUserResolver.getCurrentUserId();
        WatchlistItem item = watchlistItemRepository
                .findByUserIdAndInstrumentId(userId, instrumentId)
                .orElse(null);

        if (item == null || !item.isActive()) {
            return;
        }

        item.deactivate();
        watchlistItemRepository.save(item);
        watchlistItemRemovedCounter.increment();
        outboxService.enqueue(
                KafkaTopics.WATCHLIST_ITEM_REMOVED,
                userId.toString(),
                WatchlistItemRemovedEvent.of(userId, item.getInstrument().getId(), item.getInstrument().getSymbol())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<WatchlistItemDto> getMyWatchlist() {
        UUID userId = currentUserResolver.getCurrentUserId();
        return watchlistItemRepository.findByUserIdAndActiveTrue(userId)
                .stream()
                .map(item -> new WatchlistItemDto(
                        item.getInstrument().getId(),
                        item.getInstrument().getSymbol(),
                        item.getInstrument().getName(),
                        item.getInstrument().getType(),
                        item.isActive(),
                        item.getCreatedAt()
                ))
                .toList();
    }
}
