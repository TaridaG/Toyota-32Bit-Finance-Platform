package com.company.reporting.infrastructure.persistence;

import com.company.reporting.domain.ReportMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReportMetadataRepository extends JpaRepository<ReportMetadata, Long> {

    Optional<ReportMetadata> findByReportUuid(UUID reportUuid);
}