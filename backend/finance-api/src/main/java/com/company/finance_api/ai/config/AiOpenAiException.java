package com.company.finance_api.ai.config;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class AiOpenAiException extends RuntimeException {

    public AiOpenAiException(String message) {
        super(message);
    }

    public AiOpenAiException(String message, Throwable cause) {
        super(message, cause);
    }
}
