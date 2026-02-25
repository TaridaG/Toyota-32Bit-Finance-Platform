package com.company.finance_api.service.impl;

import com.company.finance_api.domain.OutboxEvent;
import com.company.finance_api.repository.OutboxEventRepository;
import com.company.finance_api.service.OutboxService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxServiceImpl implements OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void enqueue(String topic, String messageKey, Object payload) {
        try {
            String eventType = payload.getClass().getName();
            String json = objectMapper.writeValueAsString(payload);

            OutboxEvent outbox = OutboxEvent.newEvent(topic, messageKey, eventType, json);
            outboxEventRepository.save(outbox);

        } catch (JsonProcessingException e) {
            // burada fail-fast doğru: event serialize edilemiyorsa tutarlılık garanti edilemez
            throw new IllegalStateException("Outbox payload serialization failed", e);
        }
    }
}