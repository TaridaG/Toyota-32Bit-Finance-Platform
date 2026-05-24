package com.company.newsservice.admin.application;

import com.company.newsservice.admin.domain.AdminNewsAnalyticsPreset;
import com.company.newsservice.admin.domain.AdminNewsMetricsMath;
import com.company.newsservice.admin.infrastructure.http.dto.AdminNewsAnalyticsDashboardDto;
import com.company.newsservice.admin.infrastructure.http.dto.AdminNewsAnalyticsSummaryDto;
import com.company.newsservice.admin.infrastructure.http.dto.AdminNewsDailyPublishDto;
import com.company.newsservice.admin.infrastructure.http.dto.AdminRecentNewsArticleRowDto;
import com.company.newsservice.query.domain.NewsArticle;
import com.company.newsservice.query.infrastructure.persistence.NewsArticleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin news analytics dashboard verisini UTC preset veya custom date range ile hesaplar.
 */
@Component
public class BuildAdminNewsAnalyticsUseCase {

    private static final ZoneOffset UTC = ZoneOffset.UTC;
    public static final int MAX_CUSTOM_RANGE_DAYS = 120;
    private static final int RECENT_LIMIT = 25;

    private final NewsArticleRepository newsArticleRepository;
    private final String translationLanguage;

    public BuildAdminNewsAnalyticsUseCase(
            NewsArticleRepository newsArticleRepository,
            @Value("${news.translation.default-language:en}") String translationLanguage) {
        this.newsArticleRepository = newsArticleRepository;
        this.translationLanguage = translationLanguage == null || translationLanguage.isBlank()
                ? "en"
                : translationLanguage.trim();
    }

    /** Preset'e göre analytics dashboard DTO üretir. */
    @Transactional(readOnly = true)
    public AdminNewsAnalyticsDashboardDto dashboard(AdminNewsAnalyticsPreset preset) {
        Instant now = Instant.now();
        LocalDate todayUtc = LocalDate.ofInstant(now, UTC);
        int spanDays = preset.inclusiveDayCount();
        LocalDate lastChartDay = todayUtc;
        LocalDate firstChartDay = todayUtc.minusDays(spanDays - 1);
        return build(firstChartDay, lastChartDay, preset.queryParam(), now);
    }

    /** Custom {@code from}/{@code to} UTC tarih aralığı için analytics dashboard DTO üretir. */
    @Transactional(readOnly = true)
    public AdminNewsAnalyticsDashboardDto dashboardCustom(LocalDate fromInclusive, LocalDate toInclusive) {
        Instant now = Instant.now();
        LocalDate todayUtc = LocalDate.ofInstant(now, UTC);
        if (fromInclusive == null || toInclusive == null) {
            throw new IllegalArgumentException("from and to are required");
        }
        if (fromInclusive.isAfter(toInclusive)) {
            throw new IllegalArgumentException("from must be on or before to");
        }
        LocalDate last = toInclusive.isAfter(todayUtc) ? todayUtc : toInclusive;
        LocalDate first = fromInclusive;
        long spanLong = ChronoUnit.DAYS.between(first, last) + 1;
        if (spanLong < 1) {
            throw new IllegalArgumentException("invalid range");
        }
        if (spanLong > MAX_CUSTOM_RANGE_DAYS) {
            throw new IllegalArgumentException("range must not exceed " + MAX_CUSTOM_RANGE_DAYS + " days");
        }
        if (first.isBefore(todayUtc.minusYears(5))) {
            throw new IllegalArgumentException("from is too far in the past");
        }
        return build(first, last, "custom", now);
    }

    private AdminNewsAnalyticsDashboardDto build(
            LocalDate firstChartDay, LocalDate lastChartDay, String presetKey, Instant now) {
        LocalDate todayUtc = LocalDate.ofInstant(now, UTC);
        int span = (int) ChronoUnit.DAYS.between(firstChartDay, lastChartDay) + 1;

        Instant chartFrom = firstChartDay.atStartOfDay(UTC).toInstant();
        Instant chartToExclusive = lastChartDay.plusDays(1).atStartOfDay(UTC).toInstant();

        long total = newsArticleRepository.countByActiveTrue();
        int sources = (int) Math.min(Integer.MAX_VALUE, newsArticleRepository.countDistinctSourceNameByActiveTrue());

        long withTranslation = newsArticleRepository.countActiveWithTranslationLanguage(translationLanguage);
        double translationPct = total > 0 ? 100.0 * withTranslation / total : 0.0;

        LocalDate mondayThisUtcWeek = todayUtc.with(DayOfWeek.MONDAY);
        LocalDate prevIsoWeekStart = mondayThisUtcWeek.minusWeeks(1);
        Instant prevIsoWeekFrom = prevIsoWeekStart.atStartOfDay(UTC).toInstant();
        Instant prevIsoWeekToExclusive = mondayThisUtcWeek.atStartOfDay(UTC).toInstant();
        long prevWeekPublished = newsArticleRepository.countPublishedBetween(prevIsoWeekFrom, prevIsoWeekToExclusive);

        YearMonth prevMonth = YearMonth.from(todayUtc).minusMonths(1);
        Instant prevMonthFrom = prevMonth.atDay(1).atStartOfDay(UTC).toInstant();
        Instant prevMonthToExclusive = prevMonth.plusMonths(1).atDay(1).atStartOfDay(UTC).toInstant();
        long prevMonthPublished = newsArticleRepository.countPublishedBetween(prevMonthFrom, prevMonthToExclusive);

        LocalDate y = todayUtc.minusDays(1);
        Instant yStart = y.atStartOfDay(UTC).toInstant();
        Instant yEnd = y.plusDays(1).atStartOfDay(UTC).toInstant();
        LocalDate y2 = todayUtc.minusDays(2);
        Instant y2Start = y2.atStartOfDay(UTC).toInstant();
        Instant y2End = y2.plusDays(1).atStartOfDay(UTC).toInstant();
        long createdYesterday = newsArticleRepository.countCreatedBetweenActiveTrue(yStart, yEnd);
        long createdDayBefore = newsArticleRepository.countCreatedBetweenActiveTrue(y2Start, y2End);

        long impliedPrior = Math.max(0L, total - createdYesterday);
        double totalVsPrior = AdminNewsMetricsMath.percentChange(total, impliedPrior);

        AdminNewsAnalyticsSummaryDto summary = new AdminNewsAnalyticsSummaryDto(
                total,
                sources,
                translationPct,
                translationLanguage,
                prevWeekPublished,
                prevMonthPublished,
                createdYesterday,
                createdDayBefore,
                totalVsPrior);

        List<Integer> dailyCounts = new ArrayList<>(span);
        for (int i = 0; i < span; i++) {
            LocalDate d = firstChartDay.plusDays(i);
            Instant start = d.atStartOfDay(UTC).toInstant();
            Instant end = start.plus(1, ChronoUnit.DAYS);
            dailyCounts.add((int) Math.min(Integer.MAX_VALUE, newsArticleRepository.countPublishedBetween(start, end)));
        }

        List<Double> rolling = new ArrayList<>(span);
        for (int i = 0; i < span; i++) {
            int from = Math.max(0, i - 6);
            double sum = 0;
            int n = 0;
            for (int j = from; j <= i; j++) {
                sum += dailyCounts.get(j);
                n++;
            }
            rolling.add(n > 0 ? sum / n : 0.0);
        }

        List<AdminNewsDailyPublishDto> daily = new ArrayList<>(span);
        for (int i = 0; i < span; i++) {
            LocalDate d = firstChartDay.plusDays(i);
            int c = dailyCounts.get(i);
            int prev = i > 0 ? dailyCounts.get(i - 1) : 0;
            int delta = i > 0 ? c - prev : 0;
            daily.add(new AdminNewsDailyPublishDto(d.toString(), c, rolling.get(i), delta));
        }

        List<NewsArticle> recentRows =
                newsArticleRepository.findByActiveTrueOrderByPublishedAtDesc(PageRequest.of(0, RECENT_LIMIT));
        List<AdminRecentNewsArticleRowDto> recent = new ArrayList<>(recentRows.size());
        for (NewsArticle a : recentRows) {
            recent.add(new AdminRecentNewsArticleRowDto(
                    a.getId(),
                    a.getTitle(),
                    a.getSourceName(),
                    a.getCategory().name(),
                    a.getPublishedAt()));
        }

        return new AdminNewsAnalyticsDashboardDto(
                presetKey,
                chartFrom,
                chartToExclusive,
                summary,
                daily,
                recent,
                now);
    }
}
