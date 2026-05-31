package com.company.finance_api.admin.infrastructure.persistence;

import com.company.finance_api.admin.domain.AdminMarketAssetStat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** AdminMarketAssetStat entity persistence için Spring Data repository. */
public interface AdminMarketAssetStatRepository extends JpaRepository<AdminMarketAssetStat, Long> {

  Page<AdminMarketAssetStat> findAllByOrderBySortRankAsc(Pageable pageable);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("delete from AdminMarketAssetStat")
  void deleteAllRows();
}
