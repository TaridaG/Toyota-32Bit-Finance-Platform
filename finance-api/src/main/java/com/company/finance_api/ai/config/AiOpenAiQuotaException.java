package com.company.finance_api.ai.config;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** OpenAI kota veya rate limit aşıldığında fırlatılır. */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class AiOpenAiQuotaException extends RuntimeException {

  public AiOpenAiQuotaException() {
    super("OpenAI API quota exceeded");
  }
}
