package com.company.finance_api.infocards;

import com.company.finance_api.common.ApiResponse;
import com.company.finance_api.infocards.dto.InfoCardDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/portal/info-cards")
public class PortalInfoCardsController {

    private final InfoCardService infoCardService;

    public PortalInfoCardsController(InfoCardService infoCardService) {
        this.infoCardService = infoCardService;
    }

    @GetMapping
    public ApiResponse<List<InfoCardDto>> list(
            @RequestParam(required = false) String page,
            @RequestParam(defaultValue = "false") boolean includeAdminOnly,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage
    ) {
        return ApiResponse.success(infoCardService.listPortalCards(page, includeAdminOnly, acceptLanguage));
    }

    @GetMapping("/lookup")
    public ApiResponse<InfoCardDto> lookup(
            @RequestParam String page,
            @RequestParam(required = false) String term,
            @RequestParam(required = false) String elementId,
            @RequestParam(required = false) String instrumentSymbol,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage
    ) {
        return infoCardService.lookupHelpTarget(page, term, elementId, instrumentSymbol, acceptLanguage)
                .map(ApiResponse::success)
                .orElseGet(() -> ApiResponse.success(null));
    }

    @GetMapping("/slug/{slug}")
    public ApiResponse<InfoCardDto> bySlug(
            @PathVariable String slug,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage
    ) {
        return infoCardService.findBySlug(slug, acceptLanguage)
                .map(ApiResponse::success)
                .orElseGet(() -> ApiResponse.success(null));
    }

    @GetMapping("/{id}")
    public ApiResponse<InfoCardDto> byId(@PathVariable UUID id) {
        return infoCardService.findById(id)
                .map(ApiResponse::success)
                .orElseGet(() -> ApiResponse.success(null));
    }
}
