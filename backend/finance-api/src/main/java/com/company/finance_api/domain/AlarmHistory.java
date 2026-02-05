package com.company.finance_api.domain;

import com.company.finance_api.domain.enums.AlarmCondition;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alarm_history")
@Getter
public class AlarmHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID userId;
    private String instrumentSymbol;

    @Enumerated(EnumType.STRING)
    private AlarmCondition condition;

    private BigDecimal price;
    private Instant triggeredAt;

    protected AlarmHistory() {}

    public AlarmHistory(
            UUID userId,
            String instrumentSymbol,
            AlarmCondition condition,
            BigDecimal price
    ) {
        this.userId = userId;
        this.instrumentSymbol = instrumentSymbol;
        this.condition = condition;
        this.price = price;
        this.triggeredAt = Instant.now();
    }

    // getters
}