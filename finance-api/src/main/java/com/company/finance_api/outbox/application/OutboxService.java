package com.company.finance_api.outbox.application;

/** OutboxService iş mantığını uygular (outbox service). */
public interface OutboxService {
  /** Outbox tablosuna event kaydı ekler; scheduler Kafka'ya publish eder. */
  void enqueue(String topic, String messageKey, Object payload);
}
