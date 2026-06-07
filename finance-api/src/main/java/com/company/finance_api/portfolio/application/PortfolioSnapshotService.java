package com.company.finance_api.portfolio.application;

import com.company.finance_api.portfolio.domain.PortfolioSnapshot;
import java.util.List;
import java.util.UUID;

/** PortfolioSnapshotService iş mantığını uygular (portfolio snapshot service). */
public interface PortfolioSnapshotService {

  /** Belirtilen kullanıcı için günlük portfolio snapshot kaydı oluşturur. */
  void createSnapshotForUser(UUID userId);

  /** Tüm aktif kullanıcılar için snapshot oluşturur (scheduler). */
  void createSnapshotsForAllUsers();

  /** Oturum açmış kullanıcının geçmiş snapshot listesini döner. */
  List<PortfolioSnapshot> getMySnapshots(Long portfolioId);
}
