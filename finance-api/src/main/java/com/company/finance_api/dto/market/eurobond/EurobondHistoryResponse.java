package com.company.finance_api.dto.market.eurobond;

import java.util.List;

/** EurobondHistoryResponse — API transfer nesnesi (DTO/response/request). */
public record EurobondHistoryResponse(
    String isin, String range, String frequency, List<EurobondHistoryPointDto> points) {}
