package com.company.finance_api.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.finance_api.ai.client.OpenAiClient;
import com.company.finance_api.ai.client.OpenAiJsonSchemas;
import com.company.finance_api.ai.config.AiDisabledException;
import com.company.finance_api.ai.config.AiProperties;
import com.company.finance_api.ai.dto.CompleteInfoCardAiRequest;
import com.company.finance_api.ai.dto.InfoCardAiContentResponse;
import com.company.finance_api.ai.dto.InfoCardAiSourceContent;
import com.company.finance_api.ai.dto.TranslateInfoCardAiRequest;
import com.company.finance_api.ai.service.AdminInfoCardAiService;
import com.company.finance_api.ai.service.AiPromptTemplateService;
import com.company.finance_api.ai.service.AiUsageLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminInfoCardAiServiceTest {

  @Mock private OpenAiClient openAiClient;

  @Mock private AiUsageLogService usageLogService;

  private AdminInfoCardAiService service;
  private AiProperties aiProperties;

  @BeforeEach
  void setUp() {
    aiProperties = new AiProperties();
    aiProperties.setEnabled(true);
    aiProperties.getOpenai().setApiKey("test-key");
    aiProperties.getOpenai().getModels().setAdminContent("gpt-test");
    aiProperties.getOpenai().getModels().setTranslation("gpt-test-translate");

    service =
        new AdminInfoCardAiService(
            aiProperties,
            openAiClient,
            new AiPromptTemplateService(new ObjectMapper()),
            usageLogService,
            new ObjectMapper());
  }

  @Test
  void complete_returnsOnlyGeneratedFieldsWithoutTitleOrShortDescription() {
    when(openAiClient.requestStructuredJson(
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            eq(OpenAiJsonSchemas.COMPLETE_SCHEMA_NAME),
            any()))
        .thenReturn(
            """
                {
                  "detailedDescription": "Detay",
                  "howToInterpret": "Yorum",
                  "commonMistake": "Hata",
                  "example": "Ornek",
                  "relatedTerms": ["A", "B", "C"]
                }
                """);

    InfoCardAiContentResponse response =
        service.complete(
            new CompleteInfoCardAiRequest(
                "tr",
                "Tahvil",
                "Kisa aciklama",
                "BASIC_FINANCE",
                "TERM",
                "BEGINNER",
                List.of("detailedDescription", "relatedTerms")));

    assertThat(response.title()).isNull();
    assertThat(response.shortDescription()).isNull();
    assertThat(response.detailedDescription()).isEqualTo("Detay");
    assertThat(response.howToInterpret()).isNull();
    assertThat(response.relatedTerms()).containsExactly("A", "B", "C");
  }

  @Test
  void translate_returnsAllFieldsInTargetLanguage() {
    when(openAiClient.requestStructuredJson(
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            eq(OpenAiJsonSchemas.TRANSLATE_SCHEMA_NAME),
            any()))
        .thenReturn(
            """
                {
                  "title": "Bond",
                  "shortDescription": "Short",
                  "detailedDescription": "Detail",
                  "howToInterpret": "Interpret",
                  "commonMistake": "Mistake",
                  "example": "Example",
                  "relatedTerms": ["Bill", "Coupon"]
                }
                """);

    InfoCardAiContentResponse response =
        service.translate(
            new TranslateInfoCardAiRequest(
                "tr",
                "en",
                new InfoCardAiSourceContent(
                    "Tahvil", "Kisa", "Detay", "Yorum", "Hata", "Ornek", List.of("A", "B"))));

    assertThat(response.title()).isEqualTo("Bond");
    assertThat(response.shortDescription()).isEqualTo("Short");
    assertThat(response.detailedDescription()).isEqualTo("Detail");
    assertThat(response.relatedTerms()).containsExactly("Bill", "Coupon");

    ArgumentCaptor<String> modelCaptor = ArgumentCaptor.forClass(String.class);
    verify(openAiClient)
        .requestStructuredJson(
            eq("test-key"),
            modelCaptor.capture(),
            anyString(),
            anyString(),
            eq(OpenAiJsonSchemas.TRANSLATE_SCHEMA_NAME),
            any());
    assertThat(modelCaptor.getValue()).isEqualTo("gpt-test-translate");
  }

  @Test
  void complete_whenAiDisabled_throws() {
    aiProperties.setEnabled(false);

    assertThatThrownBy(
            () ->
                service.complete(
                    new CompleteInfoCardAiRequest("tr", "Tahvil", "Kisa", null, null, null, null)))
        .isInstanceOf(AiDisabledException.class);
  }
}
