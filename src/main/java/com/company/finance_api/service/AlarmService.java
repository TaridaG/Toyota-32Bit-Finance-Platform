package com.company.finance_api.service;

import java.math.BigDecimal;
import java.util.UUID;
import com.company.finance_api.domain.AlarmRule;
import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.enums.AlarmCondition;

import java.util.List;

public interface AlarmService {

    /**
     * Yeni fiyat geldiğinde çağrılır
     */
    List<AlarmRule> checkAlarms(
            Instrument instrument,
            InstrumentPrice latestPrice
    );
    void createAlarm(
            UUID userId,
            Long instrumentId,
            AlarmCondition condition,
            BigDecimal threshold
    );
    List<AlarmRule> getActiveAlarmsForUser(UUID userId);

}
