package com.company.finance_api.admin.infrastructure.http.dto;

import java.util.List;

/** Sayfalı admin kullanıcı dizin yanıtı. */
public record AdminUserDirectoryPageDto(
    List<AdminUserListItemDto> content,
    long totalElements,
    int totalPages,
    int page,
    int size,
    String sort) {}
