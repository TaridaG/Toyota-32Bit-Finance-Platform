package com.company.finance_api.admin.infrastructure.http.dto;

import java.util.List;

/** Sayfalı kayıt engelli e-posta listesi. */
public record AdminBlockedEmailsPageDto(
    List<AdminBlockedEmailRowDto> content,
    long totalElements,
    int totalPages,
    int page,
    int size) {}
