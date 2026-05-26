package com.company.finance_api.notification.infrastructure.http.dto;

import java.util.List;

/** PortalNotificationPageResponse — API transfer nesnesi (DTO/response/request). */
public record PortalNotificationPageResponse(
    List<PortalNotificationResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    long unreadCount) {}
