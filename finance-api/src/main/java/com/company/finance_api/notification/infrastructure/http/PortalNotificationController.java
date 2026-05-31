package com.company.finance_api.notification.infrastructure.http;

import com.company.finance_api.notification.application.PortalNotificationService;
import com.company.finance_api.notification.infrastructure.http.dto.PortalNotificationPageResponse;
import com.company.finance_api.notification.infrastructure.http.dto.PortalNotificationResponse;
import com.company.finance_api.shared.web.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** Portal bildirim inbox REST endpoint'leri (okuma, okundu işaretleme, silme). */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class PortalNotificationController {

  private final PortalNotificationService notificationService;

  /** Giriş yapmış kullanıcının bildirimlerini sayfalı döner. */
  @GetMapping
  public ApiResponse<PortalNotificationPageResponse> myNotifications(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return ApiResponse.success(notificationService.getMyPage(page, size));
  }

  /** Okunmamış bildirim sayısını döner. */
  @GetMapping("/unread-count")
  public ApiResponse<Long> unreadCount() {
    return ApiResponse.success(notificationService.getUnreadCount());
  }

  /** Tek bildirim detayını döner. */
  @GetMapping("/{id}")
  public ApiResponse<PortalNotificationResponse> getNotification(@PathVariable long id) {
    return ApiResponse.success(notificationService.getById(id));
  }

  /** Bildirimi okundu olarak işaretler. */
  @PatchMapping("/{id}/read")
  public ApiResponse<PortalNotificationResponse> markRead(@PathVariable long id) {
    return ApiResponse.success(notificationService.markRead(id));
  }

  /** Tüm bildirimleri okundu yapar. */
  @PatchMapping("/read-all")
  public ApiResponse<Void> markAllRead() {
    notificationService.markAllRead();
    return ApiResponse.success(null);
  }

  /** Bildirimi siler. */
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable long id) {
    notificationService.delete(id);
    return ApiResponse.success(null);
  }
}
