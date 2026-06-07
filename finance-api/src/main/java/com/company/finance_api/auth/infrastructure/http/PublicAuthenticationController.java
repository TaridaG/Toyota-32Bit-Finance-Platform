package com.company.finance_api.auth.infrastructure.http;

import com.company.finance_api.auth.domain.LoginAttemptContext;
import com.company.finance_api.auth.domain.LoginCompletionResult;
import com.company.finance_api.auth.application.PortalLoginService;
import com.company.finance_api.auth.application.PublicPasswordResetService;
import com.company.finance_api.auth.infrastructure.http.dto.PublicLoginMfaRequest;
import com.company.finance_api.auth.infrastructure.http.dto.PublicLoginRequest;
import com.company.finance_api.auth.infrastructure.http.dto.PublicLoginResponse;
import com.company.finance_api.auth.infrastructure.http.dto.PublicPasswordResetRequest;
import com.company.finance_api.auth.infrastructure.http.dto.PublicRefreshRequest;
import com.company.finance_api.auth.infrastructure.http.dto.PublicSendVerificationCodeRequest;
import com.company.finance_api.auth.infrastructure.http.dto.PublicSendVerificationCodeResponse;
import com.company.finance_api.shared.web.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Kimlik doğrulama gerektirmeyen login, MFA doğrulama ve token refresh endpoint'leri. */
@RestController
@RequestMapping("/api/v1/public")
public class PublicAuthenticationController {

  private final PortalLoginService portalLoginService;
  private final PublicPasswordResetService publicPasswordResetService;

  public PublicAuthenticationController(
      PortalLoginService portalLoginService,
      PublicPasswordResetService publicPasswordResetService) {
    this.portalLoginService = portalLoginService;
    this.publicPasswordResetService = publicPasswordResetService;
  }

  /** Kullanıcı adı/şifre ile portal girişi. */
  @PostMapping("/login")
  public ResponseEntity<ApiResponse<PublicLoginResponse>> login(
      @Valid @RequestBody PublicLoginRequest request,
      @RequestHeader(value = "X-Language", required = false) String language,
      @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor,
      @RequestHeader(value = "User-Agent", required = false) String userAgent,
      HttpServletRequest httpRequest) {
    LoginAttemptContext context =
        LoginAttemptContext.from(language, forwardedFor, userAgent, httpRequest);
    return toResponse(portalLoginService.login(request, context, httpRequest));
  }

  /** MFA challenge kodunu doğrular ve oturumu tamamlar. */
  @PostMapping("/login/mfa")
  public ResponseEntity<ApiResponse<PublicLoginResponse>> verifyLoginMfa(
      @Valid @RequestBody PublicLoginMfaRequest request,
      @RequestHeader(value = "X-Language", required = false) String language,
      @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor,
      @RequestHeader(value = "User-Agent", required = false) String userAgent,
      HttpServletRequest httpRequest) {
    LoginAttemptContext context =
        LoginAttemptContext.from(language, forwardedFor, userAgent, httpRequest);
    return toResponse(portalLoginService.verifyLoginMfa(request, context));
  }

  /** Refresh token ile yeni access token üretir. */
  @PostMapping("/refresh")
  public ApiResponse<PublicLoginResponse> refresh(
      @Valid @RequestBody PublicRefreshRequest request) {
    return ApiResponse.success(portalLoginService.refresh(request));
  }

  /** Kayıtlı e-posta adresine şifre sıfırlama doğrulama kodu gönderir. */
  @PostMapping("/password/send-reset-code")
  public ApiResponse<PublicSendVerificationCodeResponse> sendPasswordResetCode(
      @Valid @RequestBody PublicSendVerificationCodeRequest request) {
    return ApiResponse.success(
        publicPasswordResetService.sendResetCode(request.getEmail(), request.getLocale()));
  }

  /** E-posta kodu ile şifreyi sıfırlar (oturum gerekmez). */
  @PostMapping("/password/reset")
  public ApiResponse<Void> resetPassword(
      @Valid @RequestBody PublicPasswordResetRequest request,
      @RequestHeader(value = "X-Language", required = false) String language,
      @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor,
      @RequestHeader(value = "User-Agent", required = false) String userAgent,
      HttpServletRequest httpRequest) {
    LoginAttemptContext context =
        LoginAttemptContext.from(language, forwardedFor, userAgent, httpRequest);
    publicPasswordResetService.resetPassword(request, context);
    return ApiResponse.success(null);
  }

  private static ResponseEntity<ApiResponse<PublicLoginResponse>> toResponse(
      LoginCompletionResult result) {
    ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
    result.setCookieHeader().ifPresent(cookie -> builder.header(HttpHeaders.SET_COOKIE, cookie));
    return builder.body(ApiResponse.success(result.response()));
  }
}
