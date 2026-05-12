package com.company.finance_api.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminRecentRegistrationRowDto(
        UUID id,
        String displayName,
        String maskedEmail,
        Instant createdAt,
        /** Registration channel when tracked; empty when unknown (UI shows em dash). */
        String source,
        /** Client device class when tracked; empty when unknown. */
        String device,
        /** {@code ACTIVE} or {@code INACTIVE} for roster accounts. */
        String status
) {
}
