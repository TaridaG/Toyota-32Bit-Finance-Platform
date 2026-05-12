package com.company.finance_api.admin.dto;

import java.util.List;

public record AdminUserSegmentsDto(boolean available, List<AdminUserSegmentBucketDto> buckets) {
    public static AdminUserSegmentsDto notTracked() {
        return new AdminUserSegmentsDto(false, List.of());
    }
}
