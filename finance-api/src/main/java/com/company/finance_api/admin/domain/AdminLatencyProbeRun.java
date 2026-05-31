package com.company.finance_api.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** AdminLatencyProbeRun — JPA domain entity (admin latency probe run). */
@Entity
@Table(name = "admin_latency_probe_run")
public class AdminLatencyProbeRun {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "average_latency_ms", nullable = false)
  private double averageLatencyMs;

  @Column(name = "sample_count", nullable = false)
  private int sampleCount;

  @Column(name = "measured_at", nullable = false)
  private Instant measuredAt;

  protected AdminLatencyProbeRun() {}

  public AdminLatencyProbeRun(double averageLatencyMs, int sampleCount, Instant measuredAt) {
    this.averageLatencyMs = averageLatencyMs;
    this.sampleCount = sampleCount;
    this.measuredAt = measuredAt;
  }

  public Long getId() {
    return id;
  }

  public double getAverageLatencyMs() {
    return averageLatencyMs;
  }

  public int getSampleCount() {
    return sampleCount;
  }

  public Instant getMeasuredAt() {
    return measuredAt;
  }
}
