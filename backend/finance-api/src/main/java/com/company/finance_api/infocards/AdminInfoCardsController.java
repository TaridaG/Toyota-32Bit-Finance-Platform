package com.company.finance_api.infocards;

import com.company.finance_api.common.ApiResponse;
import com.company.finance_api.infocards.dto.InfoCardDto;
import com.company.finance_api.infocards.dto.InfoCardInputDto;
import com.company.finance_api.infocards.dto.InfoCardsDashboardDto;
import com.company.finance_api.infocards.dto.InfoCardsPageDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/info-cards")
public class AdminInfoCardsController {

    private final InfoCardService infoCardService;

    public AdminInfoCardsController(InfoCardService infoCardService) {
        this.infoCardService = infoCardService;
    }

    @GetMapping
    public ApiResponse<InfoCardsPageDto> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String portalPage,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "ACTIVE") String status
    ) {
        return ApiResponse.success(infoCardService.listAdmin(page, size, portalPage, query, status));
    }

    @GetMapping("/dashboard")
    public ApiResponse<InfoCardsDashboardDto> dashboard() {
        return ApiResponse.success(infoCardService.dashboard());
    }

    @PostMapping
    public ApiResponse<InfoCardDto> create(@Valid @RequestBody InfoCardInputDto input) {
        return ApiResponse.success(infoCardService.create(input));
    }

    @PutMapping("/{id}")
    public ApiResponse<InfoCardDto> update(@PathVariable UUID id, @Valid @RequestBody InfoCardInputDto input) {
        return ApiResponse.success(infoCardService.update(id, input));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<InfoCardDto> toggleStatus(@PathVariable UUID id) {
        return ApiResponse.success(infoCardService.toggleStatus(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Boolean>> delete(@PathVariable UUID id) {
        infoCardService.delete(id);
        return ApiResponse.success(Map.of("deleted", true));
    }

    @PostMapping("/seed-defaults")
    public ApiResponse<Map<String, Integer>> seedDefaults() {
        return ApiResponse.success(Map.of("created", infoCardService.seedDefaults()));
    }
}
