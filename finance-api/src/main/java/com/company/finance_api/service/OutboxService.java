package com.company.finance_api.service;

/** OutboxService iş mantığını uygular (outbox service). */
public interface OutboxService {
  /** enqueue sözleşmesi. */
  void enqueue(String topic, String messageKey, Object payload);
}
