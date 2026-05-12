package com.company.finance_api.admin;

import com.company.finance_api.common.ApiResponse;
import com.company.finance_api.admin.dto.AdminPortalDashboardMetricsDto;
import com.company.finance_api.admin.dto.AdminPortalPortfolioMetricsDto;
import com.company.finance_api.admin.dto.AdminPortalUserMetricsDto;
import com.company.finance_api.admin.dto.AdminLatencyProbeSampleItemDto;
import com.company.finance_api.admin.dto.AdminLatencyProbeTargetsDto;
import com.company.finance_api.admin.dto.AdminLatencyRunsPageDto;
import com.company.finance_api.admin.dto.AdminLatencySnapshotDto;
import com.company.finance_api.admin.dto.AdminLatencySnapshotSaveRequest;
import com.company.finance_api.admin.dto.AdminUserDirectoryPageDto;
import com.company.finance_api.admin.dto.AdminPortfolioAnalyticsDashboardDto;
import com.company.finance_api.admin.dto.AdminUserAnalyticsDashboardDto;
import com.company.finance_api.admin.dto.AdminUserPortfolioTreeDto;
import com.company.finance_api.profile.PortalProfileService;
import com.company.finance_api.repository.InstrumentRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/metrics")
public class AdminPortalMetricsController {

    private static final Logger log = LoggerFactory.getLogger(AdminPortalMetricsController.class);

    private final AdminPortalUserMetricsService adminPortalUserMetricsService;
    private final AdminPortalPortfolioMetricsService adminPortalPortfolioMetricsService;
    private final AdminPortalInstrumentActivityService adminPortalInstrumentActivityService;
    private final InstrumentRepository instrumentRepository;
    private final AdminLatencyProbeService adminLatencyProbeService;
    private final AdminUserDirectoryService adminUserDirectoryService;
    private final AdminUserPortfolioDetailsService adminUserPortfolioDetailsService;
    private final AdminUserAnalyticsService adminUserAnalyticsService;
    private final AdminPortfolioAnalyticsService adminPortfolioAnalyticsService;
    private final PortalProfileService portalProfileService;

    public AdminPortalMetricsController(
            AdminPortalUserMetricsService adminPortalUserMetricsService,
            AdminPortalPortfolioMetricsService adminPortalPortfolioMetricsService,
            AdminPortalInstrumentActivityService adminPortalInstrumentActivityService,
            InstrumentRepository instrumentRepository,
            AdminLatencyProbeService adminLatencyProbeService,
            AdminUserDirectoryService adminUserDirectoryService,
            AdminUserPortfolioDetailsService adminUserPortfolioDetailsService,
            AdminUserAnalyticsService adminUserAnalyticsService,
            AdminPortfolioAnalyticsService adminPortfolioAnalyticsService,
            PortalProfileService portalProfileService) {
        this.adminPortalUserMetricsService = adminPortalUserMetricsService;
        this.adminPortalPortfolioMetricsService = adminPortalPortfolioMetricsService;
        this.adminPortalInstrumentActivityService = adminPortalInstrumentActivityService;
        this.instrumentRepository = instrumentRepository;
        this.adminLatencyProbeService = adminLatencyProbeService;
        this.adminUserDirectoryService = adminUserDirectoryService;
        this.adminUserPortfolioDetailsService = adminUserPortfolioDetailsService;
        this.adminUserAnalyticsService = adminUserAnalyticsService;
        this.adminPortfolioAnalyticsService = adminPortfolioAnalyticsService;
        this.portalProfileService = portalProfileService;
    }

    /**
     * Portal user roster, signup velocity, and external portfolio totals in one payload (UTC windows).
     * Bundled so the SPA needs only this call for both admin KPI cards. Requires {@code ADMIN} at the API gateway.
     */
    /**
     * Enterprise “Total users” analytics (UTC). Sourced from {@code users}; segmentation / heatmap / session
     * metrics are stubbed until dedicated telemetry tables exist.
     */
    @GetMapping("/user-analytics")
    public ApiResponse<AdminUserAnalyticsDashboardDto> userAnalytics(
            @RequestParam(name = "preset", defaultValue = "7d") String preset,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        boolean hasFrom = from != null;
        boolean hasTo = to != null;
        if (hasFrom != hasTo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both 'from' and 'to' are required for a custom range.");
        }
        if (hasFrom) {
            try {
                return ApiResponse.success(adminUserAnalyticsService.dashboardCustom(from, to));
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
            }
        }
        AdminUserAnalyticsPreset p = AdminUserAnalyticsPreset.parse(preset);
        return ApiResponse.success(adminUserAnalyticsService.dashboard(p));
    }

    /**
     * Enterprise “Active portfolios” analytics (UTC): creation velocity, position depth, and roster coverage.
     */
    @GetMapping("/portfolio-analytics")
    public ApiResponse<AdminPortfolioAnalyticsDashboardDto> portfolioAnalytics(
            @RequestParam(name = "preset", defaultValue = "7d") String preset,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        boolean hasFrom = from != null;
        boolean hasTo = to != null;
        if (hasFrom != hasTo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both 'from' and 'to' are required for a custom range.");
        }
        if (hasFrom) {
            try {
                return ApiResponse.success(adminPortfolioAnalyticsService.dashboardCustom(from, to));
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
            }
        }
        AdminUserAnalyticsPreset p = AdminUserAnalyticsPreset.parse(preset);
        return ApiResponse.success(adminPortfolioAnalyticsService.dashboard(p));
    }

    @GetMapping("/portal-users")
    public ApiResponse<AdminPortalDashboardMetricsDto> portalUsers() {
        AdminPortalUserMetricsDto users = adminPortalUserMetricsService.snapshot();
        AdminPortalPortfolioMetricsDto portfolios = adminPortalPortfolioMetricsService.snapshot();
        long totalInstruments = 0L;
        try {
            // Match portal/catalog scope (InstrumentService#getAllActive), not rows with active=false.
            totalInstruments = instrumentRepository.countByActiveTrue();
        } catch (RuntimeException ex) {
            log.warn("Active instrument catalog count failed; totalInstruments=0 in admin payload", ex);
        }
        List<Integer> instrumentActivityDaily = adminPortalInstrumentActivityService.distinctInstrumentsWithPriceDailyLast7Utc(
                users.generatedAt());
        return ApiResponse.success(
                AdminPortalDashboardMetricsDto.from(users, portfolios, totalInstruments, instrumentActivityDaily));
    }

    /**
     * Paginated portal user directory (filters, sort). Lives under {@code /metrics} so the same gateway
     * route as {@link #portalUsers()} always reaches finance-api (avoids comma-in-{@code sort} quirks and
     * any stale mappings for {@code /api/admin/users}).
     */
    @GetMapping("/user-directory")
    public ApiResponse<AdminUserDirectoryPageDto> userDirectory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortField,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) Instant registeredFrom,
            @RequestParam(required = false) Instant registeredToExclusive,
            @RequestParam(required = false) Boolean emailVerified,
            @RequestParam(required = false) Integer portfolioCount) {
        String dir = sortDir.equalsIgnoreCase("asc") ? "asc" : "desc";
        String sortParam = sortField.trim() + "," + dir;
        AdminUserDirectorySort sortEnum = AdminUserDirectorySort.parse(sortParam);
        return ApiResponse.success(adminUserDirectoryService.list(
                page,
                size,
                sortEnum,
                sortParam,
                registeredFrom,
                registeredToExclusive,
                emailVerified,
                portfolioCount));
    }

    @GetMapping(value = "/user-directory/{userId}/avatar", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> userDirectoryAvatar(@PathVariable UUID userId) {
        byte[] body = portalProfileService.readAvatarForAdminByUserId(userId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(2)).cachePrivate())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE)
                .body(body);
    }

    /**
     * Admin-only: per-portfolio holdings with market-value weights for one user. Same {@code /api/admin/metrics}
     * security as the directory; never exposed under portal routes.
     */
    @GetMapping("/user-directory/{userId}/portfolios")
    public ApiResponse<AdminUserPortfolioTreeDto> userDirectoryPortfolios(@PathVariable UUID userId) {
        return ApiResponse.success(adminUserPortfolioDetailsService.loadTree(userId));
    }

    /**
     * External portfolio totals and creation velocity (UTC day buckets). Requires {@code ADMIN} at the API gateway.
     */
    @GetMapping("/portal-portfolios")
    public ApiResponse<AdminPortalPortfolioMetricsDto> portalPortfolios() {
        return ApiResponse.success(adminPortalPortfolioMetricsService.snapshot());
    }

    /** Latest persisted admin round-trip latency (see {@link #saveLatencySnapshot}). */
    @GetMapping("/latency-snapshot")
    public ApiResponse<AdminLatencySnapshotDto> latencySnapshot() {
        return ApiResponse.success(adminLatencyProbeService.findLatestSnapshot().orElse(null));
    }

    /** Ordered GET paths the SPA should probe sequentially (admin-only). */
    @GetMapping("/latency-probe-targets")
    public ApiResponse<AdminLatencyProbeTargetsDto> latencyProbeTargets() {
        return ApiResponse.success(new AdminLatencyProbeTargetsDto(adminLatencyProbeService.probeTargetPaths()));
    }

    /** Persists a probe run, replaces line-item samples from previous runs, keeps historical averages. */
    @PostMapping("/latency-snapshot")
    public ApiResponse<AdminLatencySnapshotDto> saveLatencySnapshot(@Valid @RequestBody AdminLatencySnapshotSaveRequest body) {
        return ApiResponse.success(adminLatencyProbeService.saveFromClientSamples(body));
    }

    /** Paginated history of probe averages (newest first). */
    @GetMapping("/latency-runs")
    public ApiResponse<AdminLatencyRunsPageDto> latencyRuns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(adminLatencyProbeService.listRuns(page, size));
    }

    /** Per-request timings for a run (populated only for the latest run). */
    @GetMapping("/latency-runs/{id}/samples")
    public ApiResponse<List<AdminLatencyProbeSampleItemDto>> latencyRunSamples(@PathVariable long id) {
        return ApiResponse.success(adminLatencyProbeService.listSamplesForRun(id));
    }
}
