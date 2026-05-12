package com.company.finance_api.admin.dto;

import java.util.List;

/** Admin-only nested view of a user's external portfolios and position weights. */
public record AdminUserPortfolioTreeDto(List<AdminPortfolioDetailDto> portfolios) {}
