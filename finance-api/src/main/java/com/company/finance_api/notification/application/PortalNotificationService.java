package com.company.finance_api.notification.application;

import com.company.finance_api.notification.infrastructure.http.dto.PortalNotificationPageResponse;
import com.company.finance_api.notification.infrastructure.http.dto.PortalNotificationResponse;

/** PortalNotificationService iş mantığını uygular (portal notification service). */
public interface PortalNotificationService {

  /** Oturum açmış kullanıcının bildirim inbox'unu sayfalı döner. */
  PortalNotificationPageResponse getMyPage(int page, int size);

  /** Tek bildirim detayını döner. */
  PortalNotificationResponse getById(long id);

  /** Bildirimi okundu olarak işaretler. */
  PortalNotificationResponse markRead(long id);

  /** Tüm okunmamış bildirimleri okundu yapar. */
  void markAllRead();

  void delete(long id);

  /** Okunmamış bildirim sayısını döner. */
  long getUnreadCount();
}
