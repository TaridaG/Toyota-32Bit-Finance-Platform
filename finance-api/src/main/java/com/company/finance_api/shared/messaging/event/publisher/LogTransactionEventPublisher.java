package com.company.finance_api.shared.messaging.event.publisher;

import com.company.finance_api.shared.messaging.event.TransactionExecutedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** LogTransactionEventPublisher — domain event'leri Kafka veya log kanalına publish eder. */
@Component
@Profile({"dev", "test"})
public class LogTransactionEventPublisher implements TransactionEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(LogTransactionEventPublisher.class);

  @Override
  public void publish(TransactionExecutedEvent event) {
    log.info(
        "TRADE_EVENT userId={}, instrument={}, type={}, price={}, qty={}, total={}",
        event.getUserId(),
        event.getInstrumentSymbol(),
        event.getType(),
        event.getPrice(),
        event.getQuantity(),
        event.getTotalAmount());
  }
}
