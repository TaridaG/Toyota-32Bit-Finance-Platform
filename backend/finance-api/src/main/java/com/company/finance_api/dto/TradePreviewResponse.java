package com.company.finance_api.dto;

import java.math.BigDecimal;

public record TradePreviewResponse(
        Long instrumentId,
        String instrumentSymbol,
        String instrumentQuoteCurrency,
        BigDecimal computedLots,
        BigDecimal computedInputAmount,
        String inputCurrency,
        BigDecimal unitPriceUsed,
        BigDecimal fxRateUsed,
        boolean manualUnitPriceRequired,
        String unitPriceSource
) {
}

