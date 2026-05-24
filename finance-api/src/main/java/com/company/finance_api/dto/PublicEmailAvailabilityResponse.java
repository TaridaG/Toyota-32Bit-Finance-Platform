package com.company.finance_api.dto;

import java.util.List;

/** PublicEmailAvailabilityResponse — API transfer nesnesi (DTO/response/request). */
public record PublicEmailAvailabilityResponse(
    String normalizedEmail, boolean available, boolean blocked, List<String> suggestions) {}
