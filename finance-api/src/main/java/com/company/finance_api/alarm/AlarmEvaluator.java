package com.company.finance_api.alarm;

import com.company.finance_api.alarm.domain.AlarmRule;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.enums.AlarmCondition;

/** Fiyat alarmı koşulunu son fiyata göre değerlendiren strategy arayüzü. */
public interface AlarmEvaluator {

  /** Desteklenen {@link AlarmCondition} değeri. */
  AlarmCondition supports();

  /** Alarm kuralının tetiklenip tetiklenmediğini döner. */
  boolean evaluate(AlarmRule alarm, InstrumentPrice latestPrice);
}
