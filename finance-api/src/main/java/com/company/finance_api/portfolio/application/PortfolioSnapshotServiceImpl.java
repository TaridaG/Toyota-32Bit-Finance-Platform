package com.company.finance_api.portfolio.application;

import com.company.finance_api.domain.PortfolioSnapshot;
import com.company.finance_api.domain.User;
import com.company.finance_api.portfolio.external.domain.ExternalPortfolio;
import com.company.finance_api.portfolio.external.dto.ExternalPortfolioSummaryResponse;
import com.company.finance_api.portfolio.external.repository.ExternalPortfolioRepository;
import com.company.finance_api.portfolio.external.service.ExternalPortfolioValuationService;
import com.company.finance_api.repository.PortfolioSnapshotRepository;
import com.company.finance_api.repository.UserRepository;
import com.company.finance_api.shared.security.CurrentUserResolver;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** PortfolioSnapshotServiceImpl iş mantığını uygular (portfolio snapshot service). */
@Service
@RequiredArgsConstructor
@Transactional
public class PortfolioSnapshotServiceImpl implements PortfolioSnapshotService {

  private final PortfolioSnapshotRepository repository;
  private final CurrentUserResolver currentUserResolver;
  private final UserRepository userRepository;
  private final ExternalPortfolioRepository externalPortfolioRepository;
  private final ExternalPortfolioValuationService externalPortfolioValuationService;

  /** Yeni SnapshotForUser kaydı oluşturur. */
  @Override
  public void createSnapshotForUser(UUID userId) {
    List<ExternalPortfolio> portfolios =
        externalPortfolioRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    if (portfolios.isEmpty()) {
      return;
    }
    Instant now = Instant.now();
    for (ExternalPortfolio p : portfolios) {
      ExternalPortfolioSummaryResponse summary =
          externalPortfolioValuationService.calculateSummary(userId, p.getId());
      PortfolioSnapshot snapshot = new PortfolioSnapshot();
      snapshot.setUserId(userId);
      snapshot.setExternalPortfolioId(p.getId());
      snapshot.setTotalCost(summary.getTotalCost());
      snapshot.setTotalValue(summary.getTotalMarketValue());
      snapshot.setUnrealizedPnl(summary.getTotalPnL());
      snapshot.setCreatedAt(now);
      repository.save(snapshot);
    }
  }

  /** Yeni SnapshotsForAllUsers kaydı oluşturur. */
  @Override
  public void createSnapshotsForAllUsers() {
    List<User> users = userRepository.findByActiveTrue();
    for (User user : users) {
      createSnapshotForUser(user.getId());
    }
  }

  @Override
  @Transactional(readOnly = true)
  /** MySnapshots sorgusunu döner. */
  public List<PortfolioSnapshot> getMySnapshots(Long portfolioId) {
    UUID userId = currentUserResolver.getCurrentUserId();
    if (portfolioId == null) {
      return List.of();
    }
    externalPortfolioRepository
        .findByIdAndUserId(portfolioId, userId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Portfolio not found"));
    return repository.findByUserIdAndExternalPortfolioIdOrderByCreatedAtAsc(userId, portfolioId);
  }
}
