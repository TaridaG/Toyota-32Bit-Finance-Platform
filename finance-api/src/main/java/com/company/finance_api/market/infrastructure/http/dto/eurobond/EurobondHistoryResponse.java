package com.company.finance_api.market.infrastructure.http.dto.eurobond;

import java.util.List;

/** EurobondHistoryResponse — API transfer nesnesi (DTO/response/request). */
public record EurobondHistoryResponse(
    String isin, String range, String frequency, List<EurobondHistoryPointDto> points) {}
