package com.company.finance_api.ai.application;

import com.company.finance_api.ai.client.OpenAiClient;
import com.company.finance_api.ai.client.OpenAiJsonSchemas;
import com.company.finance_api.ai.config.AiOpenAiException;
import com.company.finance_api.ai.config.AiProperties;
import com.company.finance_api.ai.infrastructure.http.dto.CompleteInfoCardAiRequest;
import com.company.finance_api.ai.infrastructure.http.dto.InfoCardAiContentResponse;
import com.company.finance_api.ai.infrastructure.http.dto.TranslateInfoCardAiRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** OpenAI structured JSON ile admin info-card içerik tamamlama ve çeviri use-case'leri. */
@Service
public class AdminInfoCardAiService {

  public static final String TASK_COMPLETE = "ADMIN_INFO_CARD_COMPLETE";
  public static final String TASK_TRANSLATE = "ADMIN_INFO_CARD_TRANSLATE";

  private static final List<String> DEFAULT_FIELDS =
      List.of("detailedDescription", "howToInterpret", "commonMistake", "example", "relatedTerms");

  private final AiProperties aiProperties;
  private final OpenAiClient openAiClient;
  private final AiPromptTemplateService promptTemplateService;
  private final AiUsageLogService usageLogService;
  private final ObjectMapper objectMapper;

  public AdminInfoCardAiService(
      AiProperties aiProperties,
      OpenAiClient openAiClient,
      AiPromptTemplateService promptTemplateService,
      AiUsageLogService usageLogService,
      ObjectMapper objectMapper) {
    this.aiProperties = aiProperties;
    this.openAiClient = openAiClient;
    this.promptTemplateService = promptTemplateService;
    this.usageLogService = usageLogService;
    this.objectMapper = objectMapper;
  }

  /** Seçili alanları AI ile üretir ve kullanım loglar. */
  public InfoCardAiContentResponse complete(CompleteInfoCardAiRequest request) {
    aiProperties.validateReady();
    List<String> fields = resolveFields(request.fieldsToGenerate());
    String model = aiProperties.getOpenai().getModels().getAdminContent();
    String createdBy = currentPrincipalName();
    String fingerprint =
        request.language() + "|" + request.title() + "|" + request.shortDescription();

    try {
      String json =
          openAiClient.requestStructuredJson(
              aiProperties.getOpenai().getApiKey(),
              model,
              promptTemplateService.buildCompleteSystemPrompt(),
              promptTemplateService.buildCompleteUserPrompt(request, fields),
              OpenAiJsonSchemas.COMPLETE_SCHEMA_NAME,
              OpenAiJsonSchemas.completeSchema());
      InfoCardAiContentResponse parsed = parseContent(json);
      usageLogService.logSuccess(
          TASK_COMPLETE,
          request.language(),
          null,
          null,
          model,
          createdBy,
          fingerprint,
          json.length());
      return filterCompleteResponse(parsed, fields);
    } catch (RuntimeException ex) {
      usageLogService.logFailure(
          TASK_COMPLETE,
          request.language(),
          null,
          null,
          model,
          createdBy,
          fingerprint,
          ex.getMessage());
      throw ex;
    }
  }

  /** Kaynak locale içeriğini hedef dile çevirir. */
  public InfoCardAiContentResponse translate(TranslateInfoCardAiRequest request) {
    aiProperties.validateReady();
    if (request.sourceLanguage().equals(request.targetLanguage())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "sourceLanguage and targetLanguage must differ");
    }

    String model = aiProperties.getOpenai().getModels().getTranslation();
    String createdBy = currentPrincipalName();
    String fingerprint =
        request.sourceLanguage()
            + "->"
            + request.targetLanguage()
            + "|"
            + request.sourceContent().title();

    try {
      String json =
          openAiClient.requestStructuredJson(
              aiProperties.getOpenai().getApiKey(),
              model,
              promptTemplateService.buildTranslateSystemPrompt(),
              promptTemplateService.buildTranslateUserPrompt(request),
              OpenAiJsonSchemas.TRANSLATE_SCHEMA_NAME,
              OpenAiJsonSchemas.translateSchema());
      InfoCardAiContentResponse parsed = parseContent(json);
      usageLogService.logSuccess(
          TASK_TRANSLATE,
          null,
          request.sourceLanguage(),
          request.targetLanguage(),
          model,
          createdBy,
          fingerprint,
          json.length());
      return parsed;
    } catch (RuntimeException ex) {
      usageLogService.logFailure(
          TASK_TRANSLATE,
          null,
          request.sourceLanguage(),
          request.targetLanguage(),
          model,
          createdBy,
          fingerprint,
          ex.getMessage());
      throw ex;
    }
  }

  private List<String> resolveFields(List<String> requested) {
    if (requested == null || requested.isEmpty()) {
      return DEFAULT_FIELDS;
    }
    Set<String> normalized = new LinkedHashSet<>();
    for (String field : requested) {
      if (StringUtils.hasText(field)) {
        normalized.add(field.trim());
      }
    }
    return normalized.isEmpty() ? DEFAULT_FIELDS : List.copyOf(normalized);
  }

  private InfoCardAiContentResponse filterCompleteResponse(
      InfoCardAiContentResponse parsed, List<String> fields) {
    return new InfoCardAiContentResponse(
        null,
        null,
        fields.contains("detailedDescription") ? parsed.detailedDescription() : null,
        fields.contains("howToInterpret") ? parsed.howToInterpret() : null,
        fields.contains("commonMistake") ? parsed.commonMistake() : null,
        fields.contains("example") ? parsed.example() : null,
        fields.contains("relatedTerms") ? parsed.relatedTerms() : null);
  }

  private InfoCardAiContentResponse parseContent(String json) {
    try {
      JsonNode node = objectMapper.readTree(json);
      List<String> relatedTerms = new ArrayList<>();
      if (node.has("relatedTerms") && node.get("relatedTerms").isArray()) {
        node.get("relatedTerms").forEach(term -> relatedTerms.add(term.asText()));
      }
      return new InfoCardAiContentResponse(
          textOrNull(node, "title"),
          textOrNull(node, "shortDescription"),
          textOrNull(node, "detailedDescription"),
          textOrNull(node, "howToInterpret"),
          textOrNull(node, "commonMistake"),
          textOrNull(node, "example"),
          relatedTerms);
    } catch (Exception ex) {
      throw new AiOpenAiException("Failed to parse AI JSON response", ex);
    }
  }

  private static String textOrNull(JsonNode node, String field) {
    JsonNode value = node.get(field);
    if (value == null || value.isNull()) {
      return null;
    }
    String text = value.asText();
    return StringUtils.hasText(text) ? text : null;
  }

  private static String currentPrincipalName() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !StringUtils.hasText(authentication.getName())) {
      return null;
    }
    return authentication.getName();
  }
}
