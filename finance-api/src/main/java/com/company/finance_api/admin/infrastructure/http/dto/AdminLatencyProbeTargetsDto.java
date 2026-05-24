package com.company.finance_api.admin.infrastructure.http.dto;

import java.util.List;

/** SPA'nın sırayla probe etmesi gereken GET path listesi. */
public record AdminLatencyProbeTargetsDto(List<String> paths) {}
