package com.company.finance_api.notification.application;

import com.company.finance_api.alarm.domain.AlarmHistory;
import com.company.finance_api.profile.domain.User;
import com.company.finance_api.notification.domain.enums.NotificationType;
import com.company.finance_api.alarm.infrastructure.persistence.AlarmHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Portal kullanıcı inbox bildirimlerini alarm_history üzerinden persist eder. */
@Service
public class PortalInboxNotificationService {

  private final AlarmHistoryRepository alarmHistoryRepository;

  public PortalInboxNotificationService(AlarmHistoryRepository alarmHistoryRepository) {
    this.alarmHistoryRepository = alarmHistoryRepository;
  }

  /** Kullanıcıya sistem bildirimi kaydı oluşturur. */
  @Transactional
  public void deliver(User user, NotificationType type, String title, String body) {
    if (user == null) {
      return;
    }
    alarmHistoryRepository.save(AlarmHistory.system(user.getId(), type, title, body));
  }
}
