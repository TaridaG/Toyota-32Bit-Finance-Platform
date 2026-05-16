package com.company.finance_api.repository;

import com.company.finance_api.domain.ChartDrawingSave;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChartDrawingSaveRepository extends JpaRepository<ChartDrawingSave, Long> {

    List<ChartDrawingSave> findByUserIdAndAssetKeyOrderByCreatedAtDesc(UUID userId, String assetKey);

    Optional<ChartDrawingSave> findByIdAndUserId(Long id, UUID userId);
}
