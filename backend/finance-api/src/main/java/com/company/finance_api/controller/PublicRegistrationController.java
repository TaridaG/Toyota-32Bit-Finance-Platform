package com.company.finance_api.controller;

import com.company.finance_api.common.ApiResponse;
import com.company.finance_api.dto.PublicRegisterRequest;
import com.company.finance_api.dto.PublicRegisterResponse;
import com.company.finance_api.dto.PublicSendVerificationCodeRequest;
import com.company.finance_api.dto.PublicSendVerificationCodeResponse;
import com.company.finance_api.dto.PublicUsernameAvailabilityResponse;
import com.company.finance_api.registration.PortalRegistrationService;
import com.company.finance_api.registration.RegistrationEmailVerificationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicRegistrationController {

    private final PortalRegistrationService portalRegistrationService;
    private final RegistrationEmailVerificationService registrationEmailVerificationService;

    public PublicRegistrationController(
            PortalRegistrationService portalRegistrationService,
            RegistrationEmailVerificationService registrationEmailVerificationService
    ) {
        this.portalRegistrationService = portalRegistrationService;
        this.registrationEmailVerificationService = registrationEmailVerificationService;
    }

    @PostMapping("/register/send-code")
    public ApiResponse<PublicSendVerificationCodeResponse> sendCode(@Valid @RequestBody PublicSendVerificationCodeRequest request) {
        return ApiResponse.success(registrationEmailVerificationService.sendCode(request.getEmail()));
    }

    @PostMapping("/register")
    public ApiResponse<PublicRegisterResponse> register(@Valid @RequestBody PublicRegisterRequest request) {
        return ApiResponse.success(portalRegistrationService.register(request));
    }

    @GetMapping("/register/username-availability")
    public ApiResponse<PublicUsernameAvailabilityResponse> checkUsernameAvailability(@RequestParam("username") String username) {
        return ApiResponse.success(portalRegistrationService.checkUsernameAvailability(username));
    }
}
