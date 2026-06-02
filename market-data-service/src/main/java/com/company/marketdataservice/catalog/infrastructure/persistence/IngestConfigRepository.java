package com.company.marketdataservice.catalog.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IngestConfigRepository extends JpaRepository<IngestConfigEntry, IngestConfigEntry.IngestConfigId> {

    List<IngestConfigEntry> findBySegmentAndEnabledTrue(String segment);

    long countByInstrumentId(Long instrumentId);
}

