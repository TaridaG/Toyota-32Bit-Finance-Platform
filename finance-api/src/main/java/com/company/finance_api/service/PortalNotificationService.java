package com.company.finance_api.service;

import com.company.finance_api.dto.PortalNotificationPageResponse;
import com.company.finance_api.dto.PortalNotificationResponse;

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
