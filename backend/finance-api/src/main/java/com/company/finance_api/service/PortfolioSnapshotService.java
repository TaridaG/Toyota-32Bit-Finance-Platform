package com.company.finance_api.service;

import com.company.finance_api.domain.PortfolioSnapshot;

import java.util.List;
import java.util.UUID;

public interface PortfolioSnapshotService {

    void createSnapshotForUser(UUID userId);

    void createSnapshotsForAllUsers();

    List<PortfolioSnapshot> getMySnapshots(Long portfolioId);
}