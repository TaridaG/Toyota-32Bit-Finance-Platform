package com.company.finance_api.repository;

import com.company.finance_api.domain.AdminMarketAssetSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

/** AdminMarketAssetSnapshot entity persistence için Spring Data repository. */
public interface AdminMarketAssetSnapshotRepository
    extends JpaRepository<AdminMarketAssetSnapshot, Long> {}
