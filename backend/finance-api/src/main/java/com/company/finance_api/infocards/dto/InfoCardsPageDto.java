package com.company.finance_api.infocards.dto;

import java.util.List;

public record InfoCardsPageDto(
        List<InfoCardDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
