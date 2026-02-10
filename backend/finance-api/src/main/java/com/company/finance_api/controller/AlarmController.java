package com.company.finance_api.controller;

import com.company.finance_api.common.ApiResponse;
import com.company.finance_api.dto.AlarmResponse;
import com.company.finance_api.dto.CreateAlarmRequest;
import com.company.finance_api.security.CurrentUserResolver;
import com.company.finance_api.service.AlarmService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/alarms")
public class AlarmController {

    private final AlarmService alarmService;
    private final CurrentUserResolver currentUserResolver;

    public AlarmController(AlarmService alarmService, CurrentUserResolver currentUserResolver) {
        this.alarmService = alarmService;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping
    public ApiResponse<List<AlarmResponse>> getUserAlarms(

    ) {
        UUID userId = currentUserResolver.getCurrentUserId();
        List<AlarmResponse> response =
                alarmService.getActiveAlarmsForUser(userId)
                        .stream()
                        .map(alarm -> new AlarmResponse(
                                alarm.getId(),
                                alarm.getInstrument().getSymbol(),
                                alarm.getCondition(),
                                alarm.getThreshold(),
                                alarm.isActive(),
                                alarm.getCreatedAt()
                        ))
                        .toList();

        return ApiResponse.success(response);
    }

    /**
     * Kullanıcı alarm oluşturur
     */
    @PostMapping
    public ApiResponse<Void> createAlarm(
            @RequestHeader("X-USER-ID") UUID userId,
            @Valid @RequestBody CreateAlarmRequest request
    ) {
        alarmService.createAlarm(
                currentUserResolver.getCurrentUserId(),
                request.getInstrumentId(),
                request.getCondition(),
                request.getThreshold()
        );

        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deactivateAlarm(@PathVariable Long id) {
        alarmService.deactivateAlarm(
                id,
                currentUserResolver.getCurrentUserId()
        );
        return ApiResponse.success(null);
    }
}
