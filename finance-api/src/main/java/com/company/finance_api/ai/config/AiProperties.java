package com.company.finance_api.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/** AI modülü yapılandırma özellikleri (OpenAI API key, model adları, enable flag). */
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

  private boolean enabled = false;
  private final OpenAi openai = new OpenAi();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public OpenAi getOpenai() {
    return openai;
  }

  /** AI özelliği kullanıma hazır değilse exception fırlatır. */
  public void validateReady() {
    if (!enabled) {
      throw new AiDisabledException();
    }
    if (!StringUtils.hasText(openai.getApiKey())) {
      throw new AiConfigurationException("OpenAI API key is not configured");
    }
  }

  public static class OpenAi {
    private String apiKey = "";
    private final Models models = new Models();

    public String getApiKey() {
      return apiKey;
    }

    public void setApiKey(String apiKey) {
      this.apiKey = apiKey;
    }

    public Models getModels() {
      return models;
    }
  }

  public static class Models {
    private String adminContent = "gpt-4.1-mini";
    private String translation = "gpt-4.1-mini";

    public String getAdminContent() {
      return adminContent;
    }

    public void setAdminContent(String adminContent) {
      this.adminContent = adminContent;
    }

    public String getTranslation() {
      return translation;
    }

    public void setTranslation(String translation) {
      this.translation = translation;
    }
  }
}
