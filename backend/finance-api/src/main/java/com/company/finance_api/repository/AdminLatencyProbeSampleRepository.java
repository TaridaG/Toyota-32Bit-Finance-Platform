package com.company.finance_api.repository;

import com.company.finance_api.domain.AdminLatencyProbeSample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdminLatencyProbeSampleRepository extends JpaRepository<AdminLatencyProbeSample, Long> {

    boolean existsByRunId(Long runId);

    List<AdminLatencyProbeSample> findAllByRunIdOrderBySortOrderAsc(Long runId);

    @Modifying
    @Query("DELETE FROM AdminLatencyProbeSample s WHERE s.runId <> :keepRunId")
    int deleteByRunIdNot(@Param("keepRunId") Long keepRunId);
}
