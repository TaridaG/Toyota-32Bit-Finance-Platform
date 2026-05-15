package com.company.finance_api.dto.market.eurobond;

import java.util.List;

public record EurobondHistoryResponse(
        String isin,
        String range,
        String frequency,
        List<EurobondHistoryPointDto> points
) {
}
