package com.company.finance_api.ai.config;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** AI yapılandırması eksik veya geçersiz olduğunda fırlatılır. */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class AiConfigurationException extends RuntimeException {

  public AiConfigurationException(String message) {
    super(message);
  }
}
