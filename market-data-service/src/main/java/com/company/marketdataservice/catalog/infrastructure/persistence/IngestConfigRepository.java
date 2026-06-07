package com.company.marketdataservice.catalog.infrastructure.persistence;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Ingest yapılandırma kayıtları için persistence Spring Data JPA repository.
 */
@Repository
public interface IngestConfigRepository extends JpaRepository<IngestConfigEntry, IngestConfigEntry.IngestConfigId> {

    List<IngestConfigEntry> findBySegmentAndEnabledTrue(String segment);

    Page<IngestConfigEntry> findAllByOrderBySegmentAscInstrumentIdAsc(Pageable pageable);

    long countByInstrumentId(Long instrumentId);
}

