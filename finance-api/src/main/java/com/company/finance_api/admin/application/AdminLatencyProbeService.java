package com.company.finance_api.admin.application;

import com.company.finance_api.admin.infrastructure.http.dto.AdminLatencyProbeSampleItemDto;
import com.company.finance_api.admin.infrastructure.http.dto.AdminLatencyRunListItemDto;
import com.company.finance_api.admin.infrastructure.http.dto.AdminLatencyRunsPageDto;
import com.company.finance_api.admin.infrastructure.http.dto.AdminLatencySnapshotDto;
import com.company.finance_api.admin.infrastructure.http.dto.AdminLatencySnapshotSaveRequest;
import com.company.finance_api.admin.infrastructure.http.dto.LatencySampleDto;
import com.company.finance_api.admin.domain.AdminLatencyProbeRun;
import com.company.finance_api.admin.domain.AdminLatencyProbeSample;
import com.company.finance_api.admin.infrastructure.persistence.AdminLatencyProbeRunRepository;
import com.company.finance_api.admin.infrastructure.persistence.AdminLatencyProbeSampleRepository;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Admin dashboard round-trip latency probe snapshot ve geçmiş run'larını yönetir. */
@Service
public class AdminLatencyProbeService {

  private static final List<String> PROBE_TARGETS =
      List.of("/api/v1/admin/metrics/portal-users", "/api/v1/admin/metrics/portal-portfolios");

  private static final Set<String> PROBE_TARGET_SET = Set.copyOf(PROBE_TARGETS);

  private final AdminLatencyProbeRunRepository runRepository;
  private final AdminLatencyProbeSampleRepository sampleRepository;

  public AdminLatencyProbeService(
      AdminLatencyProbeRunRepository runRepository,
      AdminLatencyProbeSampleRepository sampleRepository) {
    this.runRepository = runRepository;
    this.sampleRepository = sampleRepository;
  }

  /** SPA'nın sırayla probe etmesi gereken GET path listesini döner. */
  public List<String> probeTargetPaths() {
    return PROBE_TARGETS;
  }

  /** En son persist edilmiş probe özet snapshot'ını döner. */
  public Optional<AdminLatencySnapshotDto> findLatestSnapshot() {
    return runRepository.findFirstByOrderByIdDesc().map(this::toSnapshotDto);
  }

  /** Probe ortalama geçmişini sayfalı listeler (en yeni önce). */
  public AdminLatencyRunsPageDto listRuns(int page, int size) {
    int safeSize = Math.min(Math.max(size, 1), 50);
    int safePage = Math.max(page, 0);
    Page<AdminLatencyProbeRun> p =
        runRepository.findAll(
            PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "id")));
    Optional<AdminLatencyProbeRun> latest = runRepository.findFirstByOrderByIdDesc();
    Long latestId = latest.map(AdminLatencyProbeRun::getId).orElse(null);
    List<AdminLatencyRunListItemDto> content =
        p.getContent().stream().map(r -> toListItemDto(r, latestId)).toList();
    return new AdminLatencyRunsPageDto(
        content, p.getTotalElements(), p.getTotalPages(), p.getNumber(), p.getSize());
  }

  /** Belirli bir run için path başına süre örneklerini döner. */
  public List<AdminLatencyProbeSampleItemDto> listSamplesForRun(long runId) {
    if (!runRepository.existsById(runId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "run not found");
    }
    return sampleRepository.findAllByRunIdOrderBySortOrderAsc(runId).stream()
        .map(s -> new AdminLatencyProbeSampleItemDto(s.getPath(), s.getDurationMs()))
        .toList();
  }

  /** İstemci ölçümlerini persist eder; önceki run örneklerini temizler. */
  @Transactional
  public AdminLatencySnapshotDto saveFromClientSamples(AdminLatencySnapshotSaveRequest request) {
    List<LatencySampleDto> samples = request.samples();
    validateSamples(samples);
    Instant now = Instant.now();
    double avgMs = samples.stream().mapToDouble(LatencySampleDto::durationMs).average().orElse(0.0);
    AdminLatencyProbeRun run = new AdminLatencyProbeRun(avgMs, samples.size(), now);
    run = runRepository.save(run);
    int order = 0;
    for (LatencySampleDto s : samples) {
      sampleRepository.save(
          new AdminLatencyProbeSample(
              run.getId(), normalizePath(s.path()), s.durationMs(), order++));
    }
    sampleRepository.deleteByRunIdNot(run.getId());
    return toSnapshotDto(run);
  }

  private AdminLatencySnapshotDto toSnapshotDto(AdminLatencyProbeRun r) {
    boolean has = sampleRepository.existsByRunId(r.getId());
    return new AdminLatencySnapshotDto(
        r.getId(), r.getAverageLatencyMs() / 1000.0, r.getSampleCount(), r.getMeasuredAt(), has);
  }

  private AdminLatencyRunListItemDto toListItemDto(AdminLatencyProbeRun r, Long latestId) {
    boolean has =
        latestId != null && latestId.equals(r.getId()) && sampleRepository.existsByRunId(r.getId());
    return new AdminLatencyRunListItemDto(
        r.getId(), r.getAverageLatencyMs() / 1000.0, r.getSampleCount(), r.getMeasuredAt(), has);
  }

  private void validateSamples(List<LatencySampleDto> samples) {
    if (samples == null || samples.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "samples required");
    }
    Set<String> normalized = new LinkedHashSet<>();
    for (LatencySampleDto s : samples) {
      String p = normalizePath(s.path());
      if (!PROBE_TARGET_SET.contains(p)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Illegal probe path: " + p);
      }
      if (!normalized.add(p)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate probe path: " + p);
      }
    }
    if (!normalized.equals(PROBE_TARGET_SET)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "samples must cover exactly these paths: "
              + PROBE_TARGETS.stream().collect(Collectors.joining(", ")));
    }
  }

  private static String normalizePath(String path) {
    if (path == null) {
      return "";
    }
    String p = path.trim();
    if (p.endsWith("/") && p.length() > 1) {
      p = p.substring(0, p.length() - 1);
    }
    return p;
  }
}
