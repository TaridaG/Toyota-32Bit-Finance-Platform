package com.company.marketdataservice.fund;

import java.math.BigDecimal;
import java.time.Instant;

public record FundSnapshot(
        Long instrumentId,
        String fundCode,
        BigDecimal nav,
        Instant timestamp,
        String source
) {
}
