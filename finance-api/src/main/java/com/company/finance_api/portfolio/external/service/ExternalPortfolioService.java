package com.company.finance_api.portfolio.external.service;

import com.company.finance_api.portfolio.external.dto.CreateExternalPortfolioRequest;
import com.company.finance_api.portfolio.external.dto.CreateExternalPositionRequest;
import com.company.finance_api.portfolio.external.dto.ExternalPortfolioResponse;
import com.company.finance_api.portfolio.external.dto.PatchExternalPortfolioRequest;
import java.util.List;
import java.util.UUID;

/** External portfolio CRUD ve pozisyon yönetimi için service arayüzü. */
public interface ExternalPortfolioService {

  /** Yeni external portfolio oluşturur. */
  ExternalPortfolioResponse createPortfolio(UUID userId, CreateExternalPortfolioRequest request);

  /** Kullanıcının external portfolio listesini döner. */
  List<ExternalPortfolioResponse> getUserPortfolios(UUID userId);

  /** Tek external portfolio detayını döner. */
  ExternalPortfolioResponse getPortfolio(UUID userId, Long portfolioId);

  /** External portfolio ayarlarını günceller. */
  ExternalPortfolioResponse patchPortfolio(
      UUID userId, Long portfolioId, PatchExternalPortfolioRequest request);

  /** External portfolio'yu ve bağlı kayıtları siler. */
  void deletePortfolio(UUID userId, Long portfolioId);

  /** Portfolio'ya pozisyon ekler. */
  void addPosition(UUID userId, Long portfolioId, CreateExternalPositionRequest request);

  /** Portfolio'dan pozisyon siler. */
  void deletePosition(UUID userId, Long portfolioId, Long positionId);
}
