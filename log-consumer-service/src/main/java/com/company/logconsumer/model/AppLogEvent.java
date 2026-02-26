package com.company.logconsumer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AppLogEvent(
        String timestamp,
        String level,
        String serviceName,
        String message,
        String traceId,
        String spanId,
        String correlationId,
        String logger,
        String thread,
        String exception
) {}