package com.company.finance_api.portfolio.external.application;

import com.company.finance_api.instrument.domain.Instrument;
import com.company.finance_api.profile.domain.User;
import com.company.finance_api.portfolio.external.domain.ExternalPortfolio;
import com.company.finance_api.portfolio.external.domain.ExternalPositionLot;
import com.company.finance_api.portfolio.external.domain.ExternalPositionSourceType;
import com.company.finance_api.portfolio.external.infrastructure.http.dto.CreateExternalPortfolioRequest;
import com.company.finance_api.portfolio.external.infrastructure.http.dto.CreateExternalPositionRequest;
import com.company.finance_api.portfolio.external.infrastructure.http.dto.ExternalPortfolioResponse;
import com.company.finance_api.portfolio.external.infrastructure.http.dto.PatchExternalPortfolioRequest;
import com.company.finance_api.portfolio.external.infrastructure.persistence.ExternalPortfolioRepository;
import com.company.finance_api.portfolio.external.infrastructure.persistence.ExternalPositionLotRepository;
import com.company.finance_api.portfolio.external.application.ExternalPortfolioService;
import com.company.finance_api.instrument.infrastructure.persistence.InstrumentRepository;
import com.company.finance_api.portfolio.infrastructure.persistence.PortfolioSnapshotRepository;
import com.company.finance_api.portfolio.infrastructure.persistence.TransactionRepository;
import com.company.finance_api.profile.infrastructure.persistence.UserRepository;
import com.company.finance_api.shared.web.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** External portfolio CRUD ve pozisyon yönetimi için service implementasyonu. */
@Service
@RequiredArgsConstructor
@Transactional
public class ExternalPortfolioServiceImpl implements ExternalPortfolioService {
  private static final int MAX_PORTFOLIOS_PER_USER = 5;

  private final ExternalPortfolioRepository portfolioRepository;
  private final ExternalPositionLotRepository lotRepository;
  private final UserRepository userRepository;
  private final InstrumentRepository instrumentRepository;
  private final TransactionRepository transactionRepository;
  private final PortfolioSnapshotRepository portfolioSnapshotRepository;

  private static ExternalPortfolioResponse toResponse(ExternalPortfolio p) {
    return ExternalPortfolioResponse.builder()
        .id(p.getId())
        .name(p.getName())
        .baseCurrency(p.getBaseCurrency())
        .createdAt(
            p.getCreatedAt() == null
                ? null
                : p.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        .amountsHidden(p.isAmountsHidden())
        .build();
  }

  /** Kullanıcı için yeni external portfolio oluşturur. */
  @Override
  public ExternalPortfolioResponse createPortfolio(
      UUID userId, CreateExternalPortfolioRequest request) {
    if (portfolioRepository.countByUserId(userId) >= MAX_PORTFOLIOS_PER_USER) {
      throw new IllegalStateException("Maximum portfolio limit reached (5)");
    }

    User user =
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

    String rawBc = request.getBaseCurrency();
    String normalizedBc;
    if (rawBc == null || rawBc.isBlank()) {
      normalizedBc = "MIXED";
    } else {
      normalizedBc = rawBc.trim().toUpperCase(Locale.ROOT);
      if (!"TRY".equals(normalizedBc)
          && !"USD".equals(normalizedBc)
          && !"MIXED".equals(normalizedBc)) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "baseCurrency must be TRY, USD, or MIXED");
      }
    }

    ExternalPortfolio portfolio = new ExternalPortfolio(user, request.getName(), normalizedBc);

    portfolioRepository.save(portfolio);

    return toResponse(portfolio);
  }

  /** Kullanıcının tüm external portfolio kayıtlarını listeler. */
  @Override
  public List<ExternalPortfolioResponse> getUserPortfolios(UUID userId) {

    return portfolioRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
        .map(ExternalPortfolioServiceImpl::toResponse)
        .toList();
  }

  /** Tek bir external portfolio detayını döner. */
  @Override
  public ExternalPortfolioResponse getPortfolio(UUID userId, Long portfolioId) {
    ExternalPortfolio portfolio =
        portfolioRepository
            .findByIdAndUserId(portfolioId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found"));
    return toResponse(portfolio);
  }

  /** External portfolio ayarlarını kısmi olarak günceller. */
  @Override
  public ExternalPortfolioResponse patchPortfolio(
      UUID userId, Long portfolioId, PatchExternalPortfolioRequest request) {
    ExternalPortfolio portfolio =
        portfolioRepository
            .findByIdAndUserId(portfolioId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found"));
    portfolio.setAmountsHidden(Boolean.TRUE.equals(request.getAmountsHidden()));
    portfolioRepository.save(portfolio);
    return toResponse(portfolio);
  }

  /**
   * External portfolio'yu ve bağlı transaction, snapshot ve lot kayıtlarını kalıcı olarak siler.
   */
  @Override
  public void deletePortfolio(UUID userId, Long portfolioId) {
    ExternalPortfolio portfolio =
        portfolioRepository
            .findByIdAndUserId(portfolioId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found"));
    transactionRepository.deleteAllByExternalPortfolioId(portfolioId);
    portfolioSnapshotRepository.deleteAllByExternalPortfolioId(portfolioId);
    lotRepository.deleteAllByPortfolioId(portfolioId);
    portfolioRepository.delete(portfolio);
  }

  /** Portfolio'ya manuel external pozisyon lot'u ekler. */
  @Override
  public void addPosition(UUID userId, Long portfolioId, CreateExternalPositionRequest request) {

    ExternalPortfolio portfolio =
        portfolioRepository
            .findByIdAndUserId(portfolioId, userId)
            .orElseThrow(() -> new RuntimeException("Portfolio not found"));

    Instrument instrument =
        instrumentRepository
            .findById(request.getInstrumentId())
            .orElseThrow(() -> new RuntimeException("Instrument not found"));

    ExternalPositionLot lot =
        new ExternalPositionLot(
            portfolio,
            instrument,
            request.getQuantity(),
            request.getUnitPrice(),
            request.getFeeAmount(),
            request.getFeeCurrency(),
            request.getAcquiredAt(),
            ExternalPositionSourceType.MANUAL,
            request.getSourceName(),
            request.getNotes());

    lotRepository.save(lot);
  }

  /** Belirtilen pozisyon lot'unu soft-delete ile kaldırır. */
  @Override
  public void deletePosition(UUID userId, Long portfolioId, Long positionId) {

    ExternalPositionLot lot =
        lotRepository
            .findByIdAndPortfolioIdAndDeletedFalse(positionId, portfolioId)
            .orElseThrow(() -> new RuntimeException("Position not found"));

    if (!lot.getPortfolio().getUser().getId().equals(userId)) {
      throw new RuntimeException("Unauthorized");
    }

    lot.softDelete();
  }
}
