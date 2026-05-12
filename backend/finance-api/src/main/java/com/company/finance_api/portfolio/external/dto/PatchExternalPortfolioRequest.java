package com.company.finance_api.portfolio.external.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PatchExternalPortfolioRequest {

    @NotNull
    private Boolean amountsHidden;
}
