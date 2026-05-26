package com.company.finance_api.notification.application;

import com.company.finance_api.notification.infrastructure.http.dto.PortalNotificationPageResponse;
import com.company.finance_api.notification.infrastructure.http.dto.PortalNotificationResponse;

/** PortalNotificationService iş mantığını uygular (portal notification service). */
public interface PortalNotificationService {

  /** getMyPage sözleşmesi. */
  PortalNotificationPageResponse getMyPage(int page, int size);

  /** getById sözleşmesi. */
  PortalNotificationResponse getById(long id);

  /** markRead sözleşmesi. */
  PortalNotificationResponse markRead(long id);

  /** markAllRead sözleşmesi. */
  void markAllRead();

  void delete(long id);

  /** getUnreadCount sözleşmesi. */
  long getUnreadCount();
}
