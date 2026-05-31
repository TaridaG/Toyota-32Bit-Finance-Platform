package com.company.finance_api.chart.infrastructure.persistence;

import com.company.finance_api.chart.domain.ChartDrawingSave;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** ChartDrawingSave entity persistence için Spring Data repository. */
public interface ChartDrawingSaveRepository extends JpaRepository<ChartDrawingSave, Long> {

  List<ChartDrawingSave> findByUserIdAndAssetKeyOrderByCreatedAtDesc(UUID userId, String assetKey);

  Page<ChartDrawingSave> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

  Optional<ChartDrawingSave> findByIdAndUserId(Long id, UUID userId);
}
