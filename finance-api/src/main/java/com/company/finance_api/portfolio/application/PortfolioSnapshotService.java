package com.company.finance_api.portfolio.application;

import com.company.finance_api.domain.PortfolioSnapshot;
import java.util.List;
import java.util.UUID;

/** PortfolioSnapshotService iş mantığını uygular (portfolio snapshot service). */
public interface PortfolioSnapshotService {

  /** createSnapshotForUser sözleşmesi. */
  void createSnapshotForUser(UUID userId);

  /** createSnapshotsForAllUsers sözleşmesi. */
  void createSnapshotsForAllUsers();

  /** getMySnapshots sözleşmesi. */
  List<PortfolioSnapshot> getMySnapshots(Long portfolioId);
}
