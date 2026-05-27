package com.company.finance_api.profile.infrastructure.http;

import com.company.finance_api.auth.LoginAttemptContext;
import com.company.finance_api.dto.PortalChangePasswordRequest;
import com.company.finance_api.dto.PortalChangeUsernameRequest;
import com.company.finance_api.dto.PortalConfirmEmailChangeRequest;
import com.company.finance_api.dto.PortalDeleteAccountRequest;
import com.company.finance_api.dto.PortalEmailChangeRequest;
import com.company.finance_api.dto.PortalForgotPasswordResetRequest;
import com.company.finance_api.dto.PortalProfileResponse;
import com.company.finance_api.dto.PortalUpdateNotificationsRequest;
import com.company.finance_api.dto.PortalUpdatePhoneRequest;
import com.company.finance_api.dto.PortalUpdatePreferencesRequest;
import com.company.finance_api.dto.PublicEmailAvailabilityResponse;
import com.company.finance_api.dto.PublicLoginResponse;
import com.company.finance_api.dto.PublicSendVerificationCodeResponse;
import com.company.finance_api.dto.PublicUsernameAvailabilityResponse;
import com.company.finance_api.profile.PortalProfileService;
import com.company.finance_api.shared.web.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Portal profil REST API: tercihler, avatar, şifre, e-posta ve hesap silme. */
@RestController
@RequestMapping("/api/v1/portal/profile")
public class PortalProfileController {

  private static final Logger log = LoggerFactory.getLogger(PortalProfileController.class);

  private final PortalProfileService portalProfileService;

  public PortalProfileController(PortalProfileService portalProfileService) {
    this.portalProfileService = portalProfileService;
  }

  /** Profil özetini döner. */
  @GetMapping
  public ApiResponse<PortalProfileResponse> getProfile() {
    return ApiResponse.success(portalProfileService.getProfile());
  }

  /** Oturumu kapatır ve trusted device cookie'sini temizler. */
  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> logout() {
    var cookie = portalProfileService.logoutCurrentSession();
    ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
    cookie.ifPresent(c -> builder.header(HttpHeaders.SET_COOKIE, c));
    return builder.body(ApiResponse.success(null));
  }

  /** Oturum açmış kullanıcının şifresini değiştirir. */
  @PostMapping("/password")
  public ApiResponse<Void> changePassword(
      @Valid @RequestBody PortalChangePasswordRequest request,
      @RequestHeader(value = "X-Language", required = false) String language,
      @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor,
      @RequestHeader(value = "User-Agent", required = false) String userAgent,
      HttpServletRequest httpRequest) {
    LoginAttemptContext context =
        LoginAttemptContext.from(language, forwardedFor, userAgent, httpRequest);
    portalProfileService.changePassword(request, context);
    return ApiResponse.success(null);
  }

  /** Şifre sıfırlama doğrulama kodu gönderir. */
  @PostMapping("/password/send-reset-code")
  public ApiResponse<PublicSendVerificationCodeResponse> sendPasswordResetCode() {
    return ApiResponse.success(portalProfileService.sendPasswordResetCode());
  }

  /** E-posta kodu ile şifre sıfırlar. */
  @PostMapping("/password/reset-forgot")
  public ApiResponse<Void> resetPasswordForgot(
      @Valid @RequestBody PortalForgotPasswordResetRequest request,
      @RequestHeader(value = "X-Language", required = false) String language,
      @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor,
      @RequestHeader(value = "User-Agent", required = false) String userAgent,
      HttpServletRequest httpRequest) {
    LoginAttemptContext context =
        LoginAttemptContext.from(language, forwardedFor, userAgent, httpRequest);
    portalProfileService.resetPasswordWithEmailVerification(request, context);
    return ApiResponse.success(null);
  }

  /** Yeni e-posta için doğrulama kodu gönderir. */
  @PostMapping("/email/send-code")
  public ApiResponse<PublicSendVerificationCodeResponse> sendEmailChangeCode(
      @Valid @RequestBody PortalEmailChangeRequest request) {
    return ApiResponse.success(portalProfileService.sendEmailChangeCode(request));
  }

  /** Doğrulama kodu ile e-posta değişikliğini onaylar. */
  @PostMapping("/email/confirm")
  public ApiResponse<PortalProfileResponse> confirmEmailChange(
      @Valid @RequestBody PortalConfirmEmailChangeRequest request,
      @RequestHeader(value = "X-Language", required = false) String language,
      @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor,
      @RequestHeader(value = "User-Agent", required = false) String userAgent,
      HttpServletRequest httpRequest) {
    LoginAttemptContext context =
        LoginAttemptContext.from(language, forwardedFor, userAgent, httpRequest);
    return ApiResponse.success(portalProfileService.confirmEmailChange(request, context));
  }

  /** Görünen kullanıcı adı müsaitliğini sorgular. */
  @GetMapping("/username-availability")
  public ApiResponse<PublicUsernameAvailabilityResponse> checkUsernameAvailability(
      @RequestParam("username") String username) {
    return ApiResponse.success(portalProfileService.checkUsernameAvailability(username));
  }

  /** E-posta müsaitliğini sorgular. */
  @GetMapping("/email-availability")
  public ApiResponse<PublicEmailAvailabilityResponse> checkEmailAvailability(
      @RequestParam("email") String email) {
    return ApiResponse.success(portalProfileService.checkEmailAvailability(email));
  }

  /** Görünen kullanıcı adını günceller. */
  @PostMapping("/username")
  public ApiResponse<PublicLoginResponse> changeUsername(
      @Valid @RequestBody PortalChangeUsernameRequest request,
      @RequestHeader(value = "X-Language", required = false) String language,
      @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor,
      @RequestHeader(value = "User-Agent", required = false) String userAgent,
      HttpServletRequest httpRequest) {
    LoginAttemptContext context =
        LoginAttemptContext.from(language, forwardedFor, userAgent, httpRequest);
    return ApiResponse.success(portalProfileService.changeUsername(request, context));
  }

  /** Telefon numarasını günceller. */
  @PutMapping("/phone")
  public ApiResponse<PortalProfileResponse> updatePhone(
      @Valid @RequestBody PortalUpdatePhoneRequest request) {
    return ApiResponse.success(portalProfileService.updatePhone(request));
  }

  /** Bildirim tercihlerini günceller. */
  @PutMapping("/notifications")
  public ApiResponse<PortalProfileResponse> updateNotifications(
      @Valid @RequestBody PortalUpdateNotificationsRequest request) {
    return ApiResponse.success(portalProfileService.updateNotifications(request));
  }

  /** Locale ve para birimi tercihlerini günceller. */
  @PutMapping("/preferences")
  public ApiResponse<PortalProfileResponse> updatePreferences(
      @Valid @RequestBody PortalUpdatePreferencesRequest request) {
    return ApiResponse.success(portalProfileService.updatePreferences(request));
  }

  /** Avatar JPEG görüntüsünü döner. */
  @GetMapping(value = "/avatar", produces = MediaType.IMAGE_JPEG_VALUE)
  public ResponseEntity<byte[]> getAvatar() {
    byte[] body = portalProfileService.readAvatarForCurrentUser();
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
        .contentType(MediaType.IMAGE_JPEG)
        .body(body);
  }

  /** Avatar yükler (multipart POST); bazı proxy'ler PUT multipart'ı güvenilir işlemez. */
  @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<PortalProfileResponse> uploadAvatar(@RequestPart("file") MultipartFile file) {
    try {
      return ApiResponse.success(portalProfileService.uploadAvatar(file));
    } catch (ResponseStatusException e) {
      throw e;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      log.error("Avatar upload failed (checked exception)", e);
      String detail = e.getClass().getSimpleName();
      if (StringUtils.hasText(e.getMessage())) {
        detail += ": " + e.getMessage();
      }
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, detail, e);
    }
  }

  /** Avatarı siler. */
  @DeleteMapping("/avatar")
  public ApiResponse<PortalProfileResponse> deleteAvatar() {
    return ApiResponse.success(portalProfileService.deleteAvatar());
  }

  /** Hesap silme talebini başlatır. */
  @PostMapping("/delete-account")
  public ApiResponse<Void> requestDeleteAccount(
      @Valid @RequestBody PortalDeleteAccountRequest request) {
    portalProfileService.requestDeleteAccount(request);
    return ApiResponse.success(null);
  }
}
