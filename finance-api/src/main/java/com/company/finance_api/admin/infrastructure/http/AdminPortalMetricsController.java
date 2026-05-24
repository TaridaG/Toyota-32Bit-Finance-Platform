package com.company.finance_api.admin.infrastructure.http;

import com.company.finance_api.admin.application.AdminBlockedEmailDirectoryService;
import com.company.finance_api.admin.application.AdminLatencyProbeService;
import com.company.finance_api.admin.application.AdminMarketAssetAnalyticsService;
import com.company.finance_api.admin.application.AdminPortalInstrumentActivityService;
import com.company.finance_api.admin.application.AdminPortalPortfolioMetricsService;
import com.company.finance_api.admin.application.AdminPortalUserMetricsService;
import com.company.finance_api.admin.application.AdminPortfolioAnalyticsService;
import com.company.finance_api.admin.application.AdminUserAccountService;
import com.company.finance_api.admin.application.AdminUserAnalyticsService;
import com.company.finance_api.admin.application.AdminUserDirectoryService;
import com.company.finance_api.admin.application.AdminUserPortfolioDetailsService;
import com.company.finance_api.admin.domain.AdminUserAnalyticsPreset;
import com.company.finance_api.admin.domain.AdminUserDirectorySort;
import com.company.finance_api.admin.infrastructure.http.dto.AdminBlockedEmailsPageDto;
import com.company.finance_api.admin.infrastructure.http.dto.AdminDeleteUserRequest;
import com.company.finance_api.admin.infrastructure.http.dto.AdminFreezeUserRequest;
import com.company.finance_api.admin.infrastructure.http.dto.AdminLatencyProbeSampleItemDto;
import com.company.finance_api.admin.infrastructure.http.dto.AdminLatencyProbeTargetsDto;
import com.company.finance_api.admin.infrastructure.http.dto.AdminLatencyRunsPageDto;
import com.company.finance_api.admin.infrastructure.http.dto.AdminLatencySnapshotDto;
import com.company.finance_api.admin.infrastructure.http.dto.AdminLatencySnapshotSaveRequest;
import com.company.finance_api.admin.infrastructure.http.dto.AdminMarketAssetDashboardDto;
import com.company.finance_api.admin.infrastructure.http.dto.AdminMarketAssetRecomputeResponseDto;
import com.company.finance_api.admin.infrastructure.http.dto.AdminPortalDashboardMetricsDto;
import com.company.finance_api.admin.infrastructure.http.dto.AdminPortalPortfolioMetricsDto;
import com.company.finance_api.admin.infrastructure.http.dto.AdminPortalUserMetricsDto;
import com.company.finance_api.admin.infrastructure.http.dto.AdminPortfolioAnalyticsDashboardDto;
import com.company.finance_api.admin.infrastructure.http.dto.AdminSendUserMessageRequest;
import com.company.finance_api.admin.infrastructure.http.dto.AdminUserAnalyticsDashboardDto;
import com.company.finance_api.admin.infrastructure.http.dto.AdminUserDirectoryPageDto;
import com.company.finance_api.admin.infrastructure.http.dto.AdminUserPortfolioTreeDto;
import com.company.finance_api.profile.PortalProfileService;
import com.company.finance_api.repository.InstrumentRepository;
import com.company.finance_api.shared.web.ApiResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Admin portal metrics REST endpoint'leri: dashboard KPI, dizin, analytics ve latency probe. */
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
  private final AdminUserAccountService adminUserAccountService;
  private final AdminMarketAssetAnalyticsService adminMarketAssetAnalyticsService;
  private final AdminBlockedEmailDirectoryService adminBlockedEmailDirectoryService;

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
      PortalProfileService portalProfileService,
      AdminUserAccountService adminUserAccountService,
      AdminMarketAssetAnalyticsService adminMarketAssetAnalyticsService,
      AdminBlockedEmailDirectoryService adminBlockedEmailDirectoryService) {
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
    this.adminUserAccountService = adminUserAccountService;
    this.adminMarketAssetAnalyticsService = adminMarketAssetAnalyticsService;
    this.adminBlockedEmailDirectoryService = adminBlockedEmailDirectoryService;
  }

  /** Önceden hesaplanmış portal market-asset analytics tablosunu döner. */
  @GetMapping("/market-assets")
  public ApiResponse<AdminMarketAssetDashboardDto> marketAssets(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
    return ApiResponse.success(adminMarketAssetAnalyticsService.dashboard(page, size));
  }

  /** Market-asset analytics yeniden hesaplamasını tetikler. */
  @PostMapping("/market-assets/recompute")
  public ApiResponse<AdminMarketAssetRecomputeResponseDto> recomputeMarketAssets() {
    return ApiResponse.success(adminMarketAssetAnalyticsService.startRecompute());
  }

  /** Enterprise kullanıcı analytics dashboard payload'unu döner. */
  @GetMapping("/user-analytics")
  public ApiResponse<AdminUserAnalyticsDashboardDto> userAnalytics(
      @RequestParam(name = "preset", defaultValue = "7d") String preset,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    boolean hasFrom = from != null;
    boolean hasTo = to != null;
    if (hasFrom != hasTo) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Both 'from' and 'to' are required for a custom range.");
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

  /** Enterprise portfolio analytics dashboard payload'unu döner. */
  @GetMapping("/portfolio-analytics")
  public ApiResponse<AdminPortfolioAnalyticsDashboardDto> portfolioAnalytics(
      @RequestParam(name = "preset", defaultValue = "7d") String preset,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    boolean hasFrom = from != null;
    boolean hasTo = to != null;
    if (hasFrom != hasTo) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Both 'from' and 'to' are required for a custom range.");
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

  /** Kullanıcı + portfolio KPI'larını tek HTTP round-trip'te birleştirir. */
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
    List<Integer> instrumentActivityDaily =
        adminPortalInstrumentActivityService.distinctInstrumentsWithPriceDailyLast7Utc(
            users.generatedAt());
    return ApiResponse.success(
        AdminPortalDashboardMetricsDto.from(
            users, portfolios, totalInstruments, instrumentActivityDaily));
  }

  /** Sayfalı portal kullanıcı dizinini filtre ve sort ile döner. */
  @GetMapping("/user-directory")
  public ApiResponse<AdminUserDirectoryPageDto> userDirectory(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "createdAt") String sortField,
      @RequestParam(defaultValue = "desc") String sortDir,
      @RequestParam(required = false) Instant registeredFrom,
      @RequestParam(required = false) Instant registeredToExclusive,
      @RequestParam(required = false) Boolean emailVerified,
      @RequestParam(required = false) Integer portfolioCount,
      @RequestParam(required = false) String search) {
    String dir = sortDir.equalsIgnoreCase("asc") ? "asc" : "desc";
    String sortParam = sortField.trim() + "," + dir;
    AdminUserDirectorySort sortEnum = AdminUserDirectorySort.parse(sortParam);
    return ApiResponse.success(
        adminUserDirectoryService.list(
            page,
            size,
            sortEnum,
            sortParam,
            registeredFrom,
            registeredToExclusive,
            emailVerified,
            portfolioCount,
            search));
  }

  /** Seçili kullanıcıya admin destek mesajı gönderir. */
  @PostMapping("/user-directory/{userId}/message")
  public ApiResponse<Void> sendUserMessage(
      @PathVariable UUID userId, @Valid @RequestBody AdminSendUserMessageRequest request) {
    adminUserAccountService.sendMessage(userId, request.message());
    return ApiResponse.success(null);
  }

  /** Kullanıcı hesabını dondurur. */
  @PostMapping("/user-directory/{userId}/freeze")
  public ApiResponse<Void> freezeUser(
      @PathVariable UUID userId, @RequestBody(required = false) AdminFreezeUserRequest request) {
    String reason = request != null ? request.reason() : null;
    adminUserAccountService.freeze(userId, reason);
    return ApiResponse.success(null);
  }

  /** Kullanıcı hesabı dondurmasını kaldırır. */
  @PostMapping("/user-directory/{userId}/unfreeze")
  public ApiResponse<Void> unfreezeUser(@PathVariable UUID userId) {
    adminUserAccountService.unfreeze(userId);
    return ApiResponse.success(null);
  }

  /** Kullanıcı hesabını kalıcı siler. */
  @PostMapping("/user-directory/{userId}/delete")
  public ApiResponse<Void> deleteUser(
      @PathVariable UUID userId, @Valid @RequestBody AdminDeleteUserRequest request) {
    adminUserAccountService.deleteUser(userId, Boolean.TRUE.equals(request.blockEmail()));
    return ApiResponse.success(null);
  }

  /** Kayıt engelli e-posta listesini sayfalar. */
  @GetMapping("/blocked-emails")
  public ApiResponse<AdminBlockedEmailsPageDto> blockedEmails(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
    return ApiResponse.success(adminBlockedEmailDirectoryService.list(page, size));
  }

  /** Engellenmiş e-posta kaydının engelini kaldırır. */
  @PostMapping("/blocked-emails/{id}/unblock")
  public ApiResponse<Void> unblockEmail(@PathVariable long id) {
    adminBlockedEmailDirectoryService.unblock(id);
    return ApiResponse.success(null);
  }

  /** Kullanıcı avatar JPEG bytes döner (admin-only). */
  @GetMapping(value = "/user-directory/{userId}/avatar", produces = MediaType.IMAGE_JPEG_VALUE)
  public ResponseEntity<byte[]> userDirectoryAvatar(@PathVariable UUID userId) {
    byte[] body = portalProfileService.readAvatarForAdminByUserId(userId);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(Duration.ofMinutes(2)).cachePrivate())
        .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE)
        .body(body);
  }

  /** Kullanıcının portfolio ağacını ve pozisyon ağırlıklarını döner. */
  @GetMapping("/user-directory/{userId}/portfolios")
  public ApiResponse<AdminUserPortfolioTreeDto> userDirectoryPortfolios(@PathVariable UUID userId) {
    return ApiResponse.success(adminUserPortfolioDetailsService.loadTree(userId));
  }

  /** External portfolio toplamları ve oluşturma hızını döner. */
  @GetMapping("/portal-portfolios")
  public ApiResponse<AdminPortalPortfolioMetricsDto> portalPortfolios() {
    return ApiResponse.success(adminPortalPortfolioMetricsService.snapshot());
  }

  /** En son persist edilmiş admin round-trip latency özetini döner. */
  @GetMapping("/latency-snapshot")
  public ApiResponse<AdminLatencySnapshotDto> latencySnapshot() {
    return ApiResponse.success(adminLatencyProbeService.findLatestSnapshot().orElse(null));
  }

  /** SPA'nın sırayla probe etmesi gereken GET path listesini döner. */
  @GetMapping("/latency-probe-targets")
  public ApiResponse<AdminLatencyProbeTargetsDto> latencyProbeTargets() {
    return ApiResponse.success(
        new AdminLatencyProbeTargetsDto(adminLatencyProbeService.probeTargetPaths()));
  }

  /** Probe run'ını persist eder ve önceki örnekleri temizler. */
  @PostMapping("/latency-snapshot")
  public ApiResponse<AdminLatencySnapshotDto> saveLatencySnapshot(
      @Valid @RequestBody AdminLatencySnapshotSaveRequest body) {
    return ApiResponse.success(adminLatencyProbeService.saveFromClientSamples(body));
  }

  /** Probe ortalama geçmişini sayfalı listeler. */
  @GetMapping("/latency-runs")
  public ApiResponse<AdminLatencyRunsPageDto> latencyRuns(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
    return ApiResponse.success(adminLatencyProbeService.listRuns(page, size));
  }

  /** Belirli run için path başına süre örneklerini döner. */
  @GetMapping("/latency-runs/{id}/samples")
  public ApiResponse<List<AdminLatencyProbeSampleItemDto>> latencyRunSamples(
      @PathVariable long id) {
    return ApiResponse.success(adminLatencyProbeService.listSamplesForRun(id));
  }
}
