package com.company.finance_api.admin.infrastructure.http.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** İstemciden gelen latency probe örneklerini kaydetme isteği. */
public record AdminLatencySnapshotSaveRequest(@NotEmpty @Valid List<LatencySampleDto> samples) {}
