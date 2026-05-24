package com.company.finance_api.admin.infrastructure.http.dto;

/** Tek segment kovası: etiket, adet ve yüzde. */
public record AdminUserSegmentBucketDto(String label, long count, double percentage) {}
