package com.company.finance_api.portfolio.application;

import com.company.finance_api.portfolio.infrastructure.http.dto.PortfolioPositionResponse;
import com.company.finance_api.portfolio.infrastructure.http.dto.PortfolioSummaryResponse;
import java.util.List;
import java.util.UUID;

/** PortfolioService iş mantığını uygular (portfolio service). */
public interface PortfolioService {

  /** Oturum açmış kullanıcının açık pozisyonlarını listeler. */
  List<PortfolioPositionResponse> getMyPortfolio();

  /** Oturum açmış kullanıcı için portfolio özet metriklerini döner. */
  PortfolioSummaryResponse getPortfolioSummary();

  /** Belirtilen kullanıcı için portfolio özet metriklerini döner (admin/internal). */
  PortfolioSummaryResponse getPortfolioSummary(UUID userId);
}
