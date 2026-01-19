package com.company.finance_api.service;

import com.company.finance_api.domain.AlarmRule;
import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.InstrumentPrice;

import java.util.List;

public interface AlarmService {

    /**
     * Yeni fiyat geldiğinde çağrılır
     */
    List<AlarmRule> checkAlarms(
            Instrument instrument,
            InstrumentPrice latestPrice
    );
}
