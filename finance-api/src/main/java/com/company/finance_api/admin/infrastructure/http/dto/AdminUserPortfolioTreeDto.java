package com.company.finance_api.admin.infrastructure.http.dto;

import java.util.List;

/** Bir kullanıcının external portfolio'ları ve pozisyon ağırlıkları (admin-only iç görünüm). */
public record AdminUserPortfolioTreeDto(List<AdminPortfolioDetailDto> portfolios) {}
