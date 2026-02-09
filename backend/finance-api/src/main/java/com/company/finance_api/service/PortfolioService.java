package com.company.finance_api.service;

import com.company.finance_api.dto.PortfolioPositionResponse;

import java.util.List;

public interface PortfolioService {

    List<PortfolioPositionResponse> getMyPortfolio();
}