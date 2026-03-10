package com.company.analytics.application.impl;

import com.company.analytics.application.EventIdempotencyService;
import com.company.analytics.domain.ProcessedAnalyticsEvent;
import com.company.analytics.infrastructure.persistence.ProcessedAnalyticsEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class EventIdempotencyServiceImpl implements EventIdempotencyService {

    private final ProcessedAnalyticsEventRepository repository;

    @Override
    public boolean isProcessed(String eventKey) {
        return repository.findByEventKey(eventKey).isPresent();
    }

    @Override
    public void markProcessed(String eventKey) {

        ProcessedAnalyticsEvent e = new ProcessedAnalyticsEvent();

        e.setEventKey(eventKey);
        e.setProcessedAt(Instant.now());

        repository.save(e);
    }
}