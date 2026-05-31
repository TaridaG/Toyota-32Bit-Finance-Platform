package com.company.finance_api.portfolio.infrastructure.persistence;

import com.company.finance_api.portfolio.domain.TransactionAcquisitionFx;
import org.springframework.data.jpa.repository.JpaRepository;

/** TransactionAcquisitionFx entity persistence için Spring Data repository. */
public interface TransactionAcquisitionFxRepository
    extends JpaRepository<TransactionAcquisitionFx, Long> {}
