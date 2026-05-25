package com.company.finance_api.alarm.infrastructure.http;

import com.company.finance_api.alarm.infrastructure.http.dto.AlarmResponse;
import com.company.finance_api.alarm.infrastructure.http.dto.CreateAlarmRequest;
import com.company.finance_api.alarm.application.AlarmService;
import com.company.finance_api.shared.security.CurrentUserResolver;
import com.company.finance_api.shared.web.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

/** Kullanıcı fiyat alarmları CRUD endpoint'leri. */
@RestController
@RequestMapping("/api/alarms")
public class AlarmController {

  private final AlarmService alarmService;
  private final CurrentUserResolver currentUserResolver;

  public AlarmController(AlarmService alarmService, CurrentUserResolver currentUserResolver) {
    this.alarmService = alarmService;
    this.currentUserResolver = currentUserResolver;
  }

  /** Oturum açmış kullanıcının aktif alarmlarını listeler. */
  @GetMapping
  public ApiResponse<List<AlarmResponse>> getUserAlarms() {

    UUID userId = currentUserResolver.getCurrentUserId();
    List<AlarmResponse> response =
        alarmService.getActiveAlarmsForUser(userId).stream()
            .map(
                alarm ->
                    new AlarmResponse(
                        alarm.getId(),
                        alarm.getInstrument().getSymbol(),
                        alarm.getCondition(),
                        alarm.getThreshold(),
                        alarm.isActive(),
                        alarm.getCreatedAt()))
            .toList();

    return ApiResponse.success(response);
  }

  /** Yeni fiyat alarmı oluşturur. */
  @PostMapping
  public ApiResponse<Void> createAlarm(@Valid @RequestBody CreateAlarmRequest request) {
    alarmService.createAlarm(
        currentUserResolver.getCurrentUserId(),
        request.getInstrumentId(),
        request.getCondition(),
        request.getThreshold());

    return ApiResponse.success(null);
  }

  /** Alarmı devre dışı bırakır (soft delete). */
  @DeleteMapping("/{id}")
  public ApiResponse<Void> deactivateAlarm(@PathVariable Long id) {
    alarmService.deactivateAlarm(id, currentUserResolver.getCurrentUserId());
    return ApiResponse.success(null);
  }
}
