package com.company.finance_api.service.impl;

import com.company.finance_api.domain.PortfolioSnapshot;
import com.company.finance_api.domain.User;
import com.company.finance_api.dto.PortfolioSummaryResponse;
import com.company.finance_api.repository.PortfolioSnapshotRepository;
import com.company.finance_api.repository.UserRepository;
import com.company.finance_api.security.CurrentUserResolver;
import com.company.finance_api.service.PortfolioService;
import com.company.finance_api.service.PortfolioSnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PortfolioSnapshotServiceImpl implements PortfolioSnapshotService {

    private final PortfolioSnapshotRepository repository;
    private final PortfolioService portfolioService;
    private final CurrentUserResolver currentUserResolver;
    private final UserRepository userRepository;

    @Override
    public void createSnapshotForUser(UUID userId) {
        PortfolioSummaryResponse summary = portfolioService.getPortfolioSummary(userId);

        PortfolioSnapshot snapshot = new PortfolioSnapshot();
        snapshot.setUserId(userId);
        snapshot.setTotalCost(summary.totalCost());
        snapshot.setTotalValue(summary.totalValue());
        snapshot.setUnrealizedPnl(summary.unrealizedPnl());
        snapshot.setCreatedAt(Instant.now());

        repository.save(snapshot);
    }

    @Override
    public void createSnapshotsForAllUsers() {
        List<User> users = userRepository.findByActiveTrue(); // EKLE
        for (User user : users) {
            createSnapshotForUser(user.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioSnapshot> getMySnapshots() {
        UUID userId = currentUserResolver.getCurrentUserId();
        return repository.findByUserIdOrderByCreatedAtAsc(userId);
    }
}