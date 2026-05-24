package com.company.finance_api.ai.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Info-card AI görevleri için OpenAI JSON schema sabitleri. */
public final class OpenAiJsonSchemas {

  public static final String COMPLETE_SCHEMA_NAME = "info_card_complete";
  public static final String TRANSLATE_SCHEMA_NAME = "info_card_translate";

  private OpenAiJsonSchemas() {}

  /** Tamamlama görevi JSON schema haritasını döner. */
  public static Map<String, Object> completeSchema() {
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put("detailedDescription", stringProperty());
    properties.put("howToInterpret", stringProperty());
    properties.put("commonMistake", stringProperty());
    properties.put("example", stringProperty());
    properties.put("relatedTerms", relatedTermsProperty());
    return strictObject(
        properties,
        List.of(
            "detailedDescription", "howToInterpret", "commonMistake", "example", "relatedTerms"));
  }

  /** Çeviri görevi JSON schema haritasını döner. */
  public static Map<String, Object> translateSchema() {
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put("title", stringProperty());
    properties.put("shortDescription", stringProperty());
    properties.put("detailedDescription", stringProperty());
    properties.put("howToInterpret", stringProperty());
    properties.put("commonMistake", stringProperty());
    properties.put("example", stringProperty());
    properties.put("relatedTerms", relatedTermsProperty());
    return strictObject(
        properties,
        List.of(
            "title",
            "shortDescription",
            "detailedDescription",
            "howToInterpret",
            "commonMistake",
            "example",
            "relatedTerms"));
  }

  private static Map<String, Object> stringProperty() {
    return Map.of("type", "string");
  }

  private static Map<String, Object> relatedTermsProperty() {
    return Map.of("type", "array", "items", Map.of("type", "string"));
  }

  private static Map<String, Object> strictObject(
      Map<String, Object> properties, List<String> required) {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("type", "object");
    schema.put("properties", properties);
    schema.put("required", required);
    schema.put("additionalProperties", false);
    return schema;
  }
}
