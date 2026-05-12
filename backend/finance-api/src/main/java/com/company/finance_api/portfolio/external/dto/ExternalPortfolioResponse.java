package com.company.finance_api.portfolio.external.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExternalPortfolioResponse {

    private Long id;
    private String name;
    private String baseCurrency;
    /** ISO-8601 local date-time (server default zone). */
    private String createdAt;
    private boolean amountsHidden;
}

