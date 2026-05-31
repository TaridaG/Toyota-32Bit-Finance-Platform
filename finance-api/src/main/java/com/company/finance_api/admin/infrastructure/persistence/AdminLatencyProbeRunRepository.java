package com.company.finance_api.admin.infrastructure.persistence;

import com.company.finance_api.admin.domain.AdminLatencyProbeRun;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** AdminLatencyProbeRun entity persistence için Spring Data repository. */
public interface AdminLatencyProbeRunRepository extends JpaRepository<AdminLatencyProbeRun, Long> {

  Optional<AdminLatencyProbeRun> findFirstByOrderByIdDesc();
}
