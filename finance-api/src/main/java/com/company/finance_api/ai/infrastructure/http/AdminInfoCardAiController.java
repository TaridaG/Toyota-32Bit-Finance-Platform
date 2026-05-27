package com.company.finance_api.ai.infrastructure.http;

import com.company.finance_api.ai.dto.CompleteInfoCardAiRequest;
import com.company.finance_api.ai.dto.InfoCardAiContentResponse;
import com.company.finance_api.ai.dto.TranslateInfoCardAiRequest;
import com.company.finance_api.ai.service.AdminInfoCardAiService;
import com.company.finance_api.shared.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/info-cards/ai")
@PreAuthorize("hasRole('ADMIN')")
/** Admin info-card AI tamamlama ve çeviri REST endpoint'leri. */
public class AdminInfoCardAiController {

  private final AdminInfoCardAiService adminInfoCardAiService;

  public AdminInfoCardAiController(AdminInfoCardAiService adminInfoCardAiService) {
    this.adminInfoCardAiService = adminInfoCardAiService;
  }

  /** Eksik info-card alanlarını AI ile tamamlar. */
  @PostMapping("/complete")
  public ApiResponse<InfoCardAiContentResponse> complete(
      @Valid @RequestBody CompleteInfoCardAiRequest request) {
    return ApiResponse.success(adminInfoCardAiService.complete(request));
  }

  /** Kaynak info-card içeriğini hedef dile AI ile çevirir. */
  @PostMapping("/translate")
  public ApiResponse<InfoCardAiContentResponse> translate(
      @Valid @RequestBody TranslateInfoCardAiRequest request) {
    return ApiResponse.success(adminInfoCardAiService.translate(request));
  }
}
