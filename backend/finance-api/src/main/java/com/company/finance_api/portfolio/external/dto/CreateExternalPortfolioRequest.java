package com.company.finance_api.portfolio.external.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateExternalPortfolioRequest {

    @NotBlank
    @Size(max = 120)
    private String name;

    private String baseCurrency; // optional
}

