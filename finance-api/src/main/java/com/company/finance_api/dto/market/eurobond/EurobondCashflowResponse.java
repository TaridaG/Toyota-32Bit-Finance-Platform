package com.company.finance_api.dto.market.eurobond;

import java.math.BigDecimal;

/** EurobondCashflowResponse — API transfer nesnesi (DTO/response/request). */
public record EurobondCashflowResponse(
    String isin,
    BigDecimal nominalAmount,
    BigDecimal couponPercent,
    BigDecimal annualCouponUsd,
    BigDecimal semiAnnualCouponUsd,
    BigDecimal approximatePurchaseAmountUsd,
    BigDecimal maturityPrincipalUsd,
    String disclaimer) {}
