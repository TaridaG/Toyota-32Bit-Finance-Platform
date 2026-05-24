package com.company.marketdataservice.history.infrastructure.persistence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * `geçmiş veri ve backfill` verisi için Spring Data JPA repository.
 */
public interface BackfillStateRepository extends JpaRepository<BackfillStateEntry, Long> {

    Optional<BackfillStateEntry> findByAssetTypeAndSymbol(String assetType, String symbol);

    List<BackfillStateEntry> findByStatusIn(Collection<String> statuses);

    List<BackfillStateEntry> findByStatusAndNextRetryAtLessThanEqual(String status, Instant now);

    List<BackfillStateEntry> findByAssetTypeOrderBySymbolAsc(String assetType);

    List<BackfillStateEntry> findByAssetTypeAndStatusOrderBySymbolAsc(String assetType, String status);

    List<BackfillStateEntry> findByAssetTypeAndSymbolStartingWithIgnoreCaseOrderBySymbolAsc(String assetType, String symbolPrefix);
}
