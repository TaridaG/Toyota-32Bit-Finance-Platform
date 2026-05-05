package com.company.finance_api.dto;

import java.time.Instant;

public record PortalProfileResponse(
        String email,
        String username,
        String phone,
        boolean notifySecurityAlerts,
        boolean notifyProductUpdates,
        Instant avatarUpdatedAt
) {}
