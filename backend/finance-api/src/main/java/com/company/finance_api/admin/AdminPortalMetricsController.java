package com.company.finance_api.admin;

import com.company.finance_api.common.ApiResponse;
import com.company.finance_api.admin.dto.AdminPortalUserMetricsDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/metrics")
public class AdminPortalMetricsController {

    private final AdminPortalUserMetricsService adminPortalUserMetricsService;

    public AdminPortalMetricsController(AdminPortalUserMetricsService adminPortalUserMetricsService) {
        this.adminPortalUserMetricsService = adminPortalUserMetricsService;
    }

    /**
     * Portal user roster and signup velocity (UTC windows). Requires {@code ADMIN} at the API gateway.
     */
    @GetMapping("/portal-users")
    public ApiResponse<AdminPortalUserMetricsDto> portalUsers() {
        return ApiResponse.success(adminPortalUserMetricsService.snapshot());
    }
}
