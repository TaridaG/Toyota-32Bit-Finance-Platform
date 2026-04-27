package com.company.notification.repository;

import com.company.notification.domain.WatchlistProjection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WatchlistProjectionRepository extends JpaRepository<WatchlistProjection, Long> {

    Optional<WatchlistProjection> findByUserIdAndInstrumentId(UUID userId, Long instrumentId);

    List<WatchlistProjection> findByInstrumentIdAndActiveTrue(Long instrumentId);

    List<WatchlistProjection> findByUserIdAndActiveTrue(UUID userId);

    long countByActiveTrue();
}
