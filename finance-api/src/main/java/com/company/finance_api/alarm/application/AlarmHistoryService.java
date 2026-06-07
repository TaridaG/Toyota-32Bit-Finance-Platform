package com.company.finance_api.alarm.application;

import com.company.finance_api.alarm.infrastructure.http.dto.AlarmHistoryResponse;
import java.util.List;

/** AlarmHistoryService iş mantığını uygular (alarm history service). */
public interface AlarmHistoryService {
  /** Oturum açmış kullanıcının tetiklenmiş alarm geçmişini kronolojik döner. */
  List<AlarmHistoryResponse> getMyTimeline();
}
