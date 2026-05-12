package com.company.finance_api.admin.dto;

import java.util.List;

public record AdminUserDirectoryPageDto(
        List<AdminUserListItemDto> content,
        long totalElements,
        int totalPages,
        int page,
        int size,
        String sort
) {}
