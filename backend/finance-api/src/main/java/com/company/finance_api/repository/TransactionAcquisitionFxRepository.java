package com.company.finance_api.repository;

import com.company.finance_api.domain.TransactionAcquisitionFx;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionAcquisitionFxRepository extends JpaRepository<TransactionAcquisitionFx, Long> {
}
