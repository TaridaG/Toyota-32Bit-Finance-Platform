package com.company.finance_api.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.finance_api.admin.infrastructure.http.dto.AdminLatencySnapshotSaveRequest;
import com.company.finance_api.admin.infrastructure.http.dto.LatencySampleDto;
import com.company.finance_api.domain.AdminLatencyProbeRun;
import com.company.finance_api.domain.AdminLatencyProbeSample;
import com.company.finance_api.repository.AdminLatencyProbeRunRepository;
import com.company.finance_api.repository.AdminLatencyProbeSampleRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AdminLatencyProbeServiceTest {

  @Mock AdminLatencyProbeRunRepository runRepository;

  @Mock AdminLatencyProbeSampleRepository sampleRepository;

  AdminLatencyProbeService service;

  @BeforeEach
  void init() {
    service = new AdminLatencyProbeService(runRepository, sampleRepository);
  }

  @Test
  void rejectsUnknownPath() {
    assertThatThrownBy(
            () ->
                service.saveFromClientSamples(
                    new AdminLatencySnapshotSaveRequest(
                        List.of(new LatencySampleDto("/api/evil", 1.0)))))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void savesRunAndSamplesThenPrunesOtherSamples() {
    Instant now = Instant.now();
    AdminLatencyProbeRun persisted = new AdminLatencyProbeRun(15.0, 2, now);
    setRunId(persisted, 42L);
    when(runRepository.save(any(AdminLatencyProbeRun.class))).thenReturn(persisted);
    when(sampleRepository.save(any(AdminLatencyProbeSample.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    when(sampleRepository.existsByRunId(42L)).thenReturn(true);

    var dto =
        service.saveFromClientSamples(
            new AdminLatencySnapshotSaveRequest(
                List.of(
                    new LatencySampleDto("/api/v1/admin/metrics/portal-users", 10.0),
                    new LatencySampleDto("/api/v1/admin/metrics/portal-portfolios", 20.0))));

    assertThat(dto.id()).isEqualTo(42L);
    assertThat(dto.sampleCount()).isEqualTo(2);
    assertThat(dto.averageLatencySec()).isEqualTo(0.015);
    verify(sampleRepository).deleteByRunIdNot(42L);
  }

  @Test
  void listSamplesThrowsWhenRunMissing() {
    when(runRepository.existsById(99L)).thenReturn(false);
    assertThatThrownBy(() -> service.listSamplesForRun(99L))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void listRunsMapsHasSamples() {
    AdminLatencyProbeRun older = new AdminLatencyProbeRun(20, 2, Instant.now());
    setRunId(older, 1L);
    AdminLatencyProbeRun latest = new AdminLatencyProbeRun(30, 2, Instant.now());
    setRunId(latest, 2L);
    when(runRepository.findAll(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(latest, older)));
    when(runRepository.findFirstByOrderByIdDesc()).thenReturn(Optional.of(latest));
    when(sampleRepository.existsByRunId(2L)).thenReturn(true);

    var page = service.listRuns(0, 10);
    assertThat(page.content()).hasSize(2);
    assertThat(page.content().get(0).hasSamples()).isTrue();
    assertThat(page.content().get(1).hasSamples()).isFalse();
  }

  private static void setRunId(AdminLatencyProbeRun r, long id) {
    try {
      var idField = AdminLatencyProbeRun.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(r, id);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }
}
