package com.company.finance_api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "admin_latency_probe_sample")
public class AdminLatencyProbeSample {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(nullable = false, length = 512)
    private String path;

    @Column(name = "duration_ms", nullable = false)
    private double durationMs;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected AdminLatencyProbeSample() {
    }

    public AdminLatencyProbeSample(Long runId, String path, double durationMs, int sortOrder) {
        this.runId = runId;
        this.path = path;
        this.durationMs = durationMs;
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public Long getRunId() {
        return runId;
    }

    public String getPath() {
        return path;
    }

    public double getDurationMs() {
        return durationMs;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
