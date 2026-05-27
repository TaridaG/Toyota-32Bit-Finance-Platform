package com.company.finance_api.auth.infrastructure.http;

import com.company.finance_api.auth.PortalTrustedDeviceService;
import com.company.finance_api.dto.PortalTrustedDevicesResponseDto;
import com.company.finance_api.shared.security.CurrentUserResolver;
import com.company.finance_api.shared.web.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Portal kullanıcısının güvenilir cihaz listesi ve iptal işlemleri. */
@RestController
@RequestMapping("/api/v1/portal/profile/trusted-devices")
public class PortalTrustedDevicesController {

  private final PortalTrustedDeviceService portalTrustedDeviceService;
  private final CurrentUserResolver currentUserResolver;

  public PortalTrustedDevicesController(
      PortalTrustedDeviceService portalTrustedDeviceService,
      CurrentUserResolver currentUserResolver) {
    this.portalTrustedDeviceService = portalTrustedDeviceService;
    this.currentUserResolver = currentUserResolver;
  }

  /** Oturum açmış kullanıcının trusted device listesini döner. */
  @GetMapping
  public ApiResponse<PortalTrustedDevicesResponseDto> list(HttpServletRequest request) {
    UUID userId = currentUserResolver.getCurrentUserId();
    return ApiResponse.success(portalTrustedDeviceService.listForUser(userId, request));
  }

  /** Belirtilen trusted device kaydını iptal eder. */
  @DeleteMapping("/{deviceId}")
  public ResponseEntity<ApiResponse<Void>> revoke(
      @PathVariable UUID deviceId, HttpServletRequest request) {
    UUID userId = currentUserResolver.getCurrentUserId();
    var clearCookie = portalTrustedDeviceService.revokeDeviceForUser(userId, deviceId, request);
    ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
    clearCookie.ifPresent(cookie -> builder.header(HttpHeaders.SET_COOKIE, cookie));
    return builder.body(ApiResponse.success(null));
  }

  /** Tüm trusted device kayıtlarını iptal eder. */
  @DeleteMapping
  public ResponseEntity<ApiResponse<Void>> revokeAll() {
    UUID userId = currentUserResolver.getCurrentUserId();
    var clearCookie = portalTrustedDeviceService.revokeAllForUserSession(userId);
    ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
    clearCookie.ifPresent(cookie -> builder.header(HttpHeaders.SET_COOKIE, cookie));
    return builder.body(ApiResponse.success(null));
  }
}
