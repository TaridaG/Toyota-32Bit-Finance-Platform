package com.company.finance_api.dto;

import java.util.List;

/** PublicUsernameAvailabilityResponse — API transfer nesnesi (DTO/response/request). */
public record PublicUsernameAvailabilityResponse(
    String normalizedUsername, boolean available, List<String> suggestions) {}
