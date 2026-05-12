package com.company.finance_api.repository;

import com.company.finance_api.domain.AdminLatencyProbeRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminLatencyProbeRunRepository extends JpaRepository<AdminLatencyProbeRun, Long> {

    Optional<AdminLatencyProbeRun> findFirstByOrderByIdDesc();
}
