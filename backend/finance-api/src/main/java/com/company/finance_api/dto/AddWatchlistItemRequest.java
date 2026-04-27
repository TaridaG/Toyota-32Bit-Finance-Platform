package com.company.finance_api.dto;

import jakarta.validation.constraints.NotNull;

public class AddWatchlistItemRequest {

    @NotNull
    private Long instrumentId;

    public Long getInstrumentId() {
        return instrumentId;
    }
}
