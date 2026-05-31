package com.company.finance_api.infocards.infrastructure.http;

import com.company.finance_api.infocards.application.InfoCardService;
import com.company.finance_api.infocards.infrastructure.http.dto.InfoCardDto;
import com.company.finance_api.infocards.infrastructure.http.dto.LiteracyCatalogPageDto;
import com.company.finance_api.shared.web.ApiResponse;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Portal info-card ve literacy catalog REST endpoint'leri. */
@RestController
@RequestMapping("/api/v1/portal/info-cards")
public class PortalInfoCardsController {

  private final InfoCardService infoCardService;

  public PortalInfoCardsController(InfoCardService infoCardService) {
    this.infoCardService = infoCardService;
  }

  /** Portal sayfası için info-card listesini döner. */
  @GetMapping
  public ApiResponse<List<InfoCardDto>> list(
      @RequestParam(required = false) String page,
      @RequestParam(defaultValue = "false") boolean includeAdminOnly,
      @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
    return ApiResponse.success(
        infoCardService.listPortalCards(page, includeAdminOnly, acceptLanguage));
  }

  /** Financial literacy catalog sayfasını filtrelerle döner. */
  @GetMapping("/literacy-catalog")
  public ApiResponse<LiteracyCatalogPageDto> literacyCatalog(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "30") int size,
      @RequestParam(defaultValue = "false") boolean includeAdminOnly,
      @RequestParam(required = false) String query,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String difficulties,
      @RequestParam(required = false) String types,
      @RequestParam(required = false) String portalPages,
      @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
    return ApiResponse.success(
        infoCardService.listLiteracyCatalog(
            page,
            size,
            includeAdminOnly,
            acceptLanguage,
            query,
            category,
            splitCsv(difficulties),
            splitCsv(types),
            splitCsv(portalPages)));
  }

  private static List<String> splitCsv(String raw) {
    if (raw == null || raw.isBlank()) {
      return List.of();
    }
    return Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
  }

  /** Bağlamsal yardım hedefi için info-card arar. */
  @GetMapping("/lookup")
  public ApiResponse<InfoCardDto> lookup(
      @RequestParam String page,
      @RequestParam(required = false) String term,
      @RequestParam(required = false) String elementId,
      @RequestParam(required = false) String instrumentSymbol,
      @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
    return infoCardService
        .lookupHelpTarget(page, term, elementId, instrumentSymbol, acceptLanguage)
        .map(ApiResponse::success)
        .orElseGet(() -> ApiResponse.success(null));
  }

  /** Slug ile info-card detayını döner. */
  @GetMapping("/slug/{slug}")
  public ApiResponse<InfoCardDto> bySlug(
      @PathVariable String slug,
      @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
    return infoCardService
        .findBySlug(slug, acceptLanguage)
        .map(ApiResponse::success)
        .orElseGet(() -> ApiResponse.success(null));
  }

  /** Kimlik ile info-card detayını döner. */
  @GetMapping("/{id}")
  public ApiResponse<InfoCardDto> byId(@PathVariable UUID id) {
    return infoCardService
        .findById(id)
        .map(ApiResponse::success)
        .orElseGet(() -> ApiResponse.success(null));
  }
}
