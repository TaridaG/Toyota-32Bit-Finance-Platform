package com.company.finance_api.admin.infrastructure.http.dto;

/** users tablosundan türetilen KPI bloğu (roster + oluşturma zaman damgaları). */
public record AdminUserAnalyticsSummaryDto(
    long totalUsers,
    long newUsersLast7Days,
    long newUsersPrevious7Days,
    double newUsersWeekOverWeekPercent,
    long userDeltaLast7VsPrev7,
    long newUsersCurrentPeriod,
    long newUsersPreviousPeriod,
    double growthPercentPeriodVsPrevious,
    long activeUsers,
    double activeRatePercent,
    long newUsersYesterday,
    long newUsersDayBeforeYesterday,
    long yesterdayVsPriorDayDelta,
    Integer todayNewUsersUtc,
    /**
     * Önceki ISO haftasında yeni kayıtlar (Pzt 00:00 UTC → sonraki Pzt 00:00 exclusive), silme
     * bekleyenler hariç.
     */
    long newUsersPreviousIsoWeekUtc,
    /**
     * Önceki UTC takvim ayında yeni kayıtlar (ayın 1'i 00:00 → sonraki ayın 1'i 00:00 exclusive).
     */
    long newUsersPreviousCalendarMonthUtc,
    /**
     * Yaklaşık roster gün/gün yüzdesi: {@code totalUsers} vs {@code totalUsers -
     * newUsersYesterday}; dünkü net eklemeler yalnızca yeni kayıt sayılır (aynı gün roster
     * çıkışları yok sayılır).
     */
    double totalUsersVsPriorDayPercentApprox,
    /** Ömür boyu kalıcı silmeler (admin + kullanıcı saga); veritabanında monoton sayaç. */
    long deletedAccountsTotal,
    /** {@code frozen_at} dolu roster kullanıcıları (şu an askıda). */
    long frozenAccountsNow) {}
