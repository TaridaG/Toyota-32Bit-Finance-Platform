package com.company.finance_api.outbox.application;

/** OutboxService iş mantığını uygular (outbox service). */
public interface OutboxService {
  /** enqueue sözleşmesi. */
  void enqueue(String topic, String messageKey, Object payload);
}
