package com.company.finance_api.ai.client;

import com.company.finance_api.ai.config.AiOpenAiException;
import com.company.finance_api.ai.config.AiOpenAiQuotaException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** OpenAI Chat Completions API'sine structured JSON çıktı istekleri gönderir. */
@Component
public class OpenAiClient {

  private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);
  private static final String CHAT_COMPLETIONS_PATH = "/v1/chat/completions";

  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  public OpenAiClient(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    org.springframework.http.client.SimpleClientHttpRequestFactory requestFactory =
        new org.springframework.http.client.SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout((int) Duration.ofSeconds(15).toMillis());
    requestFactory.setReadTimeout((int) Duration.ofSeconds(90).toMillis());
    this.restClient =
        RestClient.builder()
            .baseUrl("https://api.openai.com")
            .requestFactory(requestFactory)
            .build();
  }

  /** JSON schema ile yapılandırılmış model yanıtı ister. */
  public String requestStructuredJson(
      String apiKey,
      String model,
      String systemPrompt,
      String userPrompt,
      String schemaName,
      Map<String, Object> jsonSchema) {
    Map<String, Object> responseFormat =
        Map.of(
            "type",
            "json_schema",
            "json_schema",
            Map.of(
                "name", schemaName,
                "strict", true,
                "schema", jsonSchema));

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("model", model);
    body.put("temperature", 0.3);
    body.put(
        "messages",
        List.of(
            Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", userPrompt)));
    body.put("response_format", responseFormat);

    try {
      String responseBody =
          restClient
              .post()
              .uri(CHAT_COMPLETIONS_PATH)
              .contentType(MediaType.APPLICATION_JSON)
              .headers(headers -> headers.setBearerAuth(apiKey))
              .body(body)
              .retrieve()
              .body(String.class);

      if (responseBody == null || responseBody.isBlank()) {
        throw new AiOpenAiException("OpenAI returned an empty response");
      }

      JsonNode root = objectMapper.readTree(responseBody);
      JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
      if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
        throw new AiOpenAiException("OpenAI response did not contain message content");
      }
      return contentNode.asText();
    } catch (AiOpenAiException ex) {
      throw ex;
    } catch (RestClientException ex) {
      log.warn("OpenAI HTTP error: {}", ex.getMessage());
      if (isQuotaExceeded(ex)) {
        throw new AiOpenAiQuotaException();
      }
      throw new AiOpenAiException("OpenAI request failed", ex);
    } catch (Exception ex) {
      log.warn("OpenAI parse error: {}", ex.getMessage());
      throw new AiOpenAiException("Failed to parse OpenAI response", ex);
    }
  }

  private static boolean isQuotaExceeded(RestClientException ex) {
    String message = ex.getMessage();
    return message != null && (message.contains("429") || message.contains("insufficient_quota"));
  }
}
