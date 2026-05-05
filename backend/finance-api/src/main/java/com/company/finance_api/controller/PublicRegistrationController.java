package com.company.finance_api.controller;

import com.company.finance_api.common.ApiResponse;
import com.company.finance_api.dto.PublicRegisterRequest;
import com.company.finance_api.dto.PublicRegisterResponse;
import com.company.finance_api.registration.PortalRegistrationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicRegistrationController {

    private final PortalRegistrationService portalRegistrationService;

    public PublicRegistrationController(PortalRegistrationService portalRegistrationService) {
        this.portalRegistrationService = portalRegistrationService;
    }

    @PostMapping("/register")
    public ApiResponse<PublicRegisterResponse> register(@Valid @RequestBody PublicRegisterRequest request) {
        return ApiResponse.success(portalRegistrationService.register(request));
    }
}
