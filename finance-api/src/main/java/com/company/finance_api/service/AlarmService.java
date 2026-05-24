package com.company.finance_api.service;

import com.company.finance_api.domain.AlarmRule;
import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.enums.AlarmCondition;
import com.company.finance_api.dto.AlarmResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Kullanıcı alarm kurallarının oluşturulması, listelenmesi ve fiyat güncellemesinde
 * değerlendirilmesi.
 */
public interface AlarmService {

  /** Yeni fiyat snapshot'ı geldiğinde tetiklenen alarm kurallarını değerlendirir. */
  List<AlarmRule> checkAlarms(Instrument instrument, InstrumentPrice latestPrice);

  /** Alarm kuralını pasifleştirir. */
  void deactivateAlarm(Long alarmId, UUID currentUserId);

  /** Kullanıcı için yeni alarm kuralı oluşturur. */
  void createAlarm(UUID userId, Long instrumentId, AlarmCondition condition, BigDecimal threshold);

  /** Kullanıcının aktif alarm kurallarını döner. */
  List<AlarmRule> getActiveAlarmsForUser(UUID userId);

  /** Kullanıcının alarm listesini API response olarak döner. */
  List<AlarmResponse> getUserAlarms(UUID userId);
}
