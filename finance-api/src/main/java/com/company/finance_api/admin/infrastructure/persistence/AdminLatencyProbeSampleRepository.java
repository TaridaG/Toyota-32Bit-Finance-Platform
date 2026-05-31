package com.company.finance_api.admin.infrastructure.persistence;

import com.company.finance_api.admin.domain.AdminLatencyProbeSample;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** AdminLatencyProbeSample entity persistence için Spring Data repository. */
public interface AdminLatencyProbeSampleRepository
    extends JpaRepository<AdminLatencyProbeSample, Long> {

  boolean existsByRunId(Long runId);

  List<AdminLatencyProbeSample> findAllByRunIdOrderBySortOrderAsc(Long runId);

  @Modifying
  @Query("DELETE FROM AdminLatencyProbeSample s WHERE s.runId <> :keepRunId")
  int deleteByRunIdNot(@Param("keepRunId") Long keepRunId);
}
