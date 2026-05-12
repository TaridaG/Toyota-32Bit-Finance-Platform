package com.company.finance_api.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AdminLatencySnapshotSaveRequest(
        @NotEmpty @Valid List<LatencySampleDto> samples
) {
}
