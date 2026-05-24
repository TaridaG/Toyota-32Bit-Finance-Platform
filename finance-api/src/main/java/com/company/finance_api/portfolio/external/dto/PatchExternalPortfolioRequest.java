package com.company.finance_api.portfolio.external.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** External portfolio kısmi güncelleme isteği DTO'su. */
@Data
public class PatchExternalPortfolioRequest {

  @NotNull private Boolean amountsHidden;
}
