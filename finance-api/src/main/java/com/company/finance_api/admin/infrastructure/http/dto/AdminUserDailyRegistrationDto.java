package com.company.finance_api.admin.infrastructure.http.dto;

/** Tek UTC takvim günü için yeni kayıt ve silme isteği sayıları. */
public record AdminUserDailyRegistrationDto(
    String date,
    int newUsers,
    int deletionRequests,
    double rollingAverage7d,
    int deltaVsPreviousDay) {}
