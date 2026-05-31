package com.company.finance_api.admin.infrastructure.persistence;

import com.company.finance_api.admin.domain.AdminMarketAssetSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

/** AdminMarketAssetSnapshot entity persistence için Spring Data repository. */
public interface AdminMarketAssetSnapshotRepository
    extends JpaRepository<AdminMarketAssetSnapshot, Long> {}
