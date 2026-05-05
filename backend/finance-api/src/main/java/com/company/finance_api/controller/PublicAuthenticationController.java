package com.company.finance_api.controller;

import com.company.finance_api.auth.PortalLoginService;
import com.company.finance_api.common.ApiResponse;
import com.company.finance_api.dto.PublicLoginRequest;
import com.company.finance_api.dto.PublicLoginResponse;
import com.company.finance_api.dto.PublicRefreshRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicAuthenticationController {

    private final PortalLoginService portalLoginService;

    public PublicAuthenticationController(PortalLoginService portalLoginService) {
        this.portalLoginService = portalLoginService;
    }

    @PostMapping("/login")
    public ApiResponse<PublicLoginResponse> login(@Valid @RequestBody PublicLoginRequest request) {
        return ApiResponse.success(portalLoginService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<PublicLoginResponse> refresh(@Valid @RequestBody PublicRefreshRequest request) {
        return ApiResponse.success(portalLoginService.refresh(request));
    }
}
