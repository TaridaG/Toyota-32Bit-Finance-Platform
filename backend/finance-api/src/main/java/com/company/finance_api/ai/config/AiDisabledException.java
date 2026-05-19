package com.company.finance_api.ai.config;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class AiDisabledException extends RuntimeException {

    public AiDisabledException() {
        super("AI content generation is disabled");
    }
}
