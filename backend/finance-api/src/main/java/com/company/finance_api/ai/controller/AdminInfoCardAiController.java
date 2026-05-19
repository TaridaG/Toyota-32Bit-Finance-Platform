package com.company.finance_api.ai.controller;

import com.company.finance_api.ai.dto.CompleteInfoCardAiRequest;
import com.company.finance_api.ai.dto.InfoCardAiContentResponse;
import com.company.finance_api.ai.dto.TranslateInfoCardAiRequest;
import com.company.finance_api.ai.service.AdminInfoCardAiService;
import com.company.finance_api.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/info-cards/ai")
@PreAuthorize("hasRole('ADMIN')")
public class AdminInfoCardAiController {

    private final AdminInfoCardAiService adminInfoCardAiService;

    public AdminInfoCardAiController(AdminInfoCardAiService adminInfoCardAiService) {
        this.adminInfoCardAiService = adminInfoCardAiService;
    }

    @PostMapping("/complete")
    public ApiResponse<InfoCardAiContentResponse> complete(@Valid @RequestBody CompleteInfoCardAiRequest request) {
        return ApiResponse.success(adminInfoCardAiService.complete(request));
    }

    @PostMapping("/translate")
    public ApiResponse<InfoCardAiContentResponse> translate(@Valid @RequestBody TranslateInfoCardAiRequest request) {
        return ApiResponse.success(adminInfoCardAiService.translate(request));
    }
}
