package com.company.finance_api.admin.dto;

/**
 * One UTC calendar day bucket for the admin registrations chart.
 */
public record AdminUserDailyRegistrationDto(
        String date,
        int newUsers,
        int deletionRequests,
        double rollingAverage7d,
        int deltaVsPreviousDay
) {
}
