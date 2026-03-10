package com.company.analytics.event;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
public class TransactionExecutedEvent {

    private UUID userId;
    private Long instrumentId;
    private String instrumentSymbol;
    private String type;
    private BigDecimal price;
    private BigDecimal quantity;
    private BigDecimal totalAmount;
    private Instant executedAt;

}