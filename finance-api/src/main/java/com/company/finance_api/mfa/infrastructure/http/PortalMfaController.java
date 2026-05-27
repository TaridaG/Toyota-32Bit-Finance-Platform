package com.company.finance_api.mfa.infrastructure.http;

import com.company.finance_api.dto.PortalMfaConfirmRequest;
import com.company.finance_api.dto.PortalMfaDisableRequest;
import com.company.finance_api.dto.PortalMfaSetupResponse;
import com.company.finance_api.dto.PortalMfaStatusResponse;
import com.company.finance_api.mfa.PortalMfaService;
import com.company.finance_api.shared.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Portal TOTP MFA kurulum ve yönetim endpoint'leri. */
@RestController
@RequestMapping("/api/v1/portal/profile/mfa")
public class PortalMfaController {

  private final PortalMfaService portalMfaService;

  public PortalMfaController(PortalMfaService portalMfaService) {
    this.portalMfaService = portalMfaService;
  }

  /** MFA durumunu döner. */
  @GetMapping
  public ApiResponse<PortalMfaStatusResponse> status() {
    return ApiResponse.success(portalMfaService.getStatus());
  }

  /** MFA kurulumunu başlatır (secret + QR). */
  @PostMapping("/setup")
  public ApiResponse<PortalMfaSetupResponse> setup() {
    return ApiResponse.success(portalMfaService.beginSetup());
  }

  /** Kurulum TOTP kodunu onaylar. */
  @PostMapping("/confirm")
  public ApiResponse<PortalMfaStatusResponse> confirm(
      @Valid @RequestBody PortalMfaConfirmRequest request) {
    return ApiResponse.success(portalMfaService.confirmSetup(request.code()));
  }

  /** MFA'yı şifre ve kod ile devre dışı bırakır. */
  @PostMapping("/disable")
  public ApiResponse<PortalMfaStatusResponse> disable(
      @Valid @RequestBody PortalMfaDisableRequest request) {
    return ApiResponse.success(portalMfaService.disable(request.password(), request.code()));
  }
}
