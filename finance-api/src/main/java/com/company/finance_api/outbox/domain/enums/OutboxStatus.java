package com.company.finance_api.outbox.domain.enums;

/** OutboxStatus — domain enum sabitleri. */
public enum OutboxStatus {
  NEW,
  RETRY,
  SENT,
  DEAD
}
