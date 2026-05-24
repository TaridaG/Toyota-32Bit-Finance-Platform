package com.company.reporting.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Reporting HTTP isteklerinde {@code X-Correlation-Id} header'ını MDC'ye taşır (Kafka log pipeline ile uyumlu).
 */
@Slf4j
@Component
public class CorrelationIdFilter implements Filter {

    public static final String HEADER_NAME = "X-Correlation-Id";

    /** Gelen header veya yeni UUID ile MDC correlation/trace alanlarını set eder. */
    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String correlationId = httpRequest.getHeader(HEADER_NAME);

        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put("correlationId", correlationId);
        MDC.put("traceId", correlationId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}