package com.company.finance_api.registration.infrastructure.http;

import com.company.finance_api.auth.infrastructure.http.dto.PublicEmailAvailabilityResponse;
import com.company.finance_api.auth.infrastructure.http.dto.PublicRegisterRequest;
import com.company.finance_api.auth.infrastructure.http.dto.PublicRegisterResponse;
import com.company.finance_api.auth.infrastructure.http.dto.PublicSendVerificationCodeRequest;
import com.company.finance_api.auth.infrastructure.http.dto.PublicSendVerificationCodeResponse;
import com.company.finance_api.auth.infrastructure.http.dto.PublicUsernameAvailabilityResponse;
import com.company.finance_api.registration.application.PortalRegistrationService;
import com.company.finance_api.registration.application.RegistrationEmailVerificationService;
import com.company.finance_api.shared.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Kimlik doğrulama gerektirmeyen kayıt, doğrulama kodu ve müsaitlik endpoint'leri. */
@RestController
@RequestMapping("/api/v1/public")
public class PublicRegistrationController {

  private final PortalRegistrationService portalRegistrationService;
  private final RegistrationEmailVerificationService registrationEmailVerificationService;

  public PublicRegistrationController(
      PortalRegistrationService portalRegistrationService,
      RegistrationEmailVerificationService registrationEmailVerificationService) {
    this.portalRegistrationService = portalRegistrationService;
    this.registrationEmailVerificationService = registrationEmailVerificationService;
  }

  /** Kayıt e-postasına doğrulama kodu gönderir. */
  @PostMapping("/register/send-code")
  public ApiResponse<PublicSendVerificationCodeResponse> sendCode(
      @Valid @RequestBody PublicSendVerificationCodeRequest request) {
    return ApiResponse.success(
        registrationEmailVerificationService.sendCode(request.getEmail(), request.getLocale()));
  }

  /** Doğrulanmış bilgilerle yeni portal hesabı oluşturur. */
  @PostMapping("/register")
  public ApiResponse<PublicRegisterResponse> register(
      @Valid @RequestBody PublicRegisterRequest request) {
    return ApiResponse.success(portalRegistrationService.register(request));
  }

  /** Kullanıcı adı müsaitliğini sorgular. */
  @GetMapping("/register/username-availability")
  public ApiResponse<PublicUsernameAvailabilityResponse> checkUsernameAvailability(
      @RequestParam("username") String username) {
    return ApiResponse.success(portalRegistrationService.checkUsernameAvailability(username));
  }

  /** E-posta müsaitliğini sorgular. */
  @GetMapping("/register/email-availability")
  public ApiResponse<PublicEmailAvailabilityResponse> checkEmailAvailability(
      @RequestParam("email") String email) {
    return ApiResponse.success(portalRegistrationService.checkEmailAvailability(email));
  }
}
