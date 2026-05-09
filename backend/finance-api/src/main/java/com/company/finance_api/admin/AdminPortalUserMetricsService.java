package com.company.finance_api.admin;

import com.company.finance_api.admin.dto.AdminPortalUserMetricsDto;
import com.company.finance_api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminPortalUserMetricsService {

    private final UserRepository userRepository;

    public AdminPortalUserMetricsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public AdminPortalUserMetricsDto snapshot() {
        Instant now = Instant.now();
        Instant sevenAgo = now.minus(7, ChronoUnit.DAYS);
        Instant fourteenAgo = now.minus(14, ChronoUnit.DAYS);

        long totalUsers = userRepository.countByNotPendingDeletion();
        long last7 = userRepository.countCreatedInRangeExcludingPendingDeletion(sevenAgo, now);
        long prev7 = userRepository.countCreatedInRangeExcludingPendingDeletion(fourteenAgo, sevenAgo);
        double wow = weekOverWeekPercent(last7, prev7);

        List<Integer> daily = dailyNewRegistrationsUtcLast7Days(now);

        return new AdminPortalUserMetricsDto(
                totalUsers,
                last7,
                prev7,
                wow,
                daily,
                now
        );
    }

    static double weekOverWeekPercent(long currentWindowCount, long previousWindowCount) {
        if (previousWindowCount == 0) {
            return currentWindowCount > 0 ? 100.0 : 0.0;
        }
        return 100.0 * (currentWindowCount - previousWindowCount) / previousWindowCount;
    }

    private List<Integer> dailyNewRegistrationsUtcLast7Days(Instant now) {
        LocalDate todayUtc = LocalDate.ofInstant(now, ZoneOffset.UTC);
        List<Integer> out = new ArrayList<>(7);
        for (int i = 6; i >= 0; i--) {
            LocalDate d = todayUtc.minusDays(i);
            Instant start = d.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant end = start.plus(1, ChronoUnit.DAYS);
            out.add((int) Math.min(Integer.MAX_VALUE, userRepository.countCreatedInRangeExcludingPendingDeletion(start, end)));
        }
        return out;
    }
}
