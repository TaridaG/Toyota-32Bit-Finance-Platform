package com.company.analytics.processing.application;

import com.company.analytics.processing.application.EventIdempotencyService;
import com.company.analytics.processing.domain.ProcessedAnalyticsEvent;
import com.company.analytics.processing.infrastructure.persistence.ProcessedAnalyticsEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

/** İşlenmiş analytics event kayıtlarını veritabanında tutarak idempotency sağlayan servis. */
@Service
@RequiredArgsConstructor
public class EventIdempotencyServiceImpl implements EventIdempotencyService {

    private final ProcessedAnalyticsEventRepository repository;

    /** Event key için daha önce kayıt oluşturulup oluşturulmadığını kontrol eder. */
    @Override
    public boolean isProcessed(String eventKey) {
        return repository.findByEventKey(eventKey).isPresent();
    }

    /** Event key'i işlenmiş olarak veritabanına kaydeder. */
    @Override
    public void markProcessed(String eventKey) {

        ProcessedAnalyticsEvent e = new ProcessedAnalyticsEvent();

        e.setEventKey(eventKey);
        e.setProcessedAt(Instant.now());

        repository.save(e);
    }
}