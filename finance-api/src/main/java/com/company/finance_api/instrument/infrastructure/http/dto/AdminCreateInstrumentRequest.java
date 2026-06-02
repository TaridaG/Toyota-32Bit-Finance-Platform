package com.company.finance_api.instrument.infrastructure.http.dto;

import com.company.finance_api.instrument.domain.enums.Exchange;
import com.company.finance_api.instrument.domain.enums.InstrumentType;

public record AdminCreateInstrumentRequest(
        String symbol,
        String name,
        InstrumentType type,
        Exchange exchange,
        String segment
) {
}

